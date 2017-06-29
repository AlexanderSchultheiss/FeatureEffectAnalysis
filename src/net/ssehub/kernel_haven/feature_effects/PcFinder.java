package net.ssehub.kernel_haven.feature_effects;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AbstractAnalysis;
import net.ssehub.kernel_haven.code_model.Block;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.CodeExtractorException;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Creates a mapping variable -> presence conditions.
 * 
 * @author Adam
 */
public class PcFinder extends AbstractAnalysis {

    public PcFinder(Configuration config) {
        super(config);
    }
    
    /**
     * Goes through all presence conditions in all source files and creates a mapping
     * variable -> all PCs that the variable appears in.
     * 
     * @param files The source files to go through.
     * @return Every PC a variable is contained in; for each variable.
     */
    public Map<String, Set<Formula>> findPcs(List<SourceFile> files) {
        Map<String, Set<Formula>> result = new HashMap<>();
        
        for (SourceFile file : files) {
            for (Block b : file) {
                findPcsInBlock(b, result);
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
     */
    private void findPcsInBlock(Block block, Map<String, Set<Formula>> result) {
        
        Set<Variable> vars = new HashSet<>();
        findVars(block.getPresenceCondition(), vars);
        
        for (Variable var : vars)  {
            result.putIfAbsent(var.getName(), new HashSet<>());
            result.get(var.getName()).add(block.getPresenceCondition());
        }
        
        for (Block child : block) {
            findPcsInBlock(child, result);
        }
    }
    
    /**
     * Finds all variables in the given formula. This recursively walks through the whole tree.
     * 
     * @param f The formula to find variables in.
     * @param result The resulting set to add variables to.
     */
    private void findVars(Formula f, Set<Variable> result) {
        
        if (f instanceof Variable) {
            result.add((Variable) f);
            
        } else if (f instanceof Negation) {
            findVars(((Negation) f).getFormula(), result);
            
        } else if (f instanceof Disjunction) {
            Disjunction dis = (Disjunction) f;
            findVars(dis.getLeft(), result);
            findVars(dis.getRight(), result);
            
        } else if (f instanceof Conjunction) {
            Conjunction con = (Conjunction) f;
            findVars(con.getLeft(), result);
            findVars(con.getRight(), result);
            
        } else {
            // ignore true and false
        }
        
    }

    @Override
    public void run() {
        try {
            cmProvider.start(config.getCodeConfiguration());
            
            List<SourceFile> files = new ArrayList<>(1000);
            
            SourceFile file;
            while ((file = cmProvider.getNext()) != null) {
                files.add(file);
            }
            
            Map<String, Set<Formula>> result = findPcs(files);
            
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
            while ((exc = cmProvider.getNextException()) != null) {
                out.println(exc.getCausingFile().getPath() + ": " + exc.getCause());
            }
            out.close();
            
        } catch (SetUpException e) {
            LOGGER.logException("Error while starting cm provider", e);
        } catch (ExtractorException e) {
            LOGGER.logException("Error running extractor", e);
        }
    }

}
