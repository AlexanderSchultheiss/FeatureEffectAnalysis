package net.ssehub.kernel_haven.fe_analysis.fes;

import java.util.List;

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
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
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
        Assert.assertSame(varA.getName(), result1.getVariable());
        Assert.assertSame(True.INSTANCE, result1.getFeatureEffect());
    }
    
    /**
     * Checks if a variable, which is (always) nested below the same feature is handled correctly.
     */
    @Test
    public void testSimpleNestedFeature() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        CodeElement element = new CodeBlock(varA);
        CodeElement nestedElement = new CodeBlock(new Conjunction(varB, varA));
        element.addNestedElement(nestedElement);
        List<VariableWithFeatureEffect> results = detectFEs(element);
        
        // Test the expected outcome (results should be ordered alphabetically)
        Assert.assertEquals(2,  results.size());
        VariableWithFeatureEffect resultA = results.get(0);
        Assert.assertSame(varA.getName(), resultA.getVariable());
        Assert.assertSame(True.INSTANCE, resultA.getFeatureEffect());
        VariableWithFeatureEffect resultB = results.get(1);
        Assert.assertSame(varB.getName(), resultB.getVariable());
        Assert.assertSame(varA, resultB.getFeatureEffect());
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
        // B -> (A || !A)
        VariableWithFeatureEffect resultB = results.get(1);
        Assert.assertSame(varB.getName(), resultB.getVariable());
        Assert.assertEquals(new Disjunction(varA, notA), resultB.getFeatureEffect());
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
