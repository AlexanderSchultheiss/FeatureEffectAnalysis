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

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.JoinComponent;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.arch_components.ArchComponentWriter;
import net.ssehub.kernel_haven.fe_analysis.arch_components.DummyArchComponentStorageCreator;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureRelations;
import net.ssehub.kernel_haven.fe_analysis.fes.ThreadedFeatureEffectFinder;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A analysis for visualization using architecture components. This uses {@link DummyArchComponentStorageCreator} to
 * create the component mapping; this is useful for example for Linux.
 * 
 * @author Adam
 */
public class DummyComponentVisualization extends PipelineAnalysis {

    /**
     * Creates this pipeline.
     * 
     * @param config The pipeline configuration.
     */
    public DummyComponentVisualization(@NonNull Configuration config) {
        super(config);
    }

    @Override
    protected @NonNull AnalysisComponent<?> createPipeline() throws SetUpException {
        
        DummyArchComponentStorageCreator archComponents
                = new DummyArchComponentStorageCreator(config, getVmComponent());
        ArchComponentWriter archComponentOut = new ArchComponentWriter(config, archComponents);
        
        PcFinder pcs = new PcFinder(config, getCmComponent(), getBmComponent());
        AnalysisComponent<VariableWithFeatureEffect> fes = new ThreadedFeatureEffectFinder(config, pcs);
        FeatureRelations feRels = new FeatureRelations(config, fes);
        
        AnalysisComponent<?> pssm;
        // get ProblemSolutionSpaceMapper via reflection, so this plugin doesn't depend on it
        try {
            Class<?> pssmClass = ClassLoader.getSystemClassLoader().loadClass(
                    "net.ssehub.kernel_haven.pss_mapper.ProblemSolutionSpaceMapper");
            
            pssm = (AnalysisComponent<?>) pssmClass.getConstructor(
                    Configuration.class, AnalysisComponent.class, AnalysisComponent.class, AnalysisComponent.class)
                    .newInstance(config, getCmComponent(), getBmComponent(), getVmComponent());
            pssm = notNull(pssm); // newInstance never returns null
            
        } catch (ReflectiveOperationException | ClassCastException | SecurityException e) {
            throw new SetUpException("Can't instantiate PSS-Mapper via reflection", e);
        }
        
        return new JoinComponent(config, archComponentOut, feRels, pssm);
    }

}
