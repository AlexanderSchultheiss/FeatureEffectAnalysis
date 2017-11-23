package net.ssehub.kernel_haven.feature_effects;

import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.feature_effects.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.feature_effects.Settings.SimplificationType;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link PcFinder}.
 * @author El-Sharkawy
 *
 */
public class PcFinderTests extends AbstractFinderTests<VariableWithPcs> {
    
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
    public void testRetrievalOfSingleStatement() {
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
        return super.runAnalysis(element, SimplificationType.NO_SIMPLIFICATION);
    }
    
    @Override
    protected AnalysisComponent<VariableWithPcs> callAnalysor(TestConfiguration tConfig,
        AnalysisComponent<SourceFile> cmComponent) throws SetUpException {
        
        PcFinder finder = new PcFinder(tConfig, cmComponent);
        finder.execute();
        return finder;
    }
}
