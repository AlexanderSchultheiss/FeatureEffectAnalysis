package net.ssehub.kernel_haven.fe_analysis.fes;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.util.OrderPreservingParallelizer;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A {@link FeatureEffectFinder} that utilizes multiple threads. This helps with performance if simplification takes
 * long.
 * 
 * @author Adam
 */
public class ThreadedFeatureEffectFinder extends FeatureEffectFinder {

    private static final int NUM_THREADS = 6;
    
    /**
     * Creates a new {@link ThreadedFeatureEffectFinder} for the given PC finder.
     * 
     * @param config The global configuration.
     * @param pcFinder The component to get the PCs from.
     * 
     * @throws SetUpException If creating this component fails.
     */
    public ThreadedFeatureEffectFinder(@NonNull Configuration config,
            @NonNull AnalysisComponent<VariableWithPcs> pcFinder) throws SetUpException {
        super(config, pcFinder);
    }
    
    @Override
    protected void execute() {
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        OrderPreservingParallelizer<VariableWithPcs, VariableWithFeatureEffect> parallelizer
            = new OrderPreservingParallelizer<>(this::processSingle, (result) -> {
                if (result != null) {
                    addResult(result);
                }
                
                progress.processedOne();
                
            }, NUM_THREADS);
        
        VariableWithPcs pcs;
        while ((pcs = pcFinder.getNextResult()) != null) {
            parallelizer.add(pcs);
        }
        
        parallelizer.end();
        parallelizer.join();
        
        progress.close();
    }

}
