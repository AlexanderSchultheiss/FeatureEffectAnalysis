package net.ssehub.kernel_haven.fe_analysis.fes;

import java.util.Collection;

import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.logic_utils.SimplifyingDisjunctionQueue;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.DisjunctionQueue;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.FormulaSimplifier;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.VariableValueReplacer;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Helper class for computing feature effects.
 * 
 * @author Adam
 */
public class FeatureEffectComputer {
    
    private boolean simplify;
    
    private boolean hasNonBooleanReplacement;
    
    /**
     * Creates a {@link FeatureEffectComputer}.
     * 
     * @param simplify Whether to use a more powerful simplification.
     */
    public FeatureEffectComputer(boolean simplify) {
        this.simplify = simplify;
        this.hasNonBooleanReplacement = false;
    }
    
    /**
     * Creates a {@link FeatureEffectComputer}.
     * 
     * @param simplify Whether to use a more powerful simplification.
     * @param hasNonBooleanReplacement Whether the formulas contain non boolean replacements (e.g. _eq_).
     */
    public FeatureEffectComputer(boolean simplify, boolean hasNonBooleanReplacement) {
        this.simplify = simplify;
        this.hasNonBooleanReplacement = hasNonBooleanReplacement;
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
    public @NonNull Formula buildFeatureEffefct(@NonNull VariableWithPcs varWithPcs) {
        String variable = varWithPcs.getVariable();
        Collection<@NonNull Formula> pcs = varWithPcs.getPcs();

        // This eliminates "duplicated" formulas, this is not done in simplifications for presence conditions.
        // TODO: remove
        // pcs = simplifier != null ? FeatureEffectReducer.simpleReduce(variable, pcs) : pcs;

        // Check if presence conditions have already been simplified in earlier step
        // TODO: remove?
//        if (helper.getSimplificationMode().ordinal() > SimplificationType.PRESENCE_CONDITIONS.ordinal()) {
//            // Simplification wasn't applied to separate presence conditions before, do this here
//            List<@NonNull Formula> tmp = new ArrayList<>(pcs.size());
//            for (Formula formula : pcs) {
//                tmp.add(FormulaSimplifier.simplify(formula));
//            }
//            pcs = tmp;
//        }
        
        Formula result = createXorTree(variable, pcs);
        if (this.hasNonBooleanReplacement) {
            int index = variable.indexOf("_eq_");
            
            if (index != -1) {
                String varBaseName = variable.substring(0, index);
                result = result.accept(new VariableValueReplacer(varBaseName + "_eq_", false, false));
            }
        }
        
        Formula simplifiedResult;
        if (simplify) {
            // Perform a simplification on the final result: Logical simplification
            simplifiedResult = FormulaSimplifier.simplify(result);
        } else {
            // At least remove the constants left from the XORs: Make constraints only readable
            simplifiedResult = FormulaSimplifier.defaultSimplifier(result);
        }
        
        return simplifiedResult;
    }

    /**
     * Creates the disjunction of the XOR elements as needed by the Feature effect algorithm.
     * 
     * @param variable The variable name for which we currently compute the feature effect.
     * @param pcs The presence conditions relevant for the variable.
     * @return The feature effect constraint (pre-condition).
     */
    private @NonNull Formula createXorTree(@NonNull String variable, @NonNull Collection<@NonNull Formula> pcs) {
        
        DisjunctionQueue innerElements;
        DisjunctionQueue xorTrees;
        
        if (this.simplify) {
            innerElements = new DisjunctionQueue(true, FormulaSimplifier::simplify);
            xorTrees = new SimplifyingDisjunctionQueue();
        } else {
            innerElements = new DisjunctionQueue(true);
            xorTrees = new DisjunctionQueue(true);
        }
        
        for (Formula pc : pcs) {
            //      A xor B
            // <==> (A || B) && (!A || !B)
            Formula trueFormula = pc.accept(new VariableValueReplacer(variable, true, true));
            Formula falseFormula = pc.accept(new VariableValueReplacer(variable, false, true));
            
            // (A || B)
            innerElements.add(trueFormula);
            innerElements.add(falseFormula);
            Formula atLeastOnePositive = innerElements.getDisjunction(variable);
            
            // (!A || !B)
            innerElements.add(new Negation(trueFormula));
            innerElements.add(new Negation(falseFormula));
            Formula atLeastOneNegative = innerElements.getDisjunction(variable);
            
            Formula xor;
            
            if (atLeastOnePositive == True.INSTANCE) {
                // TRUE AND atLeastOneNegative <-> atLeastOneNegative
                xor = atLeastOneNegative;
                
            } else if (atLeastOneNegative == True.INSTANCE) {
                // TRUE AND atLeastOnePositive <-> atLeastOnePositive
                xor = atLeastOnePositive;
                
            } else if (False.INSTANCE == atLeastOnePositive || False.INSTANCE == atLeastOneNegative) {
                // FALSE AND x <-> FALSE
                xor = False.INSTANCE;
                
            } else {
                xor = new Conjunction(atLeastOnePositive, atLeastOneNegative);
            }
            xorTrees.add(xor);
        }
        
        Formula result = xorTrees.getDisjunction(variable);
        return result;
    }
    
}
