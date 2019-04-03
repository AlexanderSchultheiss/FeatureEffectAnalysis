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
package net.ssehub.kernel_haven.fe_analysis.fes;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
import net.ssehub.kernel_haven.util.logic.Variable;
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
     * Tests that context calculation works correctly.
     */
    @Test
    public void testContext() {
        // Run Analysis
        VariableWithFeatureEffect[] input = new VariableWithFeatureEffect[2];
        input[0] = new VariableWithFeatureEffect("A", or("B", "C"));
        input[1] = new VariableWithFeatureEffect("B", and("C", "D"));
        List<FeatureDependencyRelation> result = executeAnalysis(input);
        
        // Check result
        assertThat(result.size(), is(4));
        
        assertThat(result.get(0).getFeature(), is("A"));
        assertThat(result.get(0).getDependsOn(), is("B"));
        assertThat(result.get(0).getContext(), is(True.INSTANCE));
        
        assertThat(result.get(1).getFeature(), is("A"));
        assertThat(result.get(1).getDependsOn(), is("C"));
        assertThat(result.get(1).getContext(), is(True.INSTANCE));
        
        assertThat(result.get(2).getFeature(), is("B"));
        assertThat(result.get(2).getDependsOn(), is("C"));
        assertThat(result.get(2).getContext(), is(new Variable("D")));
        
        assertThat(result.get(3).getFeature(), is("B"));
        assertThat(result.get(3).getDependsOn(), is("D"));
        assertThat(result.get(3).getContext(), is(new Variable("C")));
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
