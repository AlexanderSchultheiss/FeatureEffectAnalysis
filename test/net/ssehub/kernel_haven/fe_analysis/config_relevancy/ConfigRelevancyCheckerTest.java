package net.ssehub.kernel_haven.fe_analysis.config_relevancy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.fe_analysis.config_relevancy.ConfigRelevancyChecker;
import net.ssehub.kernel_haven.fe_analysis.config_relevancy.VariableRelevance;
import net.ssehub.kernel_haven.fe_analysis.config_relevancy.VariableRelevance.Relevance;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link ConfigRelevancyChecker}.
 * @author Adam
 *
 */
public class ConfigRelevancyCheckerTest {

    /**
     * Initializes the logger.
     */
    @BeforeClass
    public static void initLogger() {
        Logger.init();
    }
    
    /**
     * Tests how the {@link ConfigRelevancyChecker} handles a completely configured configuration.
     * @throws SetUpException Not expected
     * @throws IOException Not expected
     */
    @Test
    public void testFullyDefined() throws SetUpException, IOException {
        List<VariableWithFeatureEffect> fes = new LinkedList<>();
        fes.add(new VariableWithFeatureEffect("VAR_1", new Variable("B=1")));
        fes.add(new VariableWithFeatureEffect("VAR_2", new Variable("B=2")));
        fes.add(new VariableWithFeatureEffect("A", new Variable("B=1")));
        fes.add(new VariableWithFeatureEffect("B", new Variable("A=1")));
        
        List<VariableRelevance> result = run(fes, new File("testdata/config_relevancy/test.csv"));

        VariableRelevance r1 = result.get(0);
        assertThat(r1.getVariable(), is("VAR_1"));
        assertThat(r1.getValue(), nullValue());
        assertThat(r1.getRelevance(), is(Relevance.NOT_SET_AND_IRRELEVANT));
        assertThat(r1.getFeatureEffect(), is(new Variable("B=1")));
        
        VariableRelevance r2 = result.get(1);
        assertThat(r2.getVariable(), is("VAR_2"));
        assertThat(r2.getValue(), nullValue());
        assertThat(r2.getRelevance(), is(Relevance.NOT_SET_AND_RELEVANT));
        assertThat(r2.getFeatureEffect(), is(new Variable("B=2")));
        
        VariableRelevance r3 = result.get(2);
        assertThat(r3.getVariable(), is("A"));
        assertThat(r3.getValue(), is(1));
        assertThat(r3.getRelevance(), is(Relevance.SET_AND_IRRELEVANT));
        assertThat(r3.getFeatureEffect(), is(new Variable("B=1")));
        
        VariableRelevance r4 = result.get(3);
        assertThat(r4.getVariable(), is("B"));
        assertThat(r4.getValue(), is(2));
        assertThat(r4.getRelevance(), is(Relevance.SET_AND_RELEVANT));
        assertThat(r4.getFeatureEffect(), is(new Variable("A=1")));
        
        assertThat(result.size(), is(4));
    }
    
    @Test
    public void testFeatureEffectVariablesWithoutEquals() throws SetUpException, IOException {
        List<VariableWithFeatureEffect> fes = new LinkedList<>();
        fes.add(new VariableWithFeatureEffect("VAR_1", new Disjunction(new Variable("A"), new Variable("B=1"))));
        
        List<VariableRelevance> result = run(fes, new File("testdata/config_relevancy/test.csv"));

        VariableRelevance r1 = result.get(0);
        assertThat(r1.getVariable(), is("VAR_1"));
        assertThat(r1.getValue(), nullValue());
        assertThat(r1.getRelevance(), is(Relevance.NOT_SET_AND_RELEVANT));
        assertThat(r1.getFeatureEffect(), is(new Disjunction(new Variable("A"), new Variable("B=1"))));
        
        assertThat(result.size(), is(1));
    }
    
    @Test
    public void testUnkownFeatureEffectVariableButStillDefined() throws SetUpException, IOException {
        List<VariableWithFeatureEffect> fes = new LinkedList<>();
        fes.add(new VariableWithFeatureEffect("VAR_1", new Disjunction(new Variable("A=1"), new Variable("C=4"))));
        
        List<VariableRelevance> result = run(fes, new File("testdata/config_relevancy/test.csv"));

        VariableRelevance r1 = result.get(0);
        assertThat(r1.getVariable(), is("VAR_1"));
        assertThat(r1.getValue(), nullValue());
        assertThat(r1.getRelevance(), is(Relevance.NOT_SET_AND_RELEVANT));
        assertThat(r1.getFeatureEffect(), is(new Disjunction(new Variable("A=1"), new Variable("C=4"))));
        
        assertThat(result.size(), is(1));
    }
    
    @Test
    public void testUnkownFeatureEffectVariableAndUndefinedResult() throws SetUpException, IOException {
        List<VariableWithFeatureEffect> fes = new LinkedList<>();
        fes.add(new VariableWithFeatureEffect("VAR_1", new Conjunction(new Variable("A=1"), new Variable("C=4"))));
        
        List<VariableRelevance> result = run(fes, new File("testdata/config_relevancy/test.csv"));

        VariableRelevance r1 = result.get(0);
        assertThat(r1.getVariable(), is("VAR_1"));
        assertThat(r1.getValue(), nullValue());
        assertThat(r1.getRelevance(), is(Relevance.UNKOWN));
        assertThat(r1.getFeatureEffect(), is(new Conjunction(new Variable("A=1"), new Variable("C=4"))));
        
        assertThat(result.size(), is(1));
    }
    
    private List<VariableRelevance> run(List<VariableWithFeatureEffect> fes, File inputFile) throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(ConfigRelevancyChecker.INPUT_FILE_PROPERTY);
        config.setValue(ConfigRelevancyChecker.INPUT_FILE_PROPERTY, inputFile);
        
        TestAnalysisComponentProvider<VariableWithFeatureEffect> feProvider = new TestAnalysisComponentProvider<>(fes);
        
        ConfigRelevancyChecker checker = new ConfigRelevancyChecker(config, feProvider);
        List<VariableRelevance> result = new LinkedList<>();
        
        VariableRelevance r;
        while ((r = checker.getNextResult()) != null) {
            result.add(r);
        }
        
        return result;
    }
    
}
