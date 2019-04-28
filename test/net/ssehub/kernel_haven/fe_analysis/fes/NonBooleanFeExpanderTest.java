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

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link NonBooleanFeExpander}.
 * 
 * @author Adam
 */
public class NonBooleanFeExpanderTest {

    /**
     * Tests the simple case: {@code A -> B; A=1 -> C}.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testSimple() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.setValue(DefaultSettings.FUZZY_PARSING, true);
        
        VariableWithFeatureEffect fe1 = new VariableWithFeatureEffect("A", new Variable("B"));
        VariableWithFeatureEffect fe2 = new VariableWithFeatureEffect("A=1", new Variable("C"));
        
        List<VariableWithFeatureEffect> result = AnalysisComponentExecuter.executeComponent(
                NonBooleanFeExpander.class, config, new VariableWithFeatureEffect[] {fe1, fe2});
        
        assertThat(result, is(Arrays.asList(
                new VariableWithFeatureEffect("A", new Variable("B")),
                new VariableWithFeatureEffect("A=1", or("B", "C"))
        )));
    }
    
    /**
     * Tests that another variable follows.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testOtherVar() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.setValue(DefaultSettings.FUZZY_PARSING, true);
        
        VariableWithFeatureEffect fe1 = new VariableWithFeatureEffect("A", new Variable("B"));
        VariableWithFeatureEffect fe2 = new VariableWithFeatureEffect("A=1", new Variable("C"));
        VariableWithFeatureEffect fe3 = new VariableWithFeatureEffect("B", new Variable("A"));
        VariableWithFeatureEffect fe4 = new VariableWithFeatureEffect("B=1", new Variable("A=1"));
        VariableWithFeatureEffect fe5 = new VariableWithFeatureEffect("B=2", new Variable("C"));
        
        List<VariableWithFeatureEffect> result = AnalysisComponentExecuter.executeComponent(
                NonBooleanFeExpander.class, config, new VariableWithFeatureEffect[] {fe1, fe2, fe3, fe4, fe5});
        
        assertThat(result, is(Arrays.asList(
                new VariableWithFeatureEffect("A", new Variable("B")),
                new VariableWithFeatureEffect("A=1", or("B", "C")),
                new VariableWithFeatureEffect("B", new Variable("A")),
                new VariableWithFeatureEffect("B=1", or("A", "A=1")),
                new VariableWithFeatureEffect("B=2", or("A", "C"))
                )));
    }
    
    /**
     * Tests that a variable appears between base variable and value variable (e.g. A;AA;A=1).
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testVariableInTheMiddle() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.setValue(DefaultSettings.FUZZY_PARSING, true);
        
        VariableWithFeatureEffect fe1 = new VariableWithFeatureEffect("A", new Variable("B"));
        VariableWithFeatureEffect fe2 = new VariableWithFeatureEffect("AA", new Variable("D"));
        VariableWithFeatureEffect fe3 = new VariableWithFeatureEffect("A=1", new Variable("C"));
        
        List<VariableWithFeatureEffect> result = AnalysisComponentExecuter.executeComponent(
                NonBooleanFeExpander.class, config, new VariableWithFeatureEffect[] {fe1, fe2, fe3});
        
        assertThat(result, is(Arrays.asList(
                new VariableWithFeatureEffect("A", new Variable("B")),
                new VariableWithFeatureEffect("AA", new Variable("D")),
                new VariableWithFeatureEffect("A=1", or("B", "C"))
                )));
    }

}
