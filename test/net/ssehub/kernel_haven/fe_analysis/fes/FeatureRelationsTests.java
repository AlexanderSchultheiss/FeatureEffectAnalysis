package net.ssehub.kernel_haven.fe_analysis.fes;

import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureRelations.FeatureDependencyRelation;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Tests the {@link FeatureRelations}.
 * @author El-Sharkawy
 *
 */
public class FeatureRelationsTests {
    
    /**
     * Tests whether the same feature, with and without an operator is treated as single feature only.
     */
    @Test
    public void testOperatorRemoval() {
        // Run Analysis
        VariableWithFeatureEffect[] input = new VariableWithFeatureEffect[2];
        input[0] = new VariableWithFeatureEffect("A", True.INSTANCE);
        input[1] = new VariableWithFeatureEffect("A>0", True.INSTANCE);
        List<FeatureDependencyRelation> result = executeAnalysis(input);
        
        // Check result
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(result.get(0).getFeature(), input[0].getVariable());
        Assert.assertEquals(result.get(0).getDependsOn(), "TRUE");
    }
    
    /**
     * Performs a {@link FeatureRelations}-analysis and returns the results.
     * @param inputs The input for the analysis.
     * @return The computed result of the {@link FeatureRelations}-component.
     */
    public List<FeatureDependencyRelation> executeAnalysis(VariableWithFeatureEffect[] inputs) {
        return executeAnalysis(null, inputs);
    }
    
    /**
     * Performs a {@link FeatureRelations}-analysis and returns the results.
     * @param config Optional: Configuration of the {@link FeatureRelations}-component.
     * @param inputs The input for the analysis.
     * @return The computed result of the {@link FeatureRelations}-component.
     */
    private List<FeatureDependencyRelation> executeAnalysis(@Nullable Properties config,
        VariableWithFeatureEffect[] inputs) {
        
        TestConfiguration tConfig = null;
        Properties properties = (null != config) ? config : new Properties();
        try {
            tConfig = new TestConfiguration(properties);
        } catch (SetUpException e) {
            Assert.fail("Could not create test configuration :" + e.getMessage());
        }
        
        return AnalysisComponentExecuter.executeComponent(FeatureRelations.class, tConfig, inputs);
    }
}
