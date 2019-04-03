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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.fe_analysis.AbstractFinderTests;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link ThreadedFeatureEffectFinder}.
 * 
 * @author Adam
 */
@SuppressWarnings("null")
public class ThreadedFeatureEffectFinderTest extends AbstractFinderTests<VariableWithFeatureEffect> {

    /**
     * Checks if a single feature effect is calculated correctly.
     */
    @Test
    public void testSingleFeatureEffect() {
        Variable varA = new Variable("A");
        CodeBlock element = new CodeBlock(varA);
        List<VariableWithFeatureEffect> results = detectFEs(element);
        
        // Test the expected outcome
        Assert.assertEquals(1,  results.size());
        VariableWithFeatureEffect result1 = results.get(0);
        Assert.assertSame(varA.getName(), result1.getVariable());
        Assert.assertSame(True.INSTANCE, result1.getFeatureEffect());
    }
    
    /**
     * Checks if bunch of feature effects get calculated in the correct order.
     */
    @Test
    public void testCorrectOrder() {
        final int minElement = 10;
        final int maxElement = 18;
        
        CodeBlock topLevelElement = new CodeBlock(True.INSTANCE);
        Formula pc = True.INSTANCE;
        // do higher elements first -> lower elements have more complex FEs
        for (int i = maxElement; i >= minElement; i--) {
            
            // randomly switch between conjunction and disjunction
            if (Math.random() > 0.5) {
                
                // randomly switch sides
                if (Math.random() > 0.5) {
                    pc = new Conjunction(pc, new Variable("VAR_" + i));
                } else {
                    pc = new Conjunction(new Variable("VAR_" + i), pc);
                }
                
            } else {
                
                // randomly switch sides
                if (Math.random() > 0.5) {
                    pc = new Disjunction(pc, new Variable("VAR_" + i));
                } else {
                    pc = new Disjunction(new Variable("VAR_" + i), pc);
                }
            }
            
            // randomly add some negations
            if (Math.random() > 0.5) {
                pc = new Negation(pc);
            }
            
            CodeBlock nested = new CodeBlock(pc);
            topLevelElement.addNestedElement(nested);
        }
        
        List<VariableWithFeatureEffect> results = detectFEs(topLevelElement);
        
        int index = 0;
        for (int i = minElement; i <= maxElement; i++) {
            assertThat(results.get(index++).getVariable(), is("VAR_" + i));
        }
        
    }
    
    /**
     * Runs the {@link ThreadedFeatureEffectFinder} on the passed element and returns the result for testing.
     * @param element A mocked element, which should be analyzed by the {@link ThreadedFeatureEffectFinder}.
     * 
     * @return The detected feature effects.
     */
    private List<VariableWithFeatureEffect> detectFEs(CodeElement<?> element) {
        return super.runAnalysis(element, SimplificationType.FEATURE_EFFECTS);
    }
    
    @Override
    protected AnalysisComponent<VariableWithFeatureEffect> createAnalysor(TestConfiguration tConfig,
            AnalysisComponent<SourceFile<?>> cmComponent) throws SetUpException {
        
        PcFinder pcFinder = new PcFinder(tConfig, cmComponent);
        ThreadedFeatureEffectFinder feFinder = new ThreadedFeatureEffectFinder(tConfig, pcFinder);
        return feFinder;
    }

}
