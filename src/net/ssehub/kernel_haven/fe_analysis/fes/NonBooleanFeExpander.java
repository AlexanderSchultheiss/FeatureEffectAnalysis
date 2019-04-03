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

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.PresenceConditionAnalysisHelper;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.FormulaSimplifier;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Expands {@link VariableWithFeatureEffect}s for non-boolean variables with their "has any value" feature effect.
 * For example, consider the two {@link VariableWithFeatureEffect} "A has FE B" and "A=1 has FE C". This component
 * combines the two so that the result is "A has FE B" and "A=1 has FE C or B".
 * 
 * If non-boolean preparation is not enabled, this component does nothing.
 * 
 * @author Adam
 */
public class NonBooleanFeExpander extends AnalysisComponent<VariableWithFeatureEffect> {


    protected @NonNull AnalysisComponent<VariableWithFeatureEffect> feFinder;
    
    private FeatureEffectStorage storage;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * @param feFinder The component to get {@link VariableWithFeatureEffect}s from.
     * 
     * @throws SetUpException If detecting non-boolean mode fails.
     */
    public NonBooleanFeExpander(@NonNull Configuration config,
            @NonNull AnalysisComponent<VariableWithFeatureEffect> feFinder) throws SetUpException {
        super(config);
        
        this.feFinder = feFinder;
        
        PresenceConditionAnalysisHelper helper = new PresenceConditionAnalysisHelper(config);
        
        if (helper.isNonBooleanMode()) {
            storage = new FeatureEffectStorage();
        }
    }

    @Override
    protected void execute() {
        VariableWithFeatureEffect fe;
        while ((fe = feFinder.getNextResult()) != null) {
            VariableWithFeatureEffect result = fe;
            
            if (storage != null) {
                VariableWithFeatureEffect baseVar = storage.getBaseVariable(fe.getVariable());
                
                if (baseVar != null) {
                    result = new VariableWithFeatureEffect(fe.getVariable(), FormulaSimplifier.simplify(
                            new Disjunction(baseVar.getFeatureEffect(), fe.getFeatureEffect())));
                } else {
                    // if we could find a baseVar, there is no need to add to the storage anymore
                    storage.add(fe);
                }
            }
            
            addResult(result);
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Feature Effects" + (storage != null ? " (non-boolean expanded)" : "");
    }

}
