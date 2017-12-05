package net.ssehub.kernel_haven.fe_analysis.fes;

import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;

/**
 * A {@link net.ssehub.kernel_haven.util.logic.parser.SubFormulaChecker}, which does <b>not</b> consider negated
 * formulas.
 * @author El-Sharkawy
 *
 */
public class SubFormulaChecker extends net.ssehub.kernel_haven.util.logic.parser.SubFormulaChecker {

    private boolean isNegated;
    
    /**
     * Sole constructor for this visitor.
     * The accept method must still be called.
     * @param nestedFormula The formula to check if it is nested inside the visited formula.
     */
    public SubFormulaChecker(Formula nestedFormula) {
        super(nestedFormula);
        isNegated = false;
    }
    
    @Override
    public void visitNegation(Negation formula) {
        setNested(formula.equals(getNestedFormula()));
        
        if (!isNested()) {
            boolean oldStatus = isNegated;
            
            // Change the status only fur current part of formula
            isNegated = !isNegated;
            visit(formula.getFormula());
            
            isNegated = oldStatus;
        }
    }
    
    @Override
    public boolean isNested() {
        return super.isNested() && !isNegated;
    }

}
