package net.ssehub.kernel_haven.fe_analysis.fes;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.fe_analysis.Settings;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link FeAggregator} class.
 * 
 * @author Adam
 */
@SuppressWarnings("null")
public class FeAggregatorTest {

    /**
     * Tests whether a single variable with two values is aggregated correctly.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testSimple() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(Settings.SIMPLIFIY);
        
        TestAnalysisComponentProvider<VariableWithFeatureEffect> input
            = new TestAnalysisComponentProvider<VariableWithFeatureEffect>(
                new VariableWithFeatureEffect("A=0", new Variable("B")),
                new VariableWithFeatureEffect("A=1", new Variable("C"))
        );
        FeAggregator ag = new FeAggregator(config, input);

        VariableWithFeatureEffect fe1 = ag.getNextResult();
        assertThat(fe1.getVariable(), is("A"));
        assertThat(fe1.getFeatureEffect(), is(new Disjunction(new Variable("B"), new Variable("C"))));
        
        assertThat(ag.getNextResult(), nullValue());
    }
    
    /**
     * Tests whether two variable with two values each are aggregated correctly.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testTwoVariables() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(Settings.SIMPLIFIY);
        
        TestAnalysisComponentProvider<VariableWithFeatureEffect> input
            = new TestAnalysisComponentProvider<VariableWithFeatureEffect>(
                new VariableWithFeatureEffect("A=0", new Variable("B")),
                new VariableWithFeatureEffect("A=1", new Variable("C")),
                new VariableWithFeatureEffect("B=0", new Variable("D")),
                new VariableWithFeatureEffect("B=1", new Variable("E"))
        );
        FeAggregator ag = new FeAggregator(config, input);

        VariableWithFeatureEffect fe = ag.getNextResult();
        assertThat(fe.getVariable(), is("A"));
        assertThat(fe.getFeatureEffect(), is(new Disjunction(new Variable("B"), new Variable("C"))));
        
        fe = ag.getNextResult();
        assertThat(fe.getVariable(), is("B"));
        assertThat(fe.getFeatureEffect(), is(new Disjunction(new Variable("D"), new Variable("E"))));
        
        assertThat(ag.getNextResult(), nullValue());
    }
    
    /**
     * Tests whether a variable without a value is aggregated correctly.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testVariableWithoutValue() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(Settings.SIMPLIFIY);
        
        TestAnalysisComponentProvider<VariableWithFeatureEffect> input
            = new TestAnalysisComponentProvider<VariableWithFeatureEffect>(
                new VariableWithFeatureEffect("A", new Variable("B")),
                new VariableWithFeatureEffect("A=0", new Variable("C")),
                new VariableWithFeatureEffect("A=1", new Variable("D"))
        );
        FeAggregator ag = new FeAggregator(config, input);

        VariableWithFeatureEffect fe = ag.getNextResult();
        assertThat(fe.getVariable(), is("A"));
        assertThat(fe.getFeatureEffect(), is(new Disjunction(new Variable("D"),
            new Disjunction(new Variable("B"), new Variable("C")))));
        
        assertThat(ag.getNextResult(), nullValue());
    }
    
    /**
     * Tests whether two variables without values are aggregated correctly.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testTwoVariablesWithoutValue() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(Settings.SIMPLIFIY);
        
        TestAnalysisComponentProvider<VariableWithFeatureEffect> input
            = new TestAnalysisComponentProvider<VariableWithFeatureEffect>(
                new VariableWithFeatureEffect("A", new Variable("B")),
                new VariableWithFeatureEffect("A=0", new Variable("C")),
                new VariableWithFeatureEffect("A=1", new Variable("D")),
                new VariableWithFeatureEffect("B", new Variable("C")),
                new VariableWithFeatureEffect("B=0", new Variable("D"))
                );
        FeAggregator ag = new FeAggregator(config, input);
        
        VariableWithFeatureEffect fe = ag.getNextResult();
        assertThat(fe.getVariable(), is("A"));
        assertThat(fe.getFeatureEffect(), is(new Disjunction(new Variable("D"),
            new Disjunction(new Variable("B"), new Variable("C")))));
        
        fe = ag.getNextResult();
        assertThat(fe.getVariable(), is("B"));
        assertThat(fe.getFeatureEffect(), is(new Disjunction(new Variable("C"), new Variable("D"))));
        
        assertThat(ag.getNextResult(), nullValue());
    }
    
    /**
     * Tests whether variables without values and variables with similar names are not mixed. <br/>
     * Tests a bug reported at 15.02.2018
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testVariableWithoutValueAndSimilarVariables() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(Settings.SIMPLIFIY);
        
        TestAnalysisComponentProvider<VariableWithFeatureEffect> input
            = new TestAnalysisComponentProvider<VariableWithFeatureEffect>(
                new VariableWithFeatureEffect("A", new Variable("B")),
                new VariableWithFeatureEffect("AA=0", new Variable("F")),
                new VariableWithFeatureEffect("AA=1", new Variable("G")),
                new VariableWithFeatureEffect("A=0", new Variable("C")),
                new VariableWithFeatureEffect("A=1", new Variable("D"))
                );
        FeAggregator ag = new FeAggregator(config, input);
        
        List<VariableWithFeatureEffect> allResults = new ArrayList<>();
        VariableWithFeatureEffect fe;
        while ((fe = ag.getNextResult()) != null) {
            allResults.add(fe);
        }
        
        Assert.assertEquals("Unexpected number of results, probably some results are doubled.", 2, allResults.size());
        VariableWithFeatureEffect resultVarA = allResults.get(0);
        Assert.assertEquals("A", resultVarA.getVariable());
        
        // Check that the same variables are used in disjunction term (in any order)
        Formula expectedFE = new Disjunction(new Variable("B"), new Disjunction(new Variable("C"), new Variable("D")));
        String[] usedVarsExpected = expectedFE.toString().split(" \\|\\| ");
        List<String>usedActual = Arrays.asList(resultVarA.getFeatureEffect().toString().split(" \\|\\| "));
        Assert.assertEquals(usedVarsExpected.length, usedActual.size());
        assertThat(usedActual, hasItems(usedVarsExpected));
    }
    
    /**
     * Tests whether results are not simplified if simplification is turned off. 
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testNoSimplify() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(Settings.SIMPLIFIY);
        
        TestAnalysisComponentProvider<VariableWithFeatureEffect> input
            = new TestAnalysisComponentProvider<VariableWithFeatureEffect>(
                new VariableWithFeatureEffect("A=0", new Variable("C")),
                new VariableWithFeatureEffect("A=1", new Variable("C")),
                new VariableWithFeatureEffect("B=0", new Variable("D")),
                new VariableWithFeatureEffect("B=1", new Negation(new Variable("D")))
                );
        
        FeAggregator ag = new FeAggregator(config, input);
        
        VariableWithFeatureEffect fe = ag.getNextResult();
        assertThat(fe.getVariable(), is("A"));
        assertThat(fe.getFeatureEffect(), is(new Disjunction(new Variable("C"), new Variable("C"))));
        
        fe = ag.getNextResult();
        assertThat(fe.getVariable(), is("B"));
        assertThat(fe.getFeatureEffect(), is(new Disjunction(new Variable("D"), new Negation(new Variable("D")))));
        
        assertThat(ag.getNextResult(), nullValue());
    }
    
}
