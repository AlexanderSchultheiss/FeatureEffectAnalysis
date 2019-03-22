package net.ssehub.kernel_haven.fe_analysis.arch_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.DisjunctionQueue;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.IVoidFormulaVisitor;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * An component for splitting up {@link VariableWithFeatureEffect}s based on architecture component mapping.
 * 
 * @author Adam
 */
public class ArchComponentResolver extends AnalysisComponent<FeatureEffectWithArchComponent> {
    
    private static final Pattern OPERATOR_PATTERN = Pattern.compile("(=|<|>|>=|<=|!=|\\+|\\*|\\-|/|%|\\||&)");

    private @NonNull AnalysisComponent<ArchComponentStorage> componentInput;
    
    private @NonNull AnalysisComponent<VariableWithFeatureEffect> feInput;
    
    /**
     * Creates a new {@link ArchComponentResolver}.
     * 
     * @param config The pipeline configuration.
     * @param feInput The input to get the feature effects from.
     * @param componentInput The input to get the architecture components from.
     */
    public ArchComponentResolver(@NonNull Configuration config,
            @NonNull AnalysisComponent<VariableWithFeatureEffect> feInput,
            @NonNull AnalysisComponent<ArchComponentStorage> componentInput) {
        
        super(config);
        
        this.componentInput = componentInput;
        this.feInput = feInput;
    }
    
    /**
     * Splits the given formula at the top-level {@link Disjunction} operators. <code>A || (B && C)</code> will result
     * in the list <code>[A, B && C]</code>. If no {@link Disjunction} is at the top, then the result list will simply
     * contain formula.
     * 
     * @param formula The formula to split.
     * @param result The list to add the resulting formula parts to.
     */
    private static void splitAtOr(Formula formula, List<Formula> result) {
        if (formula instanceof Disjunction) {
            Disjunction dis = (Disjunction) formula;
            splitAtOr(dis.getLeft(), result);
            splitAtOr(dis.getRight(), result);
        } else {
            result.add(formula);
        }
    }

    /**
     * Finds what kinds of arch components are used inside a formula.
     */
    private static class ComponentFinder implements IVoidFormulaVisitor {
     
        private @NonNull ArchComponentStorage componentStorage;
        
        private @NonNull String feVar = ""; // will be initialized in reset()
        
        private boolean foundSame;
        
        private boolean foundOther;
        
        /**
         * Creates a {@link ComponentFinder}.
         * 
         * @param componentStorage The storage to get architecture components from.
         */
        public ComponentFinder(@NonNull ArchComponentStorage componentStorage) {
            this.componentStorage = componentStorage;
        }
        
        /**
         * Resets this class.
         * 
         * @param feVar The variable to run the next visitation for.
         */
        public void reset(@NonNull String feVar) {
            this.feVar = feVar;
            this.foundSame = false;
            this.foundOther = false;
        }
        
        @Override
        public void visitVariable(@NonNull Variable variable) {
            String varName = variable.getName();
            // only use base name up to <= etc.
            Matcher m = OPERATOR_PATTERN.matcher(varName);
            if (m.find()) {
                varName = notNull(varName.substring(0, m.start()));
            }
            
            if (componentStorage.isSameComponent(feVar, varName)) {
                foundSame = true;
            } else {
                foundOther = true;
            }
        }
        
        @Override
        public void visitTrue(@NonNull True trueConstant) {
        }
        
        @Override
        public void visitNegation(@NonNull Negation formula) {
            formula.getFormula().accept(this);
        }
        
        @Override
        public void visitFalse(@NonNull False falseConstant) {
        }
        
        @Override
        public void visitDisjunction(@NonNull Disjunction formula) {
            formula.getLeft().accept(this);
            formula.getRight().accept(this);
        }
        
        @Override
        public void visitConjunction(@NonNull Conjunction formula) {
            formula.getLeft().accept(this);
            formula.getRight().accept(this);
        }
        
    }
    
    @Override
    protected void execute() {
        ArchComponentStorage componentStorage = componentInput.getNextResult();
        if (componentStorage == null) {
            throw new RuntimeException("ArchComponentStorage is null");
        }
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        ComponentFinder finder = new ComponentFinder(componentStorage);
        
        VariableWithFeatureEffect fe;
        while ((fe = feInput.getNextResult()) != null) {
            
            List<Formula> splitted = new ArrayList<>();
            splitAtOr(fe.getFeatureEffect(), splitted);
            
            String feVar = fe.getVariable();
            DisjunctionQueue sameComponent = new DisjunctionQueue(false);
            DisjunctionQueue mixedComponent = new DisjunctionQueue(false);
            DisjunctionQueue otherComponent = new DisjunctionQueue(false);
            
            for (Formula f : splitted) {
                finder.reset(feVar);
                f.accept(finder);
                
                if (finder.foundSame) {
                    if (finder.foundOther) {
                        mixedComponent.add(f);
                    } else {
                        sameComponent.add(f);
                    }
                } else {
                    if (finder.foundOther) {
                        otherComponent.add(f);
                    } else {
                        // if no variables are found (neither same nor other), then just assume its same component
                        sameComponent.add(f);
                    }
                }
                
            }
            
            
            addResult(new FeatureEffectWithArchComponent(feVar, sameComponent.getDisjunction(),
                    mixedComponent.getDisjunction(), otherComponent.getDisjunction()));
            
            progress.processedOne();
        }
        
        progress.close();
    }

    @Override
    public @NonNull String getResultName() {
        return "FEs Component Split";
    }

}
