package net.ssehub.kernel_haven.fe_analysis.pcs;

import net.ssehub.kernel_haven.util.logic.*;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

import java.util.Set;

public class FeatureFilter implements IFormulaVisitor<Formula> {
    private final Set<String> features;
    private int filtered = 0;
    
    public FeatureFilter(Set<String> features) {
        this.features = features;
    }

    @Override
    public Formula visitFalse(@NonNull False falseConstant) {
        return falseConstant;
    }

    @Override
    public Formula visitTrue(@NonNull True trueConstant) {
        return trueConstant;
    }

    @Override
    public Formula visitVariable(@NonNull Variable variable) {
        if (features.contains(variable.getName())) {
            return variable;
        } else {
            filtered++;
            return True.INSTANCE;
        }
    }

    @Override
    public Formula visitNegation(@NonNull Negation formula) {
        return FormulaBuilder.not(formula.accept(this));
    }

    @Override
    public Formula visitDisjunction(@NonNull Disjunction formula) {
        Formula left = formula.getLeft();
        if (left instanceof Variable) {
            if (!features.contains(((Variable)left).getName())) {
                left = False.INSTANCE;
                filtered++;
            }
        } else {
            left = left.accept(this);
        }

        Formula right = formula.getRight();
        if (right instanceof Variable) {
            if (!features.contains(((Variable)right).getName())) {
                right = False.INSTANCE;
                filtered++;
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
            if (!features.contains(((Variable)left).getName())) {
                left = True.INSTANCE;
                filtered++;
            }
        } else {
            left = left.accept(this);
        }

        Formula right = formula.getRight();
        if (right instanceof Variable) {
            if (!features.contains(((Variable)right).getName())) {
                right = True.INSTANCE;
                filtered++;
            }
        } else {
            right = right.accept(this);
        }
        return FormulaBuilder.or(left, right);
    }
    
    public int filtered() {
        return filtered;
    }
}
