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
