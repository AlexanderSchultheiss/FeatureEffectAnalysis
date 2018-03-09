package net.ssehub.kernel_haven.fe_analysis.fes;

import java.util.LinkedList;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * A {@link FeatureEffectFinder} that utilizes multiple threads. This helps with performance if simplification takes
 * long.
 * 
 * @author Adam
 */
public class ThreadedFeatureEffectFinder extends FeatureEffectFinder {

    private static final int NUM_THREADS = 6;
    
    /**
     * A single work package, with the index where it should be added in the result list. 
     */
    private class WorkPackage {
        
        private int index;
        
        private @NonNull VariableWithPcs input;
        
        private @Nullable VariableWithFeatureEffect output;
        
        /**
         * Creates a {@link WorkPackage}.
         * 
         * @param index The index where this belongs in the output package.
         * @param input The input for the calculation.
         */
        public WorkPackage(int index, @NonNull VariableWithPcs input) {
            this.index = index;
            this.input = input;
        }
        
        /**
         * Executes this work package.
         */
        public void execute() {
            this.output = processSingle(this.input);
        }
        
        /**
         * Returns the output of the execution.
         * 
         * @return The result of the execution.
         */
        public @Nullable VariableWithFeatureEffect getOutput() {
            return output;
        }
        
        /**
         * Returns the index where the result of this package belongs in the output list.
         * 
         * @return The index where this result belongs.
         */
        public int getIndex() {
            return index;
        }
        
    }
    
    private @NonNull BlockingQueue<WorkPackage> todo;
    
    private @NonNull BlockingQueue<WorkPackage> done;
    
    private int numWorkersDone = 0;
    
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
        
        todo = new BlockingQueue<>();
        done = new BlockingQueue<>();
    }
    
    @Override
    protected void execute() {
        
        // spawn worker threads
        for (int i = 0; i < NUM_THREADS; i++) {
            new Thread(() -> {
                
                WorkPackage wp;
                while ((wp = todo.get()) != null) {
                    wp.execute();
                    done.add(wp);
                }
                
                synchronized (ThreadedFeatureEffectFinder.this) {
                    numWorkersDone++;
                    if (numWorkersDone == NUM_THREADS) {
                        done.end();
                    }
                }
                
            }, "ThreadedFeatureEffectFinder-Worker-" + (i + 1)).start();
        }
        
        // spawn collector thread
        Thread collector = new Thread(() -> {
            
            List<@NonNull WorkPackage> received = new LinkedList<>();
            int nextWantedIndex = 0;
            
            WorkPackage wp;
            while ((wp = done.get()) != null) {
                received.add(wp);
                
                boolean found;
                do {
                    found = false;
                    for (WorkPackage possibleNext : received) {
                        if (possibleNext.getIndex() == nextWantedIndex) {
                            // we have found the next result that we can send 
                            found = true;
                            VariableWithFeatureEffect result = possibleNext.getOutput();
                            if (result != null) {
                                addResult(result);
                            }
                            nextWantedIndex++;
                        }
                    }
                    
                } while (found);
                
            }
            
        }, "ThreadedFeatureEffectFinder-Collector");
        collector.start();
        
        VariableWithPcs pcs;
        int workPackageIndex = 0;
        while ((pcs = pcFinder.getNextResult()) != null) {
            todo.add(new WorkPackage(workPackageIndex, pcs));
            workPackageIndex++;
        }
        
        todo.end();
        
        try {
            collector.join();
        } catch (InterruptedException e) {
            LOGGER.logException("Unable to wait for collector thread", e);
        }
    }

}
