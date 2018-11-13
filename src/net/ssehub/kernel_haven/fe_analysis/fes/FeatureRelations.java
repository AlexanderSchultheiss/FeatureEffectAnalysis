package net.ssehub.kernel_haven.fe_analysis.fes;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureRelations.FeatureDependencyRelation;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;

/**
 * A component that prints only depends on relationships between two features.
 *  
 * @author Sascha El-Sharkawy
 */
public class FeatureRelations extends AnalysisComponent<FeatureDependencyRelation> {
    private static final Pattern OPERATOR_PATTERN = Pattern.compile("(=|<|>|>=|<=|!=|\\+|\\*|\\-|/|%|\\||&)");

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
        FeatureRelationStorage storage = new FeatureRelationStorage();
        VariableFinder varFinder = new VariableFinder();
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        VariableWithFeatureEffect var;
        while ((var = feFinder.getNextResult()) != null) {
            @NonNull String variable = normalizeVariable(var.getVariable());
            var.getFeatureEffect().accept(varFinder);
            if (!varFinder.getVariableNames().isEmpty()) {
                Set<String> dependentVars = new HashSet<>();
                for (String dependsOnVar : varFinder.getVariableNames()) {
                    // Do not track dependencies to value comparisons and keep only the assigned feature
                    // For instance: FEATURE=VALUE -> FEATURE
                    Matcher matcher = OPERATOR_PATTERN.matcher(dependsOnVar);
                    if (matcher.find()) {
                        int pos = matcher.start();
                        // Keep only Feature
                        dependsOnVar = dependsOnVar.substring(0, pos);
                    }
                    if (dependsOnVar != null && !dependsOnVar.isEmpty() && !dependsOnVar.equals(variable)) {
                        // dependsOnVar is trimmed to Feature
                        dependentVars.add(dependsOnVar);
                    }
                }
                for (String dependsOnVar : dependentVars) {
                    // Add all distinct features
                    if (!storage.elementNotProcessed(variable, dependsOnVar)) {
                        addResult(new FeatureDependencyRelation(variable, dependsOnVar));
                    }
                }
            } else {
                if (!storage.elementNotProcessed(variable, "TRUE")) {
                    addResult(new FeatureDependencyRelation(variable, "TRUE"));
                }
            }
            varFinder.clear();
            
            progress.processedOne();
        }

        progress.close();
        
    }
    
    /**
     * Cuts off all elements which come behind an operator.
     * @param variable A feature variable, maybe in the form of <tt>VARIABLE=CONSTANT</tt>.
     * @return Maybe the same instance of a shorter string without any comparison / arithmetic operation.
     */
    private @NonNull String normalizeVariable(@NonNull String variable) {
        Matcher matcher = OPERATOR_PATTERN.matcher(variable);
        if (matcher.find()) {
            int pos = matcher.start();
            // Keep only Feature
            variable = NullHelpers.notNull(variable.substring(0, pos).trim());
        }
        
        return variable;
    }

    @Override
    public String getResultName() {
        return "Feature Dependency Relations";
    }
    
}
