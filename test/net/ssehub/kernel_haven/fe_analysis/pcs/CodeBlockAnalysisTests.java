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

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.not;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.fe_analysis.AbstractFinderTests;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * 
 * Tests the {@link CodeBlockAnalysis}.
 * @author El-Sharkawy
 */
public class CodeBlockAnalysisTests extends 
    AbstractFinderTests<net.ssehub.kernel_haven.fe_analysis.pcs.CodeBlockAnalysis.CodeBlock> {
    
    private BuildModel bm;
    private boolean missingBuildAsFalse;
    
    /**
     * Tests handling of files without any code blocks.
     */
    @Test
    public void testEmptyFile() {
        // Input files to analyze
        File fakeFile = new File("src/File1.c");
        CodeBlock element = new CodeBlock(1, 42, fakeFile, True.INSTANCE, True.INSTANCE);
        
        // Run the analysis
        List<net.ssehub.kernel_haven.fe_analysis.pcs.CodeBlockAnalysis.CodeBlock> results
            = runAnalysis(element, SimplificationType.PRESENCE_CONDITIONS);
        
        // Verify correct analysis results
        Assert.assertEquals(1, results.size());
        // No build model passed -> BM = True, Block = TRUE
        assertBlock(results.get(0), element, True.INSTANCE, True.INSTANCE);
    }
    
    /**
     * Tests handling of files with code block, but no build model, no nesting of blocks.
     */
    @Test
    public void testSimpleBlocks() {
        // Input files to analyze
        Variable varA = new Variable("A");
        File fakeFile = new File("src/File1.c");
        CodeBlock element = new CodeBlock(1, 100, fakeFile, True.INSTANCE, True.INSTANCE);
        CodeBlock conditionalBlockIf = new CodeBlock(1, 50, fakeFile, varA, varA);
        CodeBlock conditionalBlockElse = new CodeBlock(51, 100, fakeFile, not(varA), not(varA));
        element.addNestedElement(conditionalBlockIf);
        element.addNestedElement(conditionalBlockElse);
        
        // Run the analysis
        List<net.ssehub.kernel_haven.fe_analysis.pcs.CodeBlockAnalysis.CodeBlock> results
            = runAnalysis(element, SimplificationType.PRESENCE_CONDITIONS);
        
        // Verify correct analysis results
        Assert.assertEquals(3, results.size());
        // No build model passed -> BM = True
        assertBlock(results.get(0), element, True.INSTANCE, True.INSTANCE);
        assertBlock(results.get(1), conditionalBlockIf, True.INSTANCE, varA);
        assertBlock(results.get(2), conditionalBlockElse, True.INSTANCE, not(varA));
    }
        
    /**
     * Tests one {@link net.ssehub.kernel_haven.fe_analysis.pcs.CodeBlockAnalysis.CodeBlock} based on the given input
     * block.
     * @param block The block to test.
     * @param expectedBlock The block containing the expected condition, line start, and end.
     * @param expectedFilePc The expected presence condition of the file
     * @param expectedPresenceCondition The expected effective condition of the tested block.
     */
    private void assertBlock(net.ssehub.kernel_haven.fe_analysis.pcs.CodeBlockAnalysis.CodeBlock block,
        CodeBlock expectedBlock, Formula expectedFilePc, Formula expectedPresenceCondition) {
        
        assertBlock(block, expectedBlock.getSourceFile().getPath(), expectedFilePc, expectedBlock.getCondition(),
            expectedPresenceCondition, expectedBlock.getLineStart(), expectedBlock.getLineEnd());
    }
    
    /**
     * Tests one {@link net.ssehub.kernel_haven.fe_analysis.pcs.CodeBlockAnalysis.CodeBlock}.
     * @param block The block to test.
     * @param expectedPath The expected path
     * @param expectedFilePc The expected presence condition of the file
     * @param expectedCondition The expected condition of the tested block
     * @param expectedPresenceCondition The expected effective condition of the tested block.
     * @param expectedStart The expected start line of the block
     * @param expectedEnd The expected end line of the block.
     */
    //checkstyle: stop parameter number check
    private void assertBlock(net.ssehub.kernel_haven.fe_analysis.pcs.CodeBlockAnalysis.CodeBlock block,
        String expectedPath, Formula expectedFilePc, Formula expectedCondition, Formula expectedPresenceCondition,
        int expectedStart, int expectedEnd) {
    //checkstyle: resume parameter number check
        
        Assert.assertEquals(expectedPath, block.getPath());
        Assert.assertEquals(expectedFilePc, block.getFileCondition());
        Assert.assertEquals(expectedCondition, block.getCondition());
        Assert.assertEquals(expectedPresenceCondition, block.getPresenceCondition());
        Assert.assertEquals(expectedStart, block.getStart());
        Assert.assertEquals(expectedEnd, block.getEnd());
    }

    @Override
    protected AnalysisComponent<net.ssehub.kernel_haven.fe_analysis.pcs.CodeBlockAnalysis.CodeBlock>
        createAnalysor(TestConfiguration tConfig, @NonNull AnalysisComponent<SourceFile<?>> cmComponent)
        throws SetUpException {
        
        // Always sort during tests
        tConfig.registerSetting(CodeBlockAnalysis.ORDER_RESULTS);
        tConfig.setValue(CodeBlockAnalysis.ORDER_RESULTS, Boolean.TRUE);
        
        // Switch to en-/disable treatment of missing build information
        tConfig.registerSetting(CodeBlockAnalysis.MISSING_BUILD_INFORMATION_AS_FALSE);
        tConfig.setValue(CodeBlockAnalysis.MISSING_BUILD_INFORMATION_AS_FALSE, missingBuildAsFalse);
        
        CodeBlockAnalysis analysis;
        if (bm == null) {
            analysis = new CodeBlockAnalysis(tConfig, cmComponent);
        } else {
            TestAnalysisComponentProvider<BuildModel> bmProvider = new TestAnalysisComponentProvider<BuildModel>(bm);
            analysis = new CodeBlockAnalysis(tConfig, cmComponent, bmProvider);
        }
        
        return analysis;
    }

}
