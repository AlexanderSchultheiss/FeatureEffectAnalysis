package net.ssehub.kernel_haven.fe_analysis.pcs;

import net.ssehub.kernel_haven.util.logic.*;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

import java.util.Set;

public class FeatureFilter implements IFormulaVisitor<Formula> {
    private final Set<String> features;
    private int filtered = 0;
    private int kept = 0;
    private int constants = 0;

    public FeatureFilter(Set<String> features) {
        this.features = features;
    }

    @Override
    public Formula visitFalse(@NonNull False falseConstant) {
        constants++;
        return falseConstant;
    }

    @Override
    public Formula visitTrue(@NonNull True trueConstant) {
        constants++;
        return trueConstant;
    }

    @Override
    public Formula visitVariable(@NonNull Variable variable) {
        if (features.contains(variable.getName())) {
            kept++;
            return variable;
        } else {
            filtered++;
            return True.INSTANCE;
        }
    }

    @Override
    public Formula visitNegation(@NonNull Negation formula) {
        return FormulaBuilder.not(formula.getFormula().accept(this));
    }

    @Override
    public Formula visitDisjunction(@NonNull Disjunction formula) {
        Formula left = formula.getLeft();
        if (left instanceof Variable) {
            if (!features.contains(((Variable) left).getName())) {
                left = False.INSTANCE;
                filtered++;
            } else {
                kept++;
            }
        } else {
            left = left.accept(this);
        }

        Formula right = formula.getRight();
        if (right instanceof Variable) {
            if (!features.contains(((Variable) right).getName())) {
                right = False.INSTANCE;
                filtered++;
            } else {
                kept++;
            }
        } else {
            right = right.accept(this);
        }
        return FormulaBuilder.or(left, right);
    }

    @Override
    public Formula visitConjunction(@NonNull Conjunction formula) {
        Formula left = formula.getLeft();
        if (left instanceof Variable) {
            if (!features.contains(((Variable) left).getName())) {
                left = True.INSTANCE;
                filtered++;
            } else {
                kept++;
            }
        } else {
            left = left.accept(this);
        }

        Formula right = formula.getRight();
        if (right instanceof Variable) {
            if (!features.contains(((Variable) right).getName())) {
                right = True.INSTANCE;
                filtered++;
            } else {
                kept++;
            }
        } else {
            right = right.accept(this);
        }
        return FormulaBuilder.or(left, right);
    }

    public int filtered() {
        return filtered;
    }
    
    public int kept() {
        return kept;   
    }
    
    public int constants() {
        return constants;
    }
}
