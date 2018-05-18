package net.ssehub.kernel_haven.fe_analysis.fes;

import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.fe_analysis.AbstractFinderTests;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
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
     * Tests in case of active non-Boolean replacement, that feature effects from value assignments
     * and define-statements are combined correctly.
     */
    @Test
    public void testNonBooleanCompinedWithDefined() {
        Variable varA = new Variable("A");
        Variable varA1 = new Variable("A_eq_1");
        Variable varAA = new Variable("AA");
        Variable varB = new Variable("B");
         
        /*
         * #ifdef(A) ... #endif
         * #ifdef(FALSE) #ifdef(AA) ... #endif #endif
         * #ifdef(B) #ifdef(A=1) ... #endif #endif
         */
        CodeElement base = new CodeBlock(True.INSTANCE);
        CodeElement defABlock = new CodeBlock(varA);
        base.addNestedElement(defABlock);
         
        CodeElement defAABlock = new CodeBlock(new Conjunction(False.INSTANCE, varAA));
        base.addNestedElement(defAABlock);
         
        CodeElement defBBlock = new CodeBlock(varB);
        base.addNestedElement(defBBlock);
        CodeElement nestedVarA1Block = new CodeBlock(new Conjunction(varB, varA1));
        defBBlock.addNestedElement(nestedVarA1Block);
         
        List<VariableWithFeatureEffect> results = detectNonBooleanFEs(base);
        
        /*
         * Test the expected outcome (results should be ordered alphabetically), expected is:
         * A   -> True
         * AA  -> False
         * A=1 -> A or B -> TRUE or B
         * B   -> True
         */
        Assert.assertEquals(4,  results.size());
        VariableWithFeatureEffect resultA = results.get(0);
        Assert.assertEquals(varA.getName(), resultA.getVariable());
        Assert.assertEquals(True.INSTANCE, resultA.getFeatureEffect());
        
        VariableWithFeatureEffect resultAA = results.get(1);
        Assert.assertEquals(varAA.getName(), resultAA.getVariable());
        Assert.assertEquals(False.INSTANCE, resultAA.getFeatureEffect());
        
        VariableWithFeatureEffect resultA1 = results.get(2);
        Assert.assertEquals("A=1", resultA1.getVariable());
        Assert.assertEquals(new Disjunction(resultA.getFeatureEffect(), varB), resultA1.getFeatureEffect());
        
        VariableWithFeatureEffect resultB = results.get(3);
        Assert.assertEquals(varB.getName(), resultB.getVariable());
        Assert.assertEquals(True.INSTANCE, resultB.getFeatureEffect());
    }

    /**
     * Runs the {@link FeatureEffectFinder} on the passed element and returns the result for testing.
     * @param element A mocked element, which should be analyzed by the {@link FeatureEffectFinder}. 
     * @return The detected feature effects.
     */
    private List<VariableWithFeatureEffect> detectFEs(CodeElement element) {
        return super.runAnalysis(element, SimplificationType.NO_SIMPLIFICATION);
    }
    
    /**
     * Runs the {@link FeatureEffectFinder} on the passed element and returns the result for testing.
     * @param element A mocked element, which should be analyzed by the {@link FeatureEffectFinder}. 
     * @return The detected feature effects.
     */
    private List<VariableWithFeatureEffect> detectNonBooleanFEs(CodeElement element) {
        Properties config = new Properties();
        config.put(DefaultSettings.FUZZY_PARSING.getKey(), "true");
        
        return super.runAnalysis(element, SimplificationType.NO_SIMPLIFICATION, config);
    }
    
    @Override
    @SuppressWarnings("null")
    protected AnalysisComponent<VariableWithFeatureEffect> callAnalysor(TestConfiguration tConfig,
        AnalysisComponent<SourceFile> cmComponent) throws SetUpException {

        PcFinder pcFinder = new PcFinder(tConfig, cmComponent);
        FeatureEffectFinder feFinder = new FeatureEffectFinder(tConfig, pcFinder);
        feFinder.execute();
        return feFinder;
    }

}
