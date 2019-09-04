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
package net.ssehub.kernel_haven.fe_analysis.pcs;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.fe_analysis.PresenceConditionAnalysisHelper;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.util.PerformanceProbe;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.FormulaSimplifier;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * A component that creates a mapping {@code variable -> set of all PCs the variable is used in}.
 * 
 * @author Adam
 */
public class PcFinder extends AnalysisComponent<VariableWithPcs> {
    
    public static final @NonNull Setting<@NonNull Boolean> CONSIDER_ALL_BM = new Setting<>(
            "analysis.pc_finder.add_all_bm_pcs", Type.BOOLEAN, true, "false", "Whether the " + PcFinder.class.getName()
            + " should consider all presence conditions from the build model. If true, then all PCs from the build"
            + " model will be considered, even if no real file for it exists.");
    
    public static final @NonNull Setting<@NonNull Boolean> COMBINE_NON_BOOLEAN = new Setting<>(
            "analysis.pc_finder.combine_non_boolean", Type.BOOLEAN, true, "false", "Whether the "
            + PcFinder.class.getSimpleName() + " should collapse all non-boolean replacements to a single variable ("
            + "i.e. VAR and VAR_eq_1 will be treated as the same variable VAR)");
    
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
    
    private @NonNull AnalysisComponent<SourceFile<?>> sourceFiles;
    
    private @Nullable AnalysisComponent<BuildModel> bmComponent;
    
    private @NonNull PresenceConditionAnalysisHelper helper;
    
    private boolean addAllBmPcs;
    
    private boolean combineNonBoolean;

    /**
     * Creates a {@link PcFinder} for the given code model.
     * 
     * @param config The global configuration.
     * @param sourceFiles The code model provider component.
     * 
     * @throws SetUpException If setting up this component fails.
     */
    public PcFinder(@NonNull Configuration config, @NonNull AnalysisComponent<SourceFile<?>> sourceFiles)
            throws SetUpException {
        
        super(config);
        this.sourceFiles = sourceFiles;
        this.helper = new PresenceConditionAnalysisHelper(config);
        
        config.registerSetting(CONSIDER_ALL_BM);
        addAllBmPcs = config.getValue(CONSIDER_ALL_BM);
        
        config.registerSetting(COMBINE_NON_BOOLEAN);
        combineNonBoolean = config.getValue(COMBINE_NON_BOOLEAN);
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
    public PcFinder(@NonNull Configuration config, @NonNull AnalysisComponent<SourceFile<?>> sourceFiles,
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
        
        ProgressLogger progress = new ProgressLogger(getClass().getSimpleName() + " Collecting");
        
        SourceFile<?> file;
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
            
            for (CodeElement<?> b : file) {
                // TODO: check if parentIsRelevant should be true if we added the file PC to the result above
                findPcsInElement(b, result, filePc, false);
            }
            
            progress.processedOne();
        }
        
        // consider all presence conditions from the build model, if configured
        if (null != bm && addAllBmPcs) {
            findPcsInBuildModel(bm, result);
        }
        
        progress.close();
        
        @NonNull VariableWithPcs[] list = sortResults(result);
        
        for (VariableWithPcs var : list) {
            addResult(var);
        }
    }

    /**
     * Turns the map of collected PCs into a sorted array of {@link VariableWithPcs}s. The results are sorted by
     * variable name. If enabled in the config, this also simplifies the presence conditions.
     * 
     * @param pcMap The map of collected presence conditions.
     * 
     * @return A sorted array of {@link VariableWithPcs}s created from the map.
     */
    @SuppressWarnings("null") // stream API and null annotations don't work so nicely together :-/
    private @NonNull VariableWithPcs @NonNull [] sortResults(Map<String, Set<@NonNull Formula>> pcMap) {
        boolean simplify = helper.getSimplificationMode() == SimplificationType.PRESENCE_CONDITIONS;
        
        ProgressLogger progress = new ProgressLogger(getClass().getSimpleName() + " Sorting"
                + (simplify ? " and simplifying" : ""), pcMap.size());
        
        LOGGER.logInfo("Sorting " + (simplify ? "and simplifying " : "") + "PCs; this may take a long time");
        
        PerformanceProbe p = new PerformanceProbe("PcFinder simplification");
        
        @NonNull VariableWithPcs[] result = new @NonNull VariableWithPcs[pcMap.size()];
        int i = 0;
        for (Map.Entry<String, Set<@NonNull Formula>> entry : pcMap.entrySet()) {
            Set<@NonNull Formula> pcs = notNull(entry.getValue());
            
            if (simplify) {
                // Stream-based simplification of formulas and re-creation of set in multiple threads.
                pcs = notNull(pcs.parallelStream().map(FormulaSimplifier::simplify).collect(Collectors.toSet()));
            }
            
            result[i++] = new VariableWithPcs(notNull(entry.getKey()), pcs);
            
            progress.processedOne();
        }
        
        p.close();
        
        Arrays.sort(result, (o1, o2) -> o1.getVariable().compareTo(o2.getVariable()));
        
        progress.close();
        
        return result;
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
    private void findPcsInElement(@NonNull CodeElement<?> element, @NonNull Map<String, Set<@NonNull Formula>> result,
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
        
        for (CodeElement<?> child : element) {
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
        if (combineNonBoolean) {
            pc = helper.removeReplacements(pc);
        }
        
        Set<@NonNull Variable> vars = new HashSet<>();
        helper.findVars(pc, vars);
        for (Variable var : vars)  {
            result.putIfAbsent(var.getName(), new HashSet<>());
            result.get(var.getName()).add(pc);
        }
    }
    
    /**
     * Adds all PCs found in the build model to the result set.
     * 
     * @param bm The build model to walk through.
     * @param result The result set to add PCs to.
     */
    private void findPcsInBuildModel(@NonNull BuildModel bm, @NonNull Map<String, Set<@NonNull Formula>> result) {
        for (File f : bm) {
            Formula pc = bm.getPc(f);
            if (pc != null) {
                addPcToResult(result, pc);
            }
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Presence Conditions";
    }

}
