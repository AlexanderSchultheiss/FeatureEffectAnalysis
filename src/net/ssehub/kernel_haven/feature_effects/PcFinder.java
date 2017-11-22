package net.ssehub.kernel_haven.feature_effects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.feature_effects.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.feature_effects.PresenceConditionAnalysisHelper.SimplificationType;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * A component that creates a mapping variable -> set of all PCs the variable is used in.
 * 
 * @author Adam
 */
public class PcFinder extends AnalysisComponent<VariableWithPcs> {
    
    /**
     * A variable together with all presence conditions it is used in.
     * 
     * @author Adam
     */
    @TableRow
    public static class VariableWithPcs {
        
        private String variable;
        
        private Set<Formula> pcs;

        /**
         * Creates a new {@link VariableWithPcs}.
         * 
         * @param variable The variable name.
         * @param pcs All the PCs that the variable is used in. Must not be <code>null</code>.
         */
        public VariableWithPcs(String variable, Set<Formula> pcs) {
            this.variable = variable;
            this.pcs = pcs;
        }
        
        /**
         * Returns the variable name.
         * 
         * @return The name of the variable.
         */
        @TableElement(name = "Variable", index = 0)
        public String getVariable() {
            return variable;
        }
        
        /**
         * Returns a set of all presence conditions that this variable is used in.
         * 
         * @return A set of all PCs, never <code>null</code>.
         */
        @TableElement(name = "Presence conditions", index  = 1)
        public Set<Formula> getPcs() {
            return pcs;
        }
        
        @Override
        public String toString() {
            return "PCs[" + variable + "] = " + pcs.toString();
        }
        
    }
    
    private AnalysisComponent<SourceFile> sourceFiles;
    
    private AnalysisComponent<BuildModel> bmComponent;
    
    private PresenceConditionAnalysisHelper helper;
    private boolean simplify;
    private FormulaSimplifier simplifier = null;

    /**
     * Creates a {@link PcFinder} for the given code model.
     * 
     * @param config The global configuration.
     * @param sourceFiles The code model provider component.
     * 
     * @throws SetUpException If setting up this component fails.
     */
    public PcFinder(Configuration config, AnalysisComponent<SourceFile> sourceFiles) throws SetUpException {
        super(config);
        this.sourceFiles = sourceFiles;
        this.helper = new PresenceConditionAnalysisHelper(config);
        simplify = helper.getSimplificationMode().ordinal() >= SimplificationType.PRESENCE_CONDITIONS.ordinal();
        if (simplify) {
            simplifier = new FormulaSimplifier();
        }
    }
    
    /**
     * Creates a {@link PcFinder} for the given code and build model. The build model presence conditions will be
     * added to the code model conditions.
     * 
     * @param config The global configuration.
     * @param sourceFiles The code model provider component.
     * @param bm The build model provider component.
     * 
     * @throws SetUpException If setting up this component fails.
     */
    public PcFinder(Configuration config, AnalysisComponent<SourceFile> sourceFiles, AnalysisComponent<BuildModel> bm)
            throws SetUpException {
        this(config, sourceFiles);
        this.bmComponent = bm;
    }

    @Override
    protected void execute() {
        BuildModel bm = null;
        if (bmComponent != null) {
            bm = bmComponent.getNextResult();
            if (bm != null) {
                Logger.get().logDebug("Calculating presence conditions including information from build model");
            } else {
                Logger.get().logWarning("Should use build information for calculation of presence conditions, "
                        + "but build model provider returned null", "Ignoring build model");
            }
        } else {
            Logger.get().logDebug("Calculating presence conditions without considering build model");
        }
            
        

        Map<String, Set<Formula>> result = new HashMap<>();
        
        SourceFile file;
        while ((file = sourceFiles.getNextResult()) != null) {
            Formula filePc = null;
            if (null != bm) {
                filePc = bm.getPc(file.getPath());
                
                if (filePc != null) {
                    Logger.get().logDebug("File PC for " + file.getPath() + ": " + filePc);
                    // add the file PC as a stand-alone PC
                    addPcToResult(result, filePc);
                    
                } else {
                    Logger.get().logWarning("No file PC for " + file.getPath() + " in build model");
                }
            }
            
            for (CodeElement b : file) {
                // TODO: check if parentIsRelevant should be true if we added the file PC to the result above
                findPcsInElement(b, result, filePc, false);
            }
        }
        
        for (Map.Entry<String, Set<Formula>> entry : result.entrySet()) {
            Set<Formula> pcs;
            if (simplify) {
                pcs = new HashSet<>();
                for (Formula formula :  entry.getValue()) {
                    pcs.add(simplifier.simplify(formula));
                }
            } else {
                pcs = entry.getValue();
            }
            addResult(new VariableWithPcs(entry.getKey(), pcs));
        }
    }
    
    /**
     * Finds all PCs in an element and recursively in all child elements. Adds the PC to the set for
     * all variables that are found in the PC.
     * 
     * @param element The element to find PCs in.
     * @param result The result to add the PCs to.
     * @param filePc Optional: The presence condition of the file which is currently processed. Will be ignored if it is
     * <tt>null</tt>.
     * @param parentIsRelevant Used for optimization (<tt>true</tt> parent condition is relevant and, thus, also all
     * nested conditions are relevant, <tt>false</tt> this method will check if the condition should be considered).
     */
    private void findPcsInElement(CodeElement element, Map<String, Set<Formula>> result, Formula filePc,
        boolean parentIsRelevant) {
        
        Formula pc = element.getPresenceCondition();
        
        if (parentIsRelevant || helper.isRelevant(pc)) {
            // Skip retrieval of variables for nested conditions (last for loop)
            parentIsRelevant = true;
            if (null != filePc) {
                pc = new Conjunction(filePc, pc);
            }
            
            addPcToResult(result, pc);
        }
        
        for (CodeElement child : element.iterateNestedElements()) {
            findPcsInElement(child, result, filePc, parentIsRelevant);
        }
    }
    
    /**
     * Adds a presence condition to the result.
     * 
     * @param result The result map to add to.
     * @param pc The presence condition that was found.
     */
    private void addPcToResult(Map<String, Set<Formula>> result, Formula pc) {
        Set<Variable> vars = new HashSet<>();
        helper.findVars(pc, vars);
        for (Variable var : vars)  {
            result.putIfAbsent(var.getName(), new HashSet<>());
            result.get(var.getName()).add(pc);
        }
    }

    @Override
    public String getResultName() {
        return "Presence Conditions";
    }

}
