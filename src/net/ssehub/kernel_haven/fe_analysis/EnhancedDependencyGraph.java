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
package net.ssehub.kernel_haven.fe_analysis;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.JoinComponent;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.analysis.SplitComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureRelations;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.fe_analysis.relations.PotentialParentFinder;
import net.ssehub.kernel_haven.fe_analysis.relations.PotentialParentRelationFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Analysis component that collects (in separate, parallel) analysis components the following information.
 * @author El-Sharkawy
 *
 */
public class EnhancedDependencyGraph extends PipelineAnalysis {

    /**
     * Creates the complete analysis pipeline.
     * @param config The pipeline configuration
     */
    public EnhancedDependencyGraph(@NonNull Configuration config) {
        super(config);
    }
    
    @Override
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
        // Common input
        PcFinder finder =  new PcFinder(config, getCmComponent(), getBmComponent());
        SplitComponent<VariableWithPcs> pcSplit = new SplitComponent<>(config, finder);

        // "Final" Analysis components of the analysis branches
        FeatureRelations relationsComponent = new FeatureRelations(config,
            new FeatureEffectFinder(config, pcSplit.createOutputComponent()));
        PotentialParentRelationFinder parentComponent = new PotentialParentRelationFinder(config,
            new PotentialParentFinder(config, pcSplit.createOutputComponent()));
        
        // Automatically prints results of PotentialParentFinder & FeatureRelations
        // Other results must be printed via the analysis.output.intermediate_results config
        return new JoinComponent(config, relationsComponent, parentComponent);
    }
}
