package net.ssehub.kernel_haven.feature_effects;

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
    
    public SubFormulaChecker(Formula nestedFormula) {
        super(nestedFormula);
        isNegated = false;
    }
    
    @Override
    public void visitNegation(Negation formula) {
        setNested(formula.equals(getNestedFormula()));
        
        if (!isNested()) {
            isNegated = !isNegated;
            formula.getFormula().accept(this);
        }
    }
    
    @Override
    public boolean isNested() {
        return super.isNested() && !isNegated;
    }

}
