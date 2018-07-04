package net.ssehub.kernel_haven.fe_analysis.fes;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureRelations.FeatureDependencyRelation;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A component that prints only depends on relationships between two features.
 *  
 * @author Sascha El-Sharkawy
 */
public class FeatureRelations extends AnalysisComponent<FeatureDependencyRelation> {

    /**
     * Stores feature relationships without any constraints.
     */
    @TableRow(isRelation = true)
    public static class FeatureDependencyRelation {
        
        private String feature;
        
        private String dependsOn;

        /**
         * Creates this object.
         * @param feature Value 1.
         * @param dependsOn Value 2.
         */
        public FeatureDependencyRelation(String feature, String dependsOn) {
            this.feature = feature;
            this.dependsOn = dependsOn;
        }

        /**
         * The (dependent) feature variable.
         * 
         * @return The (dependent) feature variable.
         */
        @TableElement(index = 1, name = "Feature")
        public String getFeature() {
            return feature;
        }
        
        /**
         * A feature variable from which the first feature depends on.
         * 
         * @return A feature variable from which the first feature depends on.
         */
        @TableElement(index = 2, name = "Depends On")
        public String getDependsOn() {
            return dependsOn;
        }
        
    }
    
    /**
     * The component to get the input feature effects from.
     */
    private @NonNull AnalysisComponent<VariableWithFeatureEffect> feFinder;
    
    /**
     * Creates a new {@link FeatureRelations} for the given PC finder.
     * 
     * @param config The global configuration.
     * @param feFinder The component to get the feature effects from.
     * 
     * @throws SetUpException If creating this component fails.
     */
    public FeatureRelations(@NonNull Configuration config,
        @NonNull AnalysisComponent<VariableWithFeatureEffect> feFinder) throws SetUpException {
        
        super(config);
        this.feFinder = feFinder;
    }

    @Override
    protected void execute() {
        VariableFinder varFinder = new VariableFinder();
        
        VariableWithFeatureEffect var;
        while ((var = feFinder.getNextResult()) != null) {
            var.getFeatureEffect().accept(varFinder);
            if (!varFinder.getVariableNames().isEmpty()) {
                for (String dependsOnVar : varFinder.getVariableNames()) {
                    addResult(new FeatureDependencyRelation(var.getVariable(), dependsOnVar));                    
                }
            } else {
                addResult(new FeatureDependencyRelation(var.getVariable(), "TRUE"));
            }
            varFinder.clear();
        }
        
    }

    @Override
    public String getResultName() {
        return "Feature Dependency Relations";
    }
    
}
