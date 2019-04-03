/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.FormulaSimplifier;
import net.ssehub.kernel_haven.util.logic.IFormulaVisitor;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
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
        
        private Formula context;

        /**
         * Creates this object.
         * 
         * @param feature The feature that depends on another feature.
         * @param dependsOn The variable that feature depends on.
         * @param context The context of this relation.
         */
        public FeatureDependencyRelation(String feature, String dependsOn, Formula context) {
            this.feature = feature;
            this.dependsOn = dependsOn;
            this.context = context;
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

        /**
         * A formula describing the left-over context for the given relation.
         * 
         * @return The context of the relation.
         */
        @TableElement(index = 3, name = "Context")
        public Formula getContext() {
            return context;
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
                        addResult(new FeatureDependencyRelation(variable, dependsOnVar,
                                computeContext(notNull(dependsOnVar), var.getFeatureEffect())));
                    }
                }
            } else {
                if (!storage.elementNotProcessed(variable, "TRUE")) {
                    addResult(new FeatureDependencyRelation(variable, "TRUE", var.getFeatureEffect()));
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
    
    /**
     * Computes the "context" for the given feature effect and the given variable name. The context is defined as the
     * "left-over" part, assuming variableName is selected. That is, variableName is replaced with True in the given
     * feature effect.
     * 
     * @param variableName The variable name that the context should be computed for.
     * @param fe The feature effect for computing the context.
     * 
     * @return The context.
     */
    private @NonNull Formula computeContext(@NonNull String variableName, @NonNull Formula fe) {
        
        Formula context = fe.accept(new IFormulaVisitor<@NonNull Formula>() {

            @Override
            public @NonNull Formula visitFalse(@NonNull False falseConstant) {
                return falseConstant;
            }

            @Override
            public @NonNull Formula visitTrue(@NonNull True trueConstant) {
                return trueConstant;
            }

            @Override
            public @NonNull Formula visitVariable(@NonNull Variable variable) {
                Formula result;
                if (variable.getName().equals(variableName)) {
                    result = True.INSTANCE;
                } else {
                    result = variable;
                }
                return result;
            }

            @Override
            public @NonNull Formula visitNegation(@NonNull Negation formula) {
                return new Negation(formula.getFormula().accept(this));
            }

            @Override
            public @NonNull Formula visitDisjunction(@NonNull Disjunction formula) {
                return new Disjunction(formula.getLeft().accept(this), formula.getRight().accept(this));
            }

            @Override
            public @NonNull Formula visitConjunction(@NonNull Conjunction formula) {
                return new Conjunction(formula.getLeft().accept(this), formula.getRight().accept(this));
            }
        });
        
        return FormulaSimplifier.defaultSimplifier(context);
    }

    @Override
    public String getResultName() {
        return "Feature Dependency Relations";
    }
    
}
