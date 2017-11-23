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
import net.ssehub.kernel_haven.feature_effects.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.feature_effects.PresenceConditionAnalysisHelper.SimplificationType;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link FeatureEffectFinder} without any simplification.
 * @author El-Sharkawy
 *
 */
public class FeatureEffectFinderTests extends AbstractFinderTests<VariableWithFeatureEffect> {
    
    /**
     * Initializes the logger.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        Logger.init();
    }
    
    /**
     * Checks if a variable, which is used at toplevel, has no feature effect condition.
     */
    @Test
    public void testAlwaysOnFeature() {
        Variable varA = new Variable("A");
        CodeElement element = new CodeBlock(varA);
        List<VariableWithFeatureEffect> results = detectFEs(element);
        
        // Test the expected outcome
        Assert.assertEquals(1,  results.size());
        VariableWithFeatureEffect result1 = results.get(0);
        Assert.assertEquals(varA.getName(), result1.getVariable());
        Assert.assertSame(True.INSTANCE, result1.getFeatureEffect());
    }

    /**
     * Runs the {@link FeatureEffectFinder} on the passed element and returns the result for testing.
     * @param element A mocked element, which should be analyzed by the {@link FeatureEffectFinder}. 
     * @return The detected feature effects.
     */
    private List<VariableWithFeatureEffect> detectFEs(CodeElement element) {
        return super.runAnalysis(element, SimplificationType.NO_SIMPLIFICATION);
    }
    
    @Override
    protected AnalysisComponent<VariableWithFeatureEffect> callAnalysor(TestConfiguration tConfig,
        AnalysisComponent<SourceFile> cmComponent) throws SetUpException {

        PcFinder pcFinder = new PcFinder(tConfig, cmComponent);
        FeatureEffectFinder feFinder = new FeatureEffectFinder(tConfig, pcFinder);
        feFinder.execute();
        return feFinder;
    }

}
