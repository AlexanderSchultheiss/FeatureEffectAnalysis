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
package net.ssehub.kernel_haven.fe_analysis.pcs;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.fe_analysis.AbstractFinderTests;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link PcFinder}.
 * @author El-Sharkawy
 *
 */
@SuppressWarnings("null")
public class PcFinderTests extends AbstractFinderTests<VariableWithPcs> {
    
    private BuildModel bm;
    
    private boolean considerAll;
    
    /**
     * Checks if a single statement with only 1 variable is detected correctly.
     */
    @Test
    public void testRetrievalOfSingleStatement() {
        Variable varA = new Variable("A");
        CodeBlock element = new CodeBlock(varA);
        List<VariableWithPcs> results = detectPCs(element);
        
        // Test the expected outcome
        Assert.assertEquals(1,  results.size());
        VariableWithPcs result1 = results.get(0);
        Assert.assertEquals(varA.getName(), result1.getVariable());
        Assert.assertEquals(1, result1.getPcs().size());
        Assert.assertTrue(result1.getPcs().contains(varA));
    }
    
    /**
     * Checks if multiple PCs are found correctly.
     */
    @Test
    public void testMultipleElements() {
        CodeBlock c1 = new CodeBlock(or("A", "B"));
        CodeBlock c2 = new CodeBlock(and("A", "C"));
        
        CodeBlock top = new CodeBlock(True.INSTANCE);
        top.addNestedElement(c1);
        top.addNestedElement(c2);
        
        List<VariableWithPcs> results = detectPCs(top);

        VariableWithPcs r = results.get(0);
        assertThat(r.getVariable(), is("A"));
        assertThat(r.getPcs(), is(set(or("A", "B"), and("A", "C"))));
        
        r = results.get(1);
        assertThat(r.getVariable(), is("B"));
        assertThat(r.getPcs(), is(set(or("A", "B"))));
        
        r = results.get(2);
        assertThat(r.getVariable(), is("C"));
        assertThat(r.getPcs(), is(set(and("A", "C"))));
        
        Assert.assertEquals(3,  results.size());
    }
    
    /**
     * Checks if multiple PCs are found correctly.
     */
    @Test
    public void testMultipleNestedElements() {
        CodeBlock c1 = new CodeBlock(or("A", "B"));
        CodeBlock c2 = new CodeBlock(new Variable("A"));
        
        c2.addNestedElement(new CodeBlock(and("A", "C")));
        
        CodeBlock top = new CodeBlock(True.INSTANCE);
        top.addNestedElement(c1);
        top.addNestedElement(c2);
        
        List<VariableWithPcs> results = detectPCs(top);

        VariableWithPcs r = results.get(0);
        assertThat(r.getVariable(), is("A"));
        assertThat(r.getPcs(), is(set(or("A", "B"), and("A", "C"), new Variable("A"))));
        
        r = results.get(1);
        assertThat(r.getVariable(), is("B"));
        assertThat(r.getPcs(), is(set(or("A", "B"))));
        
        r = results.get(2);
        assertThat(r.getVariable(), is("C"));
        assertThat(r.getPcs(), is(set(and("A", "C"))));
        
        Assert.assertEquals(3,  results.size());
    }
    
    /**
     * Checks if the BM PCs are considered correctly.
     */
    @Test
    public void testWithBuildModel() {
        bm = new BuildModel();
        bm.add(new File("file1.c"), new Variable("B"));
        bm.add(new File("file2.c"), new Variable("C")); // irrelevant
        
        CodeBlock top = new CodeBlock(new Variable("A"));
        
        List<VariableWithPcs> results = detectPCs(top);

        VariableWithPcs r = results.get(0);
        assertThat(r.getVariable(), is("A"));
        assertThat(r.getPcs(), is(set(and("B", "A"))));
        
        r = results.get(1);
        assertThat(r.getVariable(), is("B"));
        assertThat(r.getPcs(), is(set(new Variable("B"), and("B", "A"))));
        
        Assert.assertEquals(2,  results.size());
    }
    
    /**
     * Checks if the BM PCs are considered correctly, if only irreleveant PCs are there.
     */
    @Test
    public void testWithIrrelevantBuildModel() {
        bm = new BuildModel();
        bm.add(new File("file2.c"), new Variable("C")); // irrelevant
        
        CodeBlock top = new CodeBlock(new Variable("A"));
        
        List<VariableWithPcs> results = detectPCs(top);

        VariableWithPcs r = results.get(0);
        assertThat(r.getVariable(), is("A"));
        assertThat(r.getPcs(), is(set(new Variable("A"))));
        
        Assert.assertEquals(1,  results.size());
    }
    
    /**
     * Checks if the BM PCs are considered correctly.
     */
    @Test
    public void testConsiderAllBuildModel() {
        bm = new BuildModel();
        bm.add(new File("file1.c"), new Variable("B"));
        bm.add(new File("file2.c"), new Variable("C")); // irrelevant, but will still be considered
        
        considerAll = true;
        
        CodeBlock top = new CodeBlock(new Variable("A"));
        
        List<VariableWithPcs> results = detectPCs(top);

        VariableWithPcs r = results.get(0);
        assertThat(r.getVariable(), is("A"));
        assertThat(r.getPcs(), is(set(and("B", "A"))));
        
        r = results.get(1);
        assertThat(r.getVariable(), is("B"));
        assertThat(r.getPcs(), is(set(new Variable("B"), and("B", "A"))));
        
        r = results.get(2);
        assertThat(r.getVariable(), is("C"));
        assertThat(r.getPcs(), is(set(new Variable("C"))));
        
        Assert.assertEquals(3,  results.size());
    }
    
    /**
     * Checks if non-boolean collapsing works.
     */
    @Test
    public void testCombineNonBoolean() {
        CodeBlock c1 = new CodeBlock(or("A", "B"));
        CodeBlock c2 = new CodeBlock(and("A_eq_1", "C_lt_1"));
        
        CodeBlock top = new CodeBlock(True.INSTANCE);
        top.addNestedElement(c1);
        top.addNestedElement(c2);
        
        Properties props = new Properties();
        props.put(PcFinder.COMBINE_NON_BOOLEAN.getKey(), "true");
        List<VariableWithPcs> results = super.runAnalysis(top, SimplificationType.NO_SIMPLIFICATION, props);

        VariableWithPcs r = results.get(0);
        assertThat(r.getVariable(), is("A"));
        assertThat(r.getPcs(), is(set(or("A", "B"), and("A", "C"))));
        
        r = results.get(1);
        assertThat(r.getVariable(), is("B"));
        assertThat(r.getPcs(), is(set(or("A", "B"))));
        
        r = results.get(2);
        assertThat(r.getVariable(), is("C"));
        assertThat(r.getPcs(), is(set(and("A", "C"))));
        
        Assert.assertEquals(3,  results.size());
    }
    
    /**
     * Creates a set from varargs.
     * 
     * @param ts The elements to add in the set.
     * @param <T> The set type.
     * 
     * @return A set containing the elements.
     */
    @SafeVarargs
    private static <T> Set<T> set(T ... ts) {
        HashSet<T> set = new HashSet<>();
        for (T t : ts) {
            set.add(t);
        }
        return set;
    }
    
    /**
     * Runs the {@link PcFinder} on the passed element and returns the result for testing.
     * @param element A mocked element, which should be analyzed by the {@link PcFinder}. 
     * @return The detected presence conditions.
     */
    private List<VariableWithPcs> detectPCs(CodeElement<?> element) {
        return super.runAnalysis(element, SimplificationType.NO_SIMPLIFICATION);
    }
    
    @Override
    protected AnalysisComponent<VariableWithPcs> createAnalysor(TestConfiguration tConfig,
        AnalysisComponent<SourceFile<?>> cmComponent) throws SetUpException {
        
        tConfig.registerSetting(PcFinder.CONSIDER_ALL_BM);
        tConfig.setValue(PcFinder.CONSIDER_ALL_BM, considerAll);
        
        PcFinder finder;
        if (bm == null) {
            finder = new PcFinder(tConfig, cmComponent);
        } else {
            TestAnalysisComponentProvider<BuildModel> bmProvider = new TestAnalysisComponentProvider<BuildModel>(bm);
            finder = new PcFinder(tConfig, cmComponent, bmProvider);
        }
        
        return finder;
    }
}
