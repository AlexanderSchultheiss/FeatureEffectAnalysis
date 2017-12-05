package net.ssehub.kernel_haven.fe_analysis.fes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Formula;

/**
 * Strategies to simplify feature effect constraints.
 * @author El-Sharkawy
 *
 */
class FeatureEffectReducer {
    
    /**
     * Should not be instantiated.
     */
    private FeatureEffectReducer() {}

    
    /**
     * Checks if a formula contains an already processed formula as sub formula and removes it.
     * @param variable The name of the variable for which it is done, only used for logging debug messages.
     * @param pcs All presence conditions, which belong to a feature and form a feature effect
     * @return An equivalent (sub-) set of the passed constraints.
     */
    static Collection<Formula> simpleReduce(String variable, Collection<Formula> pcs) {
        List<Formula> result = new ArrayList<>();
        List<Formula> orderedPCs = new ArrayList<>(pcs);
        Collections.sort(orderedPCs, new Comparator<Formula>() {

            @Override
            public int compare(Formula formula1, Formula formula2) {
                int result = 0;
                if (formula1.getLiteralSize() < formula2.getLiteralSize()) {
                    result = -1;
                } else if (formula1.getLiteralSize() > formula2.getLiteralSize()) {
                    result = 1;
                }
                return result;
            }
        });
        
        for (int i = 0; i < orderedPCs.size(); i++) {
            Formula pc = orderedPCs.get(i);
            boolean newFormula = true;
            
            // Remove a subformula from "pc" if it is contained in one of the "result" formulas
            for (int j = 0; j < orderedPCs.size() && j < i && newFormula; j++) {
                SubFormulaReplacer replacer = new SubFormulaReplacer(orderedPCs.get(j));
                Formula minimizedPC = replacer.minimize(pc);
                if (null == minimizedPC) {
                    // Discard 
                    newFormula = false;
                    Logger.get().logDebug("Ommited feature effect constraint for feature \""
                        + variable + "\" + constraint: " + pc.toString());
                } else if (minimizedPC != pc) {
                    // Formula was minimized 
                    newFormula = false;
                    Logger.get().logDebug("Feature effect constraint for feature \"" + variable
                        + "\" + was mimized from \"" + pc.toString() + "\" -> \"" + minimizedPC.toString() + "\"");
                }
                // Else: Continue
            }
            
            if (newFormula) {
                result.add(pc);
            }
//            // Remove a subformula from "pc" if it is contained in one of the "result" formulas
//            for (int i = 0; i < result.size() && newFormula; i++) {
//                SubFormulaChecker checker = new SubFormulaChecker(result.get(i));
//                pc.accept(checker);
//                newFormula = !checker.isNested();
//            }
//            
//            if (newFormula) {
//                result.add(pc);
//            } else {
//                Logger.get().logDebug("Ommited feature effect constraint for feature \""
//                    + variable + "\" + sub-constraint: " + pc.toString());
//            }
        }
        
        return result;
    }
}
