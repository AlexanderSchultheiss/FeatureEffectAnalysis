package net.ssehub.kernel_haven.fe_analysis.pcs;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.FormulaSimplifier;
import net.ssehub.kernel_haven.fe_analysis.PresenceConditionAnalysisHelper;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

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
        
        private @NonNull String variable;
        
        private @NonNull Set<@NonNull Formula> pcs;

        /**
         * Creates a new {@link VariableWithPcs}.
         * 
         * @param variable The variable name.
         * @param pcs All the PCs that the variable is used in. Must not be <code>null</code>.
         */
        public VariableWithPcs(@NonNull String variable, @NonNull Set<@NonNull Formula> pcs) {
            this.variable = variable;
            this.pcs = pcs;
        }
        
        /**
         * Returns the variable name.
         * 
         * @return The name of the variable.
         */
        @TableElement(name = "Variable", index = 0)
        public @NonNull String getVariable() {
            return variable;
        }
        
        /**
         * Returns a set of all presence conditions that this variable is used in.
         * 
         * @return A set of all PCs, never <code>null</code>.
         */
        @TableElement(name = "Presence conditions", index  = 1)
        public @NonNull Set<@NonNull Formula> getPcs() {
            return pcs;
        }
        
        @Override
        public @NonNull String toString() {
            return "PCs[" + variable + "] = " + pcs.toString();
        }
        
    }
    
    private @NonNull AnalysisComponent<SourceFile> sourceFiles;
    
    private @Nullable AnalysisComponent<BuildModel> bmComponent;
    
    private @NonNull PresenceConditionAnalysisHelper helper;
    private @Nullable FormulaSimplifier simplifier = null;

    /**
     * Creates a {@link PcFinder} for the given code model.
     * 
     * @param config The global configuration.
     * @param sourceFiles The code model provider component.
     * 
     * @throws SetUpException If setting up this component fails.
     */
    public PcFinder(@NonNull Configuration config, @NonNull AnalysisComponent<SourceFile> sourceFiles)
            throws SetUpException {
        
        super(config);
        this.sourceFiles = sourceFiles;
        this.helper = new PresenceConditionAnalysisHelper(config);
        //simplify = helper.getSimplificationMode().ordinal() >= SimplificationType.PRESENCE_CONDITIONS.ordinal();
        if (helper.getSimplificationMode() == SimplificationType.PRESENCE_CONDITIONS) {
            // Will throw an exception if CNF Utils are not present (but was selected by user in configuration file)
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
    public PcFinder(@NonNull Configuration config, @NonNull AnalysisComponent<SourceFile> sourceFiles,
            @NonNull AnalysisComponent<BuildModel> bm) throws SetUpException {
        this(config, sourceFiles);
        this.bmComponent = bm;
    }

    @Override
    protected void execute() {
        BuildModel bm = null;
        if (bmComponent != null) {
            bm = bmComponent.getNextResult();
            if (bm != null) {
                LOGGER.logDebug("Calculating presence conditions including information from build model");
            } else {
                LOGGER.logWarning("Should use build information for calculation of presence conditions, "
                        + "but build model provider returned null", "Ignoring build model");
            }
        } else {
            LOGGER.logDebug("Calculating presence conditions without considering build model");
        }
            
        

        Map<String, Set<@NonNull Formula>> result = new HashMap<>();
        
        SourceFile file;
        while ((file = sourceFiles.getNextResult()) != null) {
            Formula filePc = null;
            if (null != bm) {
                filePc = bm.getPc(file.getPath());
                
                if (filePc != null) {
                    LOGGER.logDebug("File PC for " + file.getPath() + ": " + filePc);
                    // add the file PC as a stand-alone PC
                    addPcToResult(result, filePc);
                    
                } else {
                    LOGGER.logWarning("No file PC for " + file.getPath() + " in build model");
                }
            }
            
            for (CodeElement b : file) {
                // TODO: check if parentIsRelevant should be true if we added the file PC to the result above
                findPcsInElement(b, result, filePc, false);
            }
        }
        
        /*
         * Temporary List for sorting the results (TreeList automatically sorts add insertion)
         * This breaks the pipeline concept, as all results need to be finished before we can sort and send the results.
         * However, at this point the whole analysis is almost finished and its only about (optional) filtering of
         * results and sorting.
         */
        TreeSet<@NonNull VariableWithPcs> tmpResults = new TreeSet<>(new Comparator<VariableWithPcs>() {
            @Override
            public int compare(VariableWithPcs o1, VariableWithPcs o2) {
                return  o1.getVariable().compareTo(o2.getVariable());
            }
        });
        
        for (Map.Entry<String, Set<@NonNull Formula>> entry : result.entrySet()) {
            Set<@NonNull Formula> pcs;
            FormulaSimplifier simplifier = this.simplifier;
            if (simplifier != null) {
                pcs = new HashSet<>();
                for (Formula formula :  notNull(entry.getValue())) {
                    pcs.add(simplifier.simplify(formula));
                }
            } else {
                pcs = notNull(entry.getValue());
            }
            tmpResults.add(new VariableWithPcs(notNull(entry.getKey()), pcs));
        }
                
        for (VariableWithPcs var : tmpResults) {
            addResult(var);            
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
    private void findPcsInElement(@NonNull CodeElement element, @NonNull Map<String, Set<@NonNull Formula>> result,
            @Nullable Formula filePc, boolean parentIsRelevant) {
        
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
    private void addPcToResult(@NonNull Map<String, Set<@NonNull Formula>> result, @NonNull Formula pc) {
        Set<@NonNull Variable> vars = new HashSet<>();
        helper.findVars(pc, vars);
        for (Variable var : vars)  {
            result.putIfAbsent(var.getName(), new HashSet<>());
            result.get(var.getName()).add(pc);
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Presence Conditions";
    }

}
