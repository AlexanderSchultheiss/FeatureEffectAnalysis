package net.ssehub.kernel_haven.fe_analysis.fes;

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
import net.ssehub.kernel_haven.fe_analysis.AbstractFinderTests;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder;
import net.ssehub.kernel_haven.logic_utils.LogicUtils;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.StaticClassLoader;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link FeatureEffectFinder} with simplification.
 * @author El-Sharkawy
 *
 */
public class FeatureEffectFinderWithSimplificationTests extends AbstractFinderTests<VariableWithFeatureEffect> {
    
    /**
     * Makes sure that LogicUtils has been initialized. This is only needed in test cases, because
     * {@link StaticClassLoader} does not run.
     * 
     * @throws SetUpException unwanted.
     */
    @BeforeClass
    public static void loadLogicUtils() throws SetUpException {
        LogicUtils.initialize(new TestConfiguration(new Properties()));
    }
    
    /**
     * Checks if a variable, which is (always) nested but still has always an effect, is handled correctly.
     */
    @Test
    public void testAlwaysNestedButAlwaysOn() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula notA = new Negation(varA);
        CodeElement element1 = new CodeBlock(varA);
        element1.addNestedElement(new CodeBlock(new Conjunction(varB, varA)));
        CodeElement element2 = new CodeBlock(notA);
        element2.addNestedElement(new CodeBlock(new Conjunction(varB, notA)));
        CodeElement element = new CodeBlock(True.INSTANCE);
        element.addNestedElement(element1);
        element.addNestedElement(element2);
        List<VariableWithFeatureEffect> results = detectFEs(element);
       
        // Test the expected outcome (results should be ordered alphabetically)
        Assert.assertEquals(2,  results.size());
        VariableWithFeatureEffect resultA = results.get(0);
        Assert.assertSame(varA.getName(), resultA.getVariable());
        Assert.assertSame(True.INSTANCE, resultA.getFeatureEffect());
        // B -> (A || !A) <-> B -> TRUE
        VariableWithFeatureEffect resultB = results.get(1);
        Assert.assertSame(varB.getName(), resultB.getVariable());
        Assert.assertSame(True.INSTANCE, resultB.getFeatureEffect());
    }
    
    /**
     * Checks if a variable, which is (always) nested below the same feature is handled correctly.
     */
    @Test
    public void testSimpleNestedFeature() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Variable varC = new Variable("C");
        Formula aANDb = new Conjunction(varA, varB);
        CodeElement element1 = new CodeBlock(varA);
        element1.addNestedElement(new CodeBlock(new Conjunction(varC, varA)));
        CodeElement element2 = new CodeBlock(aANDb);
        element2.addNestedElement(new CodeBlock(new Conjunction(varC, aANDb)));
        CodeElement element = new CodeBlock(True.INSTANCE);
        element.addNestedElement(element1);
        element.addNestedElement(element2);
        List<VariableWithFeatureEffect> results = detectFEs(element);
        
        // Test the expected outcome (results should be ordered alphabetically)
        Assert.assertEquals(3,  results.size());
        // A -> True
        VariableWithFeatureEffect resultA = results.get(0);
        Assert.assertSame(varA.getName(), resultA.getVariable());
        Assert.assertSame(True.INSTANCE, resultA.getFeatureEffect());
        // B -> A
        VariableWithFeatureEffect resultB = results.get(1);
        Assert.assertSame(varB.getName(), resultB.getVariable());
        Assert.assertSame(varA, resultB.getFeatureEffect());
        // C -> A or (A and B) <-> C -> A
        VariableWithFeatureEffect resultC = results.get(2);
        Assert.assertSame(varC.getName(), resultC.getVariable());
        Assert.assertSame(varA, resultC.getFeatureEffect());
    }

    /**
     * Runs the {@link FeatureEffectFinder} on the passed element and returns the result for testing.
     * @param element A mocked element, which should be analyzed by the {@link FeatureEffectFinder}. 
     * @return The detected feature effects.
     */
    private List<VariableWithFeatureEffect> detectFEs(CodeElement element) {
        return super.runAnalysis(element, SimplificationType.FEATURE_EFFECTS);
    }
    
    @Override
    @SuppressWarnings("null")
    protected AnalysisComponent<VariableWithFeatureEffect> createAnalysor(TestConfiguration tConfig,
        AnalysisComponent<SourceFile> cmComponent) throws SetUpException {

        PcFinder pcFinder = new PcFinder(tConfig, cmComponent);
        FeatureEffectFinder feFinder = new FeatureEffectFinder(tConfig, pcFinder);
        return feFinder;
    }

}
