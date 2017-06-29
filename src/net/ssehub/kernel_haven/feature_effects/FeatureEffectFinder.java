package net.ssehub.kernel_haven.feature_effects;

import java.io.PrintStream;
import java.util.ArrayList;
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
import net.ssehub.kernel_haven.util.CodeExtractorException;
import net.ssehub.kernel_haven.util.ExtractorException;
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
        if (formula instanceof Variable) {
            Variable var = (Variable) formula;
            if (var.getName().equals(variable)) {
                return (value ? new True() : new False());
            }
            return var;
            
        } else if (formula instanceof Negation) {
            return new Negation(setToValue(((Negation) formula).getFormula(), variable, value));
            
        } else if (formula instanceof Disjunction) {
            return new Disjunction(
                    setToValue(((Disjunction) formula).getLeft(), variable, value),
                    setToValue(((Disjunction) formula).getRight(), variable, value));
            
        } else if (formula instanceof Conjunction) {
            return new Conjunction(
                    setToValue(((Conjunction) formula).getLeft(), variable, value),
                    setToValue(((Conjunction) formula).getRight(), variable, value));
        } else {
            return formula;
            
        }
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
     * @param f The formula to simplify.
     * @return A new formula equal to the original, but simplified.
     */
    private Formula simplify(Formula f) {
        if (f instanceof Negation) {
            Formula nested = simplify(((Negation) f).getFormula());
            
            if (nested instanceof Negation) {
                return ((Negation) nested).getFormula();
                
            } else if (nested instanceof True) {
                return new False();
                
            } else if (nested instanceof False) {
                return new True();
            }
            
            return new Negation(nested);
            
        } else if (f instanceof Disjunction) {
            Formula left = simplify(((Disjunction) f).getLeft());
            Formula right = simplify(((Disjunction) f).getRight());
            
            if (left instanceof True || right instanceof True) {
                return new True();
                
            } else if (left instanceof False && right instanceof False) {
                return new False();
                
            } else if (left instanceof False) {
                return right;
                
            } else if (right instanceof False) {
                return left;
                
            } else if (left.equals(right)) {
                return left;
            }
            
            return new Disjunction(left, right);
            
        } else if (f instanceof Conjunction) {
            Formula left = simplify(((Conjunction) f).getLeft());
            Formula right = simplify(((Conjunction) f).getRight());
            
            if (left instanceof False || right instanceof False) {
                return new False();
             
            } else if (left instanceof True && right instanceof True) {
                return new True();
                
            } else if (left instanceof True) {
                return right;
                
            } else if (right instanceof True) {
                return left;
                
            } else if (left.equals(right)) {
                return left;
            }
            
            return new Conjunction(left, right);
            
        } else {
            return f;
            
        }
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
        
        for (Formula pc : pcs) {
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
    public Map<String, Formula> getFeatureEffects(List<SourceFile> files) {
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
            cmProvider.start(config.getCodeConfiguration());
            
            List<SourceFile> files = new ArrayList<>(1000);
            
            SourceFile file;
            while ((file = cmProvider.getNext()) != null) {
                files.add(file);
            }
            
            Map<String, Formula> result = getFeatureEffects(files);
            
            PrintStream out = createResultStream("feature_effects.csv");
            for (Map.Entry<String, Formula> entry : result.entrySet()) {
                if (isRelevant(entry.getKey())) {
                    out.print(entry.getKey());
                    out.print(";");
                    out.print(entry.getValue());
                    out.println();
                }
            }
            out.close();
            
            out = createResultStream("unparsable_files.txt");
            CodeExtractorException exc;
            while ((exc = cmProvider.getNextException()) != null) {
                out.println(exc.getCausingFile().getPath() + ": " + exc.getCause());
            }
            out.close();
            
        } catch (SetUpException e) {
            LOGGER.logException("Error while starting cm provider", e);
        } catch (ExtractorException e) {
            LOGGER.logException("Error running extractor", e);
        }
    }

}
