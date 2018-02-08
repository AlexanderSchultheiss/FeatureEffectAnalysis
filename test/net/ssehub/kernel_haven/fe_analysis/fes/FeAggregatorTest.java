package net.ssehub.kernel_haven.fe_analysis.fes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.fe_analysis.Settings;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Disjunction;
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
     * Initializes the logger.
     */
    @BeforeClass
    public static void beforeClass() {
        if (null == Logger.get()) {
            Logger.init();
        }
    }
    
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
