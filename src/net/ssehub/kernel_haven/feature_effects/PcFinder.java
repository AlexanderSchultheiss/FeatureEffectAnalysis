package net.ssehub.kernel_haven.feature_effects;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.PipelineConfigurator;
import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.BlockingQueue;
import net.ssehub.kernel_haven.util.CodeExtractorException;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Creates a mapping variable -> presence conditions.
 * 
 * @author Adam
 */
public class PcFinder extends AbstractPresenceConditionAnalysis {
    
    private BuildModel bm;

    /**
     * Creates a new PcFinder.
     * 
     * @param config The configuration to use.
     * @throws SetUpException if a build model was specified, but exited abnormally
     */
    public PcFinder(Configuration config) throws SetUpException {
        super(config);
        
        bm = PipelineConfigurator.instance().getBmProvider().getResult();
        if (null == bm) {
            ExtractorException exc = null;
            StringBuffer errMsg = new StringBuffer();
            while ((exc = PipelineConfigurator.instance().getBmProvider().getNextException()) != null) {
                if (errMsg.length() > 0) {
                    errMsg.append(", ");                    
                }
                errMsg.append(exc.getMessage());
            }
            if (errMsg.length() == 0) {
                Logger.get().logDebug("Calculating presence conditions without considering build model");
            } else {
                throw new SetUpException("Should use build information for calculation of presence conditions, "
                    + "but build model provider returned an error: " + errMsg.toString(), exc);
            }
        } else {
            Logger.get().logDebug("Calculating presence conditions including information from build model");
        }
    }
    
    /**
     * Goes through all presence conditions in all source files and creates a mapping
     * variable -> all PCs that the variable appears in.
     * 
     * @param files The source files to go through.
     * @return Every PC a variable is contained in; for each variable.
     */
    public Map<String, Set<Formula>> findPcs(BlockingQueue<SourceFile> files) {
        Map<String, Set<Formula>> result = new HashMap<>();
        
        for (SourceFile file : files) {
            Formula filePc = null;
            if (null != bm) {
                filePc = bm.getPc(file.getPath());
                Logger.get().logDebug("Presence condition for " + file.getPath() + ": " + filePc);
            }
            
            for (CodeElement b : file) {
                findPcsInBlock(b, result, filePc, false);
            }
        }
        
        return result;
    }
    
    /**
     * Finds all PCs in a block and recursively in all child blocks. Adds the PC to the set for
     * all variables that are found in the PC.
     * 
     * @param block The block to find PCs in.
     * @param result The result to add the PCs to.
     * @param filePc Optional: The presence condition of the file which is currently processed. Will be ignored if it is
     * <tt>null</tt>.
     * @param parentIsRelevant Used for optimization (<tt>true</tt> parent condition is relevant and, thus, also all
     * nested conditions are relevant, <tt>false</tt> this method will check if the condition should be considered).
     */
    private void findPcsInBlock(CodeElement block, Map<String, Set<Formula>> result, Formula filePc,
        boolean parentIsRelevant) {
        
        Set<Variable> vars = new HashSet<>();
        Formula pc = block.getPresenceCondition();
        
        if (parentIsRelevant || isRelevant(pc)) {
            // Skip retrieval of variables for nested conditions (last for loop)
            parentIsRelevant = true;
            if (null != filePc) {
                pc = new Conjunction(filePc, pc);
            }
            
            findVars(pc, vars);
            
            for (Variable var : vars)  {
                result.putIfAbsent(var.getName(), new HashSet<>());
                result.get(var.getName()).add(pc);
            }
        }
        
        for (CodeElement child : block.iterateNestedElements()) {
            findPcsInBlock(child, result, filePc, parentIsRelevant);
        }
    }
    
    @Override
    public void run() {
        try {
            cmProvider.start();
            
            Map<String, Set<Formula>> result = findPcs(cmProvider.getResultQueue());
            
            PrintStream out = createResultStream("variable_pcs.csv");
            for (Map.Entry<String, Set<Formula>> entry : result.entrySet()) {
                out.print(entry.getKey());
                for (Formula f : entry.getValue()) {
                    out.print(";");
                    out.print(f.toString());
                }
                out.println();
            }
            out.close();
            
            out = createResultStream("unparsable_files.txt");
            CodeExtractorException exc;
            while ((exc = (CodeExtractorException) cmProvider.getNextException()) != null) {
                out.println(exc.getCausingFile().getPath() + ": " + exc.getCause());
            }
            out.close();
            
        } catch (SetUpException e) {
            LOGGER.logException("Error while starting cm provider", e);
        }
    }

}
