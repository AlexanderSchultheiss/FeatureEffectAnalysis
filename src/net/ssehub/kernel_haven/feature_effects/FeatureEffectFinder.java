package net.ssehub.kernel_haven.feature_effects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.feature_effects.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.feature_effects.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.feature_effects.Settings.SimplificationType;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.DisjunctionQueue;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * A component that finds feature effects for variables.
 *  
 * @author Adam
 */
public class FeatureEffectFinder extends AnalysisComponent<VariableWithFeatureEffect> {

    /**
     * A variable together with its feature effect formula.
     * 
     * @author Adam
     */
    @TableRow
    public static class VariableWithFeatureEffect {
        
        private String variable;
        
        private Formula featureEffect;

        /**
         * Creates a new feature effect result.
         * 
         * @param variable The variable name.
         * @param featureEffect The feature effect of the given variable. Must not be <code>null</code>.
         */
        public VariableWithFeatureEffect(String variable, Formula featureEffect) {
            this.variable = variable;
            this.featureEffect = featureEffect;
        }
        
        /**
         * Returns the variable name.
         * 
         * @return The name of the variable.
         */
        @TableElement(name = "Variable", index = 0)
        public String getVariable() {
            return variable;
        }
        
        /**
         * Returns the feature effect formula for this variable.
         * 
         * @return The feature effect, never <code>null</code>.
         */
        @TableElement(name = "Feature Effect", index = 1)
        public Formula getFeatureEffect() {
            return featureEffect;
        }
        
        @Override
        public String toString() {
            return "FeatureEffect[" + variable + "] = " + featureEffect.toString();
        }
        
    }
    
    private AnalysisComponent<VariableWithPcs> pcFinder;
    
    private PresenceConditionAnalysisHelper helper;
    private SimplificationType simplifyType;
    private FormulaSimplifier simplifier = null;
    
    /**
     * Creates a new {@link FeatureEffectFinder} for the given PC finder.
     * 
     * @param config The global configuration.
     * @param pcFinder The component to get the PCs from.
     * 
     * @throws SetUpException If creating this component fails.
     */
    public FeatureEffectFinder(Configuration config, AnalysisComponent<VariableWithPcs> pcFinder)
            throws SetUpException {
        
        super(config);
        this.pcFinder = pcFinder;
        this.helper = new PresenceConditionAnalysisHelper(config);
        simplifyType = helper.getSimplificationMode();
        if (simplifyType.ordinal() >= SimplificationType.PRESENCE_CONDITIONS.ordinal()) {
            // Will throw an exception if CNF Utils are not present (but was selected by user in configuration file)
            simplifier = new FormulaSimplifier();
        }
    }

    @Override
    protected void execute() {
        
        VariableWithPcs pcs;
        while ((pcs = pcFinder.getNextResult()) != null) {
            if (helper.isRelevant(pcs.getVariable())) {
                addResult(new VariableWithFeatureEffect(
                        helper.doReplacements(pcs.getVariable()),
                        helper.doReplacements(buildFeatureEffefct(pcs))
                ));
            }
        }
        
    }
    
    /**
     * Replaces each occurrence of a variable with a constant.
     * 
     * TODO: move this to general utils.
     * 
     * @param formula The formula to replace the variable in; this formula is not altered.
     * @param variable The variable to replace.
     * @param value Which constant the variable should be replaced with.
     * @param exactMatch Whether the variable name has to match exactly. If <code>false</code>, then startsWith()
     *      is used to find matches to replace.
     * 
     * @return A new Formula equal to the given formula, but with each occurrence of the variable replaced.
     */
    private Formula setToValue(Formula formula, String variable, boolean value, boolean exactMatch) {
        Formula result;
        
        if (formula instanceof Variable) {
            Variable var = (Variable) formula;
            boolean replace;
            
            if (exactMatch) {
                replace = var.getName().equals(variable);
            } else {
                replace = var.getName().startsWith(variable);
            }
            
            if (replace) {
                result = (value ? True.INSTANCE : False.INSTANCE);
            } else {
                result = var;
            }
            
        } else if (formula instanceof Negation) {
            result = new Negation(setToValue(((Negation) formula).getFormula(), variable, value, exactMatch));
            
        } else if (formula instanceof Disjunction) {
            result = new Disjunction(
                    setToValue(((Disjunction) formula).getLeft(), variable, value, exactMatch),
                    setToValue(((Disjunction) formula).getRight(), variable, value, exactMatch));
            
        } else if (formula instanceof Conjunction) {
            result = new Conjunction(
                    setToValue(((Conjunction) formula).getLeft(), variable, value, exactMatch),
                    setToValue(((Conjunction) formula).getRight(), variable, value, exactMatch));
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
     * TODO: move this to general utils.
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
                result = False.INSTANCE;
                
            } else if (nested instanceof False) {
                result = True.INSTANCE;
                
            } else {
                result = new Negation(nested);
            }
            
        } else if (formula instanceof Disjunction) {
            Formula left = simplify(((Disjunction) formula).getLeft());
            Formula right = simplify(((Disjunction) formula).getRight());
            
            if (left instanceof True || right instanceof True) {
                result = True.INSTANCE;
                
            } else if (left instanceof False && right instanceof False) {
                result = False.INSTANCE;
                
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
                result = False.INSTANCE;
             
            } else if (left instanceof True && right instanceof True) {
                result = True.INSTANCE;
                
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
     * <code>Or over (for each PC in PCs ( PC[variable &lt;- true] XOR PC[variable &lt;- false] ))</code>.
     * 
     * 
     * @param varWithPcs The variable and all presence condition that the variable appears in.
     * @return A formula representing the feature effect of the variable.
     */
    private Formula buildFeatureEffefct(VariableWithPcs varWithPcs) {
        String variable = varWithPcs.getVariable();
        Collection<Formula> pcs = varWithPcs.getPcs();
        boolean simplify = simplifyType.ordinal() >= SimplificationType.PRESENCE_CONDITIONS.ordinal();

        // This eliminates "duplicated" formulas, this is not done in simplifications for presence conditions.
        pcs = simplify ? FeatureEffectReducer.simpleReduce(variable, pcs) : pcs;

        // Check if presence conditions have already been simplified in earlier step
        if (simplifyType.ordinal() > SimplificationType.PRESENCE_CONDITIONS.ordinal()) {
            // Simplification wasn't applied to separate presence conditions before, do this here
            List<Formula> tmp = new ArrayList<>(pcs.size());
            for (Formula formula : pcs) {
                tmp.add(simplifier.simplify(formula));
            }
            pcs = tmp;
        }
        
        Formula result = createXorTree(variable, simplify, pcs);
        if (helper.isNonBooleanReplacements()) {
            int index = variable.indexOf("_eq_");
            
            if (index != -1) {
                String varBaseName = variable.substring(0, index);
                result = setToValue(result, varBaseName + "_", false, false);
            }
        }
        
        Formula simplifiedResult;
        if (simplify) {
            // Perform a simplification on the final result
            simplifiedResult = simplifier.simplify(result);
        } else {
            // At least try to resolve all the (unnecessary) XORs
            simplifiedResult = simplify(result);
        }
        
        return simplifiedResult;
    }

    /**
     * Creates the disjunction of the XOR elements as needed by the Feature effect algorithm.
     * @param variable The variable name for which we currently compute the feature effect.
     * @param simplify <tt>true</tt> if the result should be simplified
     * @param pcs The presence conditions relevant for the variable.
     * @return The feature effect constraint (pre-condition).
     */
    private Formula createXorTree(String variable, boolean simplify, Collection<Formula> pcs) {
        DisjunctionQueue innerElements;
        DisjunctionQueue xorTrees;
        if (null != simplifier) {
            innerElements = new DisjunctionQueue(true, f -> simplifier.simplify(f));
            xorTrees = new DisjunctionQueue(simplify, f -> simplifier.simplify(f));
        } else {
            innerElements = new DisjunctionQueue(true);
            xorTrees = new DisjunctionQueue(simplify);
        }
        
        for (Formula pc : pcs) {
            //      A xor B
            // <==> (A || B) && (!A || !B)
            Formula trueFormula = setToValue(pc, variable, true, true);
            Formula falseFormula = setToValue(pc, variable, false, true);
            
            // (A || B)
            innerElements.add(trueFormula);
            innerElements.add(falseFormula);
            Formula atLeastOnePositive = innerElements.getDisjunction(variable);
            
            // (!A || !B)
            innerElements.add(new Negation(trueFormula));
            innerElements.add(new Negation(falseFormula));
            Formula atLeastOneNegative = innerElements.getDisjunction(variable);
            
            Formula xor;
            if (atLeastOnePositive == null && atLeastOneNegative != null) {
                xor = atLeastOneNegative;
            } else if (atLeastOnePositive != null && atLeastOneNegative == null) {
                xor = atLeastOnePositive;
            } else {
                xor = new Conjunction(atLeastOnePositive, atLeastOneNegative);
            }
            xorTrees.add(xor);
        }
        
        Formula result = xorTrees.getDisjunction(variable);
        return result;
    }

    @Override
    public String getResultName() {
        return "Feature Effects";
    }
    
}
