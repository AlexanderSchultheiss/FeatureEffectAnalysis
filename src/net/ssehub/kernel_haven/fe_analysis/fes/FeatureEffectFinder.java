package net.ssehub.kernel_haven.fe_analysis.fes;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.PresenceConditionAnalysisHelper;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * A component that finds feature effects for variables.
 *  
 * @author Adam
 */
public class FeatureEffectFinder extends AnalysisComponent<VariableWithFeatureEffect> {

    /**
     * A variable together with its feature effect formula.
     * 
     * @author Adam
     */
    @TableRow
    public static class VariableWithFeatureEffect {
        
        private @NonNull String variable;
        
        private @NonNull Formula featureEffect;

        /**
         * Creates a new feature effect result.
         * 
         * @param variable The variable name.
         * @param featureEffect The feature effect of the given variable. Must not be <code>null</code>.
         */
        public VariableWithFeatureEffect(@NonNull String variable, @NonNull Formula featureEffect) {
            this.variable = variable;
            this.featureEffect = featureEffect;
        }
        
        /**
         * Returns the variable name.
         * 
         * @return The name of the variable.
         */
        @TableElement(name = "Variable", index = 0)
        public @NonNull String getVariable() {
            return variable;
        }
        
        /**
         * Returns the feature effect formula for this variable.
         * 
         * @return The feature effect, never <code>null</code>.
         */
        @TableElement(name = "Feature Effect", index = 1)
        public @NonNull Formula getFeatureEffect() {
            return featureEffect;
        }
        
        @Override
        public @NonNull String toString() {
            return "FeatureEffect[" + variable + "] = " + featureEffect.toString();
        }
        
        @Override
        public int hashCode() {
            return variable.hashCode() + featureEffect.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            boolean equal = false;
            if (obj instanceof VariableWithFeatureEffect) {
                VariableWithFeatureEffect other = (VariableWithFeatureEffect) obj;
                equal = this.variable.equals(other.variable) && this.featureEffect.equals(other.featureEffect);
            }
            return equal;
        }
        
    }
    
    /**
     * The component to get the input PCs from.
     */
    protected @NonNull AnalysisComponent<VariableWithPcs> pcFinder;
    
    private @NonNull PresenceConditionAnalysisHelper helper;
    
    private @NonNull FeatureEffectComputer computer;
    
    private @Nullable FeatureEffectStorage storage;
    
    /**
     * Creates a new {@link FeatureEffectFinder} for the given PC finder.
     * 
     * @param config The global configuration.
     * @param pcFinder The component to get the PCs from.
     * 
     * @throws SetUpException If creating this component fails.
     */
    public FeatureEffectFinder(@NonNull Configuration config, @NonNull AnalysisComponent<VariableWithPcs> pcFinder)
            throws SetUpException {
        
        super(config);
        this.pcFinder = pcFinder;
        this.helper = new PresenceConditionAnalysisHelper(config);
        
        boolean simplify = helper.getSimplificationMode().ordinal() >= SimplificationType.PRESENCE_CONDITIONS.ordinal();
        this.computer = new FeatureEffectComputer(simplify, helper.isNonBooleanReplacements());
        
        if (helper.isNonBooleanMode()) {
            storage = new FeatureEffectStorage();
        }
    }

    @Override
    protected void execute() {
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        VariableWithPcs pcs;
        while ((pcs = pcFinder.getNextResult()) != null) {
            VariableWithFeatureEffect result = processSingle(pcs);
            if (result != null) {
                addResult(result);
            }
            
            progress.oneDone();
        }
        
        progress.close();
    }
    
    /**
     * Calculates the feature effect for a single variable.
     * 
     * @param pcs The variable with presence conditions to calculate the feature effect for.
     * @return The variable with the calculated feature effect. <code>null</code> if the variable was not relevant.
     */
    protected @Nullable VariableWithFeatureEffect processSingle(@NonNull VariableWithPcs pcs) {
        VariableWithFeatureEffect result = null;
        
        String varName = pcs.getVariable();
        if (helper.isRelevant(varName)) {
            Formula feConstraint = helper.doReplacements(computer.buildFeatureEffefct(pcs));
            varName = helper.doReplacements(varName);
            
            if (null != storage) {
                VariableWithFeatureEffect baseVar = storage.getBaseVariable(varName);
                if (null != baseVar && baseVar.featureEffect != feConstraint) {
                    feConstraint = new Disjunction(baseVar.featureEffect, feConstraint);
                }
            }
            
            result = new VariableWithFeatureEffect(varName, feConstraint);
            
            if (null != storage) {
                storage.add(result);
            }
        }
        
        
        return result;
    }
    
    @Override
    public String getResultName() {
        return "Feature Effects";
    }
    
}
