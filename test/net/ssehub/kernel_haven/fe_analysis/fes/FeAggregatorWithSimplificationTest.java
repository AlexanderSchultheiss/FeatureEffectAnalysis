package net.ssehub.kernel_haven.fe_analysis.fes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.fe_analysis.Settings;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.StaticClassLoader;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link FeAggregator} class, with simplification.
 * 
 * @author Adam
 */
public class FeAggregatorWithSimplificationTest {
    
    /**
     * Makes sure that LogicUtils has been initialized. This is only needed in test cases, because
     * {@link StaticClassLoader} does not run.
     * 
     * @throws ReflectiveOperationException unwanted.
     * @throws SecurityException unwanted.
     */
    @BeforeClass
    public static void loadLogicUtils() throws ReflectiveOperationException, SecurityException {
        Class.forName("net.ssehub.kernel_haven.logic_utils.LogicUtils").getMethod(StaticClassLoader.INIT_METHOD_NAME)
            .invoke(null);
    }
    
    /**
     * Tests whether results are simplified correctly. 
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    @SuppressWarnings("null")
    public void testSimplify() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(Settings.SIMPLIFIY);
        config.setValue(Settings.SIMPLIFIY, SimplificationType.FEATURE_EFFECTS);
        
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
        assertThat(fe.getFeatureEffect(), is(new Variable("C")));
        
        fe = ag.getNextResult();
        assertThat(fe.getVariable(), is("B"));
        assertThat(fe.getFeatureEffect(), is(True.INSTANCE));
        
        assertThat(ag.getNextResult(), nullValue());
    }

}
