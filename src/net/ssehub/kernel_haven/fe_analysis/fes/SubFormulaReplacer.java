package net.ssehub.kernel_haven.fe_analysis.fes;

import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.IVoidFormulaVisitor;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Checks if the given <tt>original</tt> is contained in the <tt>other</tt> formula and will eliminate the doubled
 * part form <tt>other</tt>.
 * @author El-Sharkawy
 *
 */
class SubFormulaReplacer implements IVoidFormulaVisitor {
    
    private Formula original;
    private Formula tmp;
    
    /**
     * Single constructor to check for if another formula contains <tt>original</tt>.
     * @param original The formula, which should be checked if it is contained inside the <tt>other</tt> formula, won't
     * be changed.
     */
    SubFormulaReplacer(Formula original) {
        this.original = original;
    }
    
    /**
     * Checks if <tt>other</tt> contains <tt>original</tt> and will return a minimized {@link Formula}.
     * @param other The formula, which may contains <tt>original</tt>.
     * @return <ul>
     *     <li><tt>null</tt> if both formulas a identical</li>
     *     <li>A new, smaller formula, if <tt>original</tt> was contained in <tt>other</tt></li>
     *     <li><tt>other</tt> if <tt>original</tt> is not a sub formula of <tt>other</tt></li>
     * </ul> 
     */
    public Formula minimize(Formula other) {
        Formula result = other;
        
        if (null != original) {
            if (original.equals(other)) {
                result = null;
            } else {
                SubFormulaChecker checker = new SubFormulaChecker(other);
                checker.visit(original);
                if (checker.isNested()) {
                    // Rewrite the "other" formula
                    visit(other);
                    result = tmp;
                }
            }
        }
        
        return result;
    }

    @Override
    public void visitFalse(False falseConstant) {
        if (falseConstant.equals(original)) {
            tmp = null;
        } else {
            tmp = False.INSTANCE;
        }
    }

    @Override
    public void visitTrue(True trueConstant) {
        if (trueConstant.equals(original)) {
            tmp = null;
        } else {
            tmp = False.INSTANCE;
        }
    }

    @Override
    public void visitVariable(Variable variable) {
        if (variable.equals(original)) {
            tmp = null;
        } else {
            tmp = variable;
        }
    }

    @Override
    public void visitNegation(Negation formula) {
        if (formula.equals(original)) {
            tmp = null;
        } else {
            visit(formula.getFormula());
            if (null != tmp) {
                tmp = new Negation(tmp);
            } else {
                tmp = False.INSTANCE;
            }
        }
    }

    @Override
    public void visitDisjunction(Disjunction formula) {
        if (formula.equals(original)) {
            tmp = null;
        } else {
            visit(formula.getLeft());
            Formula left = tmp;
            visit(formula.getRight());
            Formula right = tmp;
            if (null != left && null != right) {
                tmp = new Disjunction(left, right);
            } else {
                // One side is permanently fulfilled (or both side are fulfilled)
                tmp = null;
            }
        }
    }

    @Override
    public void visitConjunction(Conjunction formula) {
        if (formula.equals(original)) {
            tmp = null;
        } else {
            visit(formula.getLeft());
            Formula left = tmp;
            visit(formula.getRight());
            Formula right = tmp;
            if (null != left && null != right) {
                tmp = new Conjunction(left, right);
            } else if (null == left && null == right) {
                tmp = null;
            } else {
                // One won't be null
                tmp = left == null ? right : left;
            }
        }
    }
}
