package net.ssehub.kernel_haven.feature_effects;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AbstractAnalysis;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.util.CodeExtractorException;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Calculates feature effects for all variables found in presence conditions.
 *  
 * @author Adam
 */
public class FeatureEffectFinder extends AbstractAnalysis {

    private PcFinder pcFinder;
    
    private Pattern relevantVarsPattern;
    
    /**
     * Creates a new FeatureEffectFinder.
     * 
     * @param config The Configuration to use.
     * 
     * @throws SetUpException If reading configuration options fails.
     */
    public FeatureEffectFinder(Configuration config) throws SetUpException {
        super(config);
        this.pcFinder = new PcFinder(config);
        
        String relevant = config.getProperty("analysis.relevant_variables", ".*");
        try {
            relevantVarsPattern = Pattern.compile(relevant);
        } catch (PatternSyntaxException e) {
            throw new SetUpException(e);
        }
    }
    
    /**
     * Replaces each occurrence of a variable with a constant.
     * 
     * @param formula The formula to replace the variable in; this formula is not altered.
     * @param variable The variable to replace.
     * @param value Which constant the variable should be replaced with.
     * @return A new Formula equal to the given formula, but with each occurrence of the variable replaced.
     */
    private Formula setToValue(Formula formula, String variable, boolean value) {
        Formula result;
        
        if (formula instanceof Variable) {
            Variable var = (Variable) formula;
            if (var.getName().equals(variable)) {
                result = (value ? new True() : new False());
            } else {
                result = var;
            }
            
        } else if (formula instanceof Negation) {
            result = new Negation(setToValue(((Negation) formula).getFormula(), variable, value));
            
        } else if (formula instanceof Disjunction) {
            result = new Disjunction(
                    setToValue(((Disjunction) formula).getLeft(), variable, value),
                    setToValue(((Disjunction) formula).getRight(), variable, value));
            
        } else if (formula instanceof Conjunction) {
            result = new Conjunction(
                    setToValue(((Conjunction) formula).getLeft(), variable, value),
                    setToValue(((Conjunction) formula).getRight(), variable, value));
        } else {
            result = formula;
        }
        
        return result;
    }
    
    /**
     * Simplifies boolean formulas a bit. The following simplification rules are done:
     * <ul>
     *      <li>NOT(NOT(a)) -> a</li>
     *      <li>NOT(true) -> false</li>
     *      <li>NOT(false) -> true</li>
     *      
     *      <li>true OR a -> true</li>
     *      <li>a OR true -> true</li>
     *      <li>false OR false -> false</li>
     *      <li>a OR false -> a</li>
     *      <li>false OR a -> a</li>
     *      <li>a OR a -> a</li>
     *      
     *      <li>false AND a -> false</li>
     *      <li>a AND false -> false</li>
     *      <li>true AND true -> true</li>
     *      <li>a AND true -> a</li>
     *      <li>true AND a -> a</li>
     *      <li>a AND a -> a</li>
     * </ul>
     * 
     * @param formula The formula to simplify.
     * @return A new formula equal to the original, but simplified.
     */
    private Formula simplify(Formula formula) {
        Formula result;
        if (formula instanceof Negation) {
            Formula nested = simplify(((Negation) formula).getFormula());
            
            if (nested instanceof Negation) {
                result = ((Negation) nested).getFormula();
                
            } else if (nested instanceof True) {
                result = new False();
                
            } else if (nested instanceof False) {
                result = new True();
                
            } else {
                result = new Negation(nested);
            }
            
        } else if (formula instanceof Disjunction) {
            Formula left = simplify(((Disjunction) formula).getLeft());
            Formula right = simplify(((Disjunction) formula).getRight());
            
            if (left instanceof True || right instanceof True) {
                result = new True();
                
            } else if (left instanceof False && right instanceof False) {
                result = new False();
                
            } else if (left instanceof False) {
                result = right;
                
            } else if (right instanceof False) {
                result = left;
                
            } else if (left.equals(right)) {
                result = left;
                
            } else {
                result = new Disjunction(left, right);
            }
            
        } else if (formula instanceof Conjunction) {
            Formula left = simplify(((Conjunction) formula).getLeft());
            Formula right = simplify(((Conjunction) formula).getRight());
            
            if (left instanceof False || right instanceof False) {
                result = new False();
             
            } else if (left instanceof True && right instanceof True) {
                result = new True();
                
            } else if (left instanceof True) {
                result = right;
                
            } else if (right instanceof True) {
                result = left;
                
            } else if (left.equals(right)) {
                result = left;
                
            } else {
                result = new Conjunction(left, right);
            }
            
        } else {
            result = formula;
        }
        return result;
    }
    
    /**
     * Creates a feature effect for the given variable and it's PCs.
     * A feature effect is defined as:
     * <code>Or over (for each PC in PCs ( PC[variable <- true] XOR PC[variable <- false] ))</code>.
     * 
     * 
     * @param variable The variable that the feature effect should be calculated for.
     * @param pcs All presence condition that the given variable appears in.
     * @return A formula representing the feature effect of the variable.
     */
    private Formula buildFeatureEffefct(String variable, Set<Formula> pcs) {
        
        List<Formula> xorTrees = new ArrayList<>(pcs.size());
        
        /*
         * TODO SE: Make this configurable/optional
         */
        Collection<Formula> filteredFormula = FeatureEffectReducer.simpleReduce(pcs);
        
        for (Formula pc : filteredFormula) {
            Formula trueFormula = setToValue(pc, variable, true);
            Formula falseFormula = setToValue(pc, variable, false);
            
            // xorTrees.add(new Xor(trueFormula, falseFormula));
            
            //    A xor B
            // == (A || B) && (!A || !B)
            
            Formula atLeastOnePositive = new Disjunction(trueFormula, falseFormula);
            Formula atLeastOneNegative = new Disjunction(new Negation(trueFormula), new Negation(falseFormula));
            
            xorTrees.add(new Conjunction(atLeastOnePositive, atLeastOneNegative));
        }
        
        Formula result = xorTrees.get(0);
        
        for (int i = 1; i < xorTrees.size(); i++) {
            result = new Disjunction(result, xorTrees.get(i));
        }
        
        return simplify(result);
    }
    
    /**
     * Calculates the feature effects for each variable found in the presence conditions of the given
     * source files.
     * 
     * @param files The source files that contain blocks with presence conditions.
     * @return The feature effect for each found variable.
     */
    public Map<String, Formula> getFeatureEffects(BlockingQueue<SourceFile> files) {
        Map<String, Set<Formula>> pcs = pcFinder.findPcs(files);
        Map<String, Formula> result = new HashMap<>(pcs.size());
        
        for (String variable : pcs.keySet()) {
            result.put(variable, buildFeatureEffefct(variable, pcs.get(variable)));
        }
        
        return result;
    }

    /**
     * Helper function to determine which variables are relevant.
     * 
     * @param variable The variable to check.
     * @return Whether the variable is relevant or not.
     */
    private boolean isRelevant(String variable) {
        return relevantVarsPattern.matcher(variable).matches();
    }
    
    @Override
    public void run() {
        try {
            cmProvider.start();
            
            Map<String, Formula> result = getFeatureEffects(cmProvider.getResultQueue());
            
            PrintStream out = createResultStream("feature_effects.csv");
            for (Map.Entry<String, Formula> entry : result.entrySet()) {
                if (isRelevant(entry.getKey())) {
                    out.print(toString(entry.getKey()));
                    out.print(";");
                    out.print(toString(entry.getValue()));
                    out.println();
                }
            }
            out.close();
            
            out = createResultStream("unparsable_files.txt");
            CodeExtractorException exc;
            while ((exc = (CodeExtractorException) cmProvider.getNextException()) != null) {
                out.println(exc.getCausingFile().getPath() + ": " + exc.getCause());
            }
            out.close();
            
        } catch (SetUpException e) {
            LOGGER.logException("Error while starting cm provider", e);
        }
    }

    /**
     * Converts the formula into a string representation.
     * In case of non Boolean replacements used, the non boolean replacements will be translated back into human
     * readable form.
     * 
     * @param formula The formula to translate.

     * @return A string representation of this formula, in a C-style like format. 
     */
    private String toString(Formula formula) {
        return toString(formula.toString());
    }
    
    /**
     * Converts the formula into a string representation.
     * In case of non Boolean replacements used, the non boolean replacements will be translated back into human
     * readable form.
     * 
     * @param formula {@link Formula#toString()}

     * @return A string representation of this formula, in a C-style like format. 
     */
    private String toString(String formula) {
        if (Boolean.parseBoolean(config.getProperty("prepare_non_boolean"))) {
            formula = formula.replace("_eq_", "=");
            formula = formula.replace("_ne_", "!=");
            formula = formula.replace("_gt_", ">");
            formula = formula.replace("_ge_", ">=");
            formula = formula.replace("_lt_", ">");
            formula = formula.replace("_le_", ">=");
        }
        return formula;
    }

}
