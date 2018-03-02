package net.ssehub.kernel_haven.fe_analysis.pcs;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.fe_analysis.AbstractFinderTests;
import net.ssehub.kernel_haven.fe_analysis.RunOnlyInANT;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Special tests of the {@link PcFinder}, which test that formulas can be simplified. These tests can only be executed
 * when called from ANT scripts.
 * @author El-Sharkawy
 *
 */
@RunWith(value = RunOnlyInANT.class)
public class PcFinderTestsWithSimplificationTests extends AbstractFinderTests<VariableWithPcs> {

    /**
     * Tests that a single condition in the form of <tt>A || A</tt> can be identified and simplified.
     */
    @Test
    public void testSimplificationOfSingleStatement() {
        Variable varA = new Variable("A");
        Formula tooComplex = new Disjunction(varA, varA);
        CodeElement element = new CodeBlock(tooComplex);
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
        return super.runAnalysis(element, SimplificationType.PRESENCE_CONDITIONS);
    }
    
    @Override
    @SuppressWarnings("null")
    protected AnalysisComponent<VariableWithPcs> callAnalysor(TestConfiguration tConfig,
        AnalysisComponent<SourceFile> cmComponent) throws SetUpException {
        
        PcFinder finder = new PcFinder(tConfig, cmComponent);
        finder.execute();
        return finder;
    }
}
