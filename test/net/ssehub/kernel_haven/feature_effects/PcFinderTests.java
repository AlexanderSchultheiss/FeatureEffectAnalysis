package net.ssehub.kernel_haven.feature_effects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.feature_effects.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link PcFinder}.
 * @author El-Sharkawy
 *
 */
public class PcFinderTests {

    /**
     * Initializes the logger.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        Logger.init();
    }
    
    
    /**
     * Checks if a single statement with only 1 variable is detected correctly.
     */
    @Test
    public void testRetrievalOfSingleVariable() {
        Variable varA = new Variable("A");
        CodeElement element = new CodeBlock(varA);
        List<VariableWithPcs> results = detectPCs(element);
        
        // Test the expected outcome
        Assert.assertEquals(1,  results.size());
        VariableWithPcs result1 = results.get(0);
        Assert.assertEquals(varA.getName(), result1.getVariable());
        Assert.assertEquals(1, result1.getPcs().size());
        Assert.assertTrue(result1.getPcs().contains(varA));
    }

    /**
     * Runs the {@link PcFinder} on the passed element and returns the result for testing.
     * @param element A mocked element, which should be analyzed by the {@link PcFinder}.
     * @return The detected presence conditions.
     */
    private List<VariableWithPcs> detectPCs(CodeElement element) {
        // Generate configuration
        TestConfiguration tConfig = null;
        Properties config = new Properties();
        try {
            tConfig = new TestConfiguration(config);
        } catch (SetUpException e) {
            Assert.fail("Could not generate test configuration: " + e.getMessage());
        }
        
        // Create virtual files
        File file1 = new File("file1.c");
        SourceFile sourceFile1 = new SourceFile(file1);
        if (element != null) {
            sourceFile1.addElement(element);
        }
        
        List<VariableWithPcs> results = new ArrayList<>();
        try {
            AnalysisComponent<SourceFile> cmComponent = new TestAnalysisComponentProvider<SourceFile>(sourceFile1);
            PcFinder finder = new PcFinder(tConfig, cmComponent);
            finder.execute();
            VariableWithPcs result;
            do {
                result = finder.getNextResult();
                if (null != result) {
                    results.add(result);
                }
            } while (result != null);
        } catch (SetUpException e) {
            Assert.fail("Setting up the " + PcFinder.class.getSimpleName() + " failed: " + e.getMessage());
        }   

        return results;
    }

}
