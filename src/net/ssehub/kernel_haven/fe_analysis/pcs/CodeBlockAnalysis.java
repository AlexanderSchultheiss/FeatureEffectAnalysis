/*
 * Copyright 2021 University of Hildesheim, Software Systems Engineering
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

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.PresenceConditionAnalysisHelper;
import net.ssehub.kernel_haven.fe_analysis.pcs.CodeBlockAnalysis.CodeBlock;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * 
 * Collects (conditional) code blocks of all code files.
 * @author El-Sharkawy
 */
public class CodeBlockAnalysis extends AnalysisComponent<CodeBlock> {
    
    /**
     * An entry that stores a condition of a <b>code block</b> of a code file.
     * The entry consists of:
     * <ul>
     *   <li>The path to the file</li>
     *   <li>The presence condition of the file</li>
     *   <li>The condition of the current block</li>
     *   <li>The presence condition of the current block (considering all parents)</li>
     *   <li>The start line of the block</li>
     *   <li>The end line of the block</li>
     * </ul>
     * 
     * @author El-Sharkawy
     */
    @TableRow
    public static class CodeBlock {
       
        private @NonNull String path;
        private @NonNull Formula fileCondition;
        private @NonNull Formula condition;
        private @NonNull Formula presencecondition;
        private int start;
        private int end;
      

        /**
         * Creates a new {@link VariableWithPcs}.
         * 
         * @param path The path to the analyzed file
         * @param fileCondition The presence condition in order that the file is compiled into the final product
         * @param condition The actual condition of the analyzed code block
         * @param presencecondition The total presence condition of the analyzed code block
         * @param start The line where the block starts within the analyzed file
         * @param end The line where the block ends within the analyzed file
         */
        //checkstyle: stop parameter number check
        public CodeBlock(@NonNull String path, @NonNull Formula fileCondition, @NonNull Formula condition,
            @NonNull Formula presencecondition, int start, int end) {
        //checkstyle: resume parameter number check
           
            this.path = path;
            this.fileCondition = fileCondition;
            this.condition = condition;
            this.presencecondition = presencecondition;
            this.start = start;
            this.end = end;
        }
       
        /**
         * Returns the path to the analyzed file.
         * 
         * @return The path to the analyzed file.
         */
        @TableElement(name = "Path", index = 0)
        public @NonNull String getPath() {
            return path;
        }
        
        /**
         * Returns the presence condition of the analyzed file.
         * 
         * @return The presence condition of the analyzed file.
         */
        @TableElement(name = "File Condition", index = 1)
        public @NonNull Formula getFileCondition() {
            return fileCondition;
        }   
        
        /**
         * Returns the condition of the current block.
         * 
         * @return The presence condition of the current block.
         */
        @TableElement(name = "Block Condition", index = 2)
        public @NonNull Formula getCondition() {
            return condition;
        }   
        
        /**
         * Returns the presence condition of the current block.
         * 
         * @return The presence condition of the current block.
         */
        @TableElement(name = "Presence Condition", index = 3)
        public @NonNull Formula getPresenceCondition() {
            return presencecondition;
        }   
        
        /**
         * Returns the starting line of the block.
         * 
         * @return The starting line of the block.
         */
        @TableElement(name = "start", index = 4)
        public int getStart() {
            return start;
        }
        
        /**
         * Returns the ending line of the block.
         * 
         * @return The ending line of the block.
         */
        @TableElement(name = "end", index = 5)
        public int getEnd() {
            return end;
        }
    }
    
    private @NonNull AnalysisComponent<SourceFile<?>> sourceFiles;
    private @Nullable AnalysisComponent<BuildModel> bmComponent;

    /**
     * Creates a {@link PcFinder} for the given code model.
     * 
     * @param config The global configuration.
     * @param sourceFiles The code model provider component.

     */
    public CodeBlockAnalysis(@NonNull Configuration config, @NonNull AnalysisComponent<SourceFile<?>> sourceFiles) {
        super(config);
        this.sourceFiles = sourceFiles;
    }
    
    /**
     * Creates a {@link PcFinder} for the given code and build model. The build model presence conditions will be
     * added to the code model conditions.
     * 
     * @param config The global configuration.
     * @param sourceFiles The code model provider component.
     * @param bm The build model provider component.
     * 
     */
    public CodeBlockAnalysis(@NonNull Configuration config, @NonNull AnalysisComponent<SourceFile<?>> sourceFiles,
        @NonNull AnalysisComponent<BuildModel> bm) {
        
        this(config, sourceFiles);
        this.bmComponent = bm;
    }

    @Override
    protected void execute() {
        // Init build model
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
        
        ProgressLogger progress = new ProgressLogger(getResultName() + " Collecting");
        
        // Iterate through code files to detect all code blocks
        SourceFile<?> file;
        while ((file = sourceFiles.getNextResult()) != null) {
            Formula filePc = null;
            if (null != bm) {
                filePc = bm.getPc(file.getPath());
            }
            
            // Code block parameters, which are constant for the whole file
            @NonNull String path = NullHelpers.notNull(file.getPath().getPath());
            @NonNull Formula fileCondition = getCondition(filePc);
            
            // Recursively analyze all top level blocks of the file
            for (CodeElement<?> block : file) {
                analyzeBlock(block, path, fileCondition);
                
            }

            progress.processedOne();
        }
        
        // All files processed
        progress.close();
    }
    
    /**
     * Recursive function to analyze a code block and all its nested blocks.
     * @param block The block to analyze, start with top level blocks of a file.
     * @param path The path of the file.
     * @param fileCondition The path to the analyzed file.
     */
    private void analyzeBlock(CodeElement<?> block, @NonNull String path, @NonNull Formula fileCondition) {
        Formula blockCondition = getCondition(block.getCondition());
        Formula pcCondition = computePresenceCondition(block.getPresenceCondition(), fileCondition);
        
        addResult(new CodeBlock(path, fileCondition, blockCondition, pcCondition, block.getLineStart(),
            block.getLineEnd()));
        
        for (CodeElement<?> nested : block) {
            analyzeBlock(nested, path, fileCondition);
        }
    }
    
    /**
     * Computes the presence condition of the current block also considering the file condition. 
     * @param presenceCondition The presence condition, may be <tt>null</tt> if block is always active.
     * @param fileCondition The condition of the file, may be <tt>null</tt> if the file is always present.
     * @return The compound formula, <tt>null</tt> elements are treated as <tt>true</tt>.
     */
    private @NonNull Formula computePresenceCondition(@Nullable Formula presenceCondition,
        @Nullable Formula fileCondition) {
        
        @NonNull Formula result;
        if (null != presenceCondition) {
            result = null != fileCondition ? new Conjunction(fileCondition, presenceCondition) : presenceCondition;
        } else {
            // No presence condition, check if there is a file condition
            result = null != fileCondition ? fileCondition : True.INSTANCE;
        }
        
        return result;
    }
    
    /**
     * Helper function to treat <tt>null</tt> elements as <tt>true</tt>.
     * @param condition A condition to handle, which is maybe <tt>null</tt>.
     * @return The condition if it is not <tt>null</tt>, <tt>true</tt> otherwise.
     */
    private @NonNull Formula getCondition(@Nullable Formula condition) {
        return null != condition ? condition : True.INSTANCE;
    }

    @Override
    public @NonNull String getResultName() {
        return "Conditional Code Blocks";
    }
}
