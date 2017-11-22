package net.ssehub.kernel_haven.feature_effects;

import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.feature_effects.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.feature_effects.PresenceConditionAnalysisHelper.SimplificationType;
import net.ssehub.kernel_haven.util.Logger;
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
public class PcFinderTestsWithSimplificationTests extends AbstractPcFinderTests {

    /**
     * Initializes the logger.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        Logger.init();
    }
    
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
        return super.detectPCs(element, SimplificationType.PRESENCE_CONDITIONS);
    }
}
