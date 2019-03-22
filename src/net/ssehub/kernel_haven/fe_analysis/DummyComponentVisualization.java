package net.ssehub.kernel_haven.fe_analysis;

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
        
        return new JoinComponent(config, archComponentOut, feRels);
    }

}
