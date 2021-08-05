package net.ssehub.kernel_haven.fe_analysis.pcs;

import net.ssehub.kernel_haven.util.logic.*;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

import java.util.Set;
import java.util.regex.Pattern;

public class FeatureFilter implements IFormulaVisitor<Formula> {
    private final Set<String> features;
    private int filtered = 0;
    private int kept = 0;
    private int constants = 0;
    private final Pattern splitPattern = Pattern.compile("=");

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
        String name = variable.getName().contains("=") ? splitPattern.split(variable.getName())[0].trim() : variable.getName();
        if (features.contains(name)) {
            kept++;
            return variable;
        } else {
            filtered++;
            return True.INSTANCE;
        }
    }

    @Override
    public Formula visitNegation(@NonNull Negation formula) {
        Formula inner = formula.getFormula().accept(this);
        // Added manual handling of constants, because it was causing '!1' and '!0' as PC
        if (inner instanceof True) {
            return False.INSTANCE;
        } else if (inner instanceof False) {
            return True.INSTANCE;
        } else {
            return FormulaBuilder.not(inner);
        }
    }

    @Override
    public Formula visitDisjunction(@NonNull Disjunction formula) {
        Formula left = formula.getLeft();
        left = left.accept(this);


        Formula right = formula.getRight();
        right = right.accept(this);

        return FormulaBuilder.or(left, right);
    }

    @Override
    public Formula visitConjunction(@NonNull Conjunction formula) {
        Formula left = formula.getLeft();
        left = left.accept(this);

        Formula right = formula.getRight();
        right = right.accept(this);

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
