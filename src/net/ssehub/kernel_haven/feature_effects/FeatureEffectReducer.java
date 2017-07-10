package net.ssehub.kernel_haven.feature_effects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.parser.SubFormulaChecker;

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
     * @param pcs
     * @return
     */
    static Collection<Formula> simpleReduce(Collection<Formula> pcs) {
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
        
        for (Formula pc : orderedPCs) {
            boolean newFormula = true;
            
            // Dismiss new formula "pc" if it is contained in one of the "result" formulas
            for (int i = 0; i < result.size() && newFormula; i++) {
                SubFormulaChecker checker = new SubFormulaChecker(result.get(i));
                pc.accept(checker);
                newFormula = !checker.isNested();
            }
            
            if (newFormula) {
                result.add(pc);
            } else {
                net.ssehub.kernel_haven.util.Logger.get().logInfo("Ommited Feature effect formula: ", pc.toString());
            }
        }
        
        return result;
    }
}
