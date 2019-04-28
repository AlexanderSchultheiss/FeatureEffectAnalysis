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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.Settings;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.StringUtils;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.logic.DisjunctionQueue;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.FormulaSimplifier;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;

/**
 * Aggregates feature effect constraints for values of the same variable. Only relevant in Pseudo-Boolean settings.
 * <p>
 * Example:
 * <pre>{@code
 * VAR=1 -> A
 * VAR=2 -> B
 * 
 * => Var -> A || B
 * }</pre>
 * 
 * @author El-Sharkawy
 *
 */
public class FeAggregator extends AnalysisComponent<VariableWithFeatureEffect> {
    
    private @NonNull AnalysisComponent<VariableWithFeatureEffect> feDetector;
    private boolean simplify = false;

    /**
     * Creates an {@link FeAggregator}, do create one constraint for the separated values of integer variables.
     * @param config The global configuration.
     * @param feDetector Probably {@link FeatureEffectFinder}.
     * @throws SetUpException If creating this component fails, probably if simplification should is turned on, but
     *     LogicUtils are not present in plug-ins directory.
     */
    public FeAggregator(@NonNull Configuration config, @NonNull AnalysisComponent<VariableWithFeatureEffect> feDetector)
        throws SetUpException {
        
        super(config);
        this.feDetector = feDetector;
        
        simplify = config.getValue(Settings.SIMPLIFIY).ordinal() >= SimplificationType.PRESENCE_CONDITIONS.ordinal();
    }

    @Override
    protected void execute() {
        Map<@NonNull String, DisjunctionQueue> groupedQueues = new HashMap<>();
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        VariableWithFeatureEffect var;
        while ((var = feDetector.getNextResult()) != null) {
            @NonNull String varName = var.getVariable();
            int lastIndex = StringUtils.getLastOperatorIndex(varName);
            if (-1 != lastIndex) {
                varName = NullHelpers.notNull(varName.substring(0, lastIndex));
            }
            
            DisjunctionQueue conditions = groupedQueues.get(varName);
            if (null == conditions) {

                // New variable, check if we can (partially) process already gathered values
                if (!groupedQueues.isEmpty()) {
                    boolean containsSimilarVariables = false;
                    for (String otherVariable : groupedQueues.keySet()) {
                        if (varName.startsWith(otherVariable)) {
                            containsSimilarVariables = true;
                            break;
                        }
                    }
                    
                    if (!containsSimilarVariables) {
                        // Keep the map as small as possible and facilitate multi-threading of analysis components
                        aggregateFeatureEffects(groupedQueues);
                    }
                }
                
                // Start processing of the new (identified) variable
                conditions = new DisjunctionQueue(simplify, FormulaSimplifier::simplify);
                groupedQueues.put(varName, conditions);
            }
            
            // Store effect to allow aggregation in aggregateFeatureEffects-method.
            conditions.add(var.getFeatureEffect());
            
            progress.processedOne();
        }
        
        // Process very last elements
        aggregateFeatureEffects(groupedQueues);
        
        progress.close();
    }

    /**
     * Aggregates feature effects for the elements of the queue, clears the map, and send the results in an ordered
     * list to the next analysis component.
     * @param groupedQueues A list of tuples (variable name, collected feature effects).
     */
    private void aggregateFeatureEffects(Map<@NonNull String, DisjunctionQueue> groupedQueues) {
        // Compute aggregated feature effects for all elements of the map
        List<@NonNull VariableWithFeatureEffect> results = new ArrayList<>(groupedQueues.size());
        for (Map.Entry<@NonNull String, DisjunctionQueue> entry : groupedQueues.entrySet()) {
            Formula completeFE = entry.getValue().getDisjunction(entry.getKey());
            results.add(new VariableWithFeatureEffect(entry.getKey(), completeFE));
            
        }
        
        // Results were ordered before through the map ordering has probably been changed -> reorder elements
        Collections.sort(results, new Comparator<VariableWithFeatureEffect>() {

            @Override
            public int compare(VariableWithFeatureEffect var1, VariableWithFeatureEffect var2) {
                return var1.getVariable().compareTo(var2.getVariable());
            }
            
        });
        
        // Publish results to next component
        for (int i = 0; i < results.size(); i++) {
            addResult(results.get(i));            
        }
        groupedQueues.clear();
    }

    @Override
    public @NonNull String getResultName() {
        return "FEs per Variable";
    }

}
