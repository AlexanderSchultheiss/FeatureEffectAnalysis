package net.ssehub.kernel_haven.fe_analysis.fes;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * An analysis that finds feature effect formulas for variables.
 * 
 * @author Adam
 */
public class FeatureEffectAnalysis extends PipelineAnalysis {

    /**
     * Creates a new {@link FeatureEffectAnalysis}.
     * 
     * @param config The global configuration.
     */
    public FeatureEffectAnalysis(@NonNull Configuration config) {
        super(config);
    }

    @Override
    protected AnalysisComponent<?> createPipeline() throws SetUpException {
        return new FeatureEffectFinder(config,
                new PcFinder(config,
                        getCmComponent(),
                        getBmComponent()
                )
        );
    }

}
