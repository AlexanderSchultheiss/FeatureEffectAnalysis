package net.ssehub.kernel_haven.fe_analysis.config_relevancy;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.fe_analysis.config_relevancy.VariableRelevance.Relevance;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link ConfigRelevancyChecker}.
 * 
 * @author Adam
 */
@SuppressWarnings("null")
public class ConfigRelevancyCheckerTest {

    /**
     * Tests how the {@link ConfigRelevancyChecker} handles a completely configured configuration.
     * Covers all four cases of {@link Relevance}.
     * 
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
    
    /**
     * Tests whether variables in the feature effect without an equal sign ("=") are treated correctly.
     * 
     * @throws SetUpException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testFeatureEffectVariablesWithoutEquals() throws SetUpException, IOException {
        List<VariableWithFeatureEffect> fes = new LinkedList<>();
        fes.add(new VariableWithFeatureEffect("VAR_1", or("A", "B=1")));
        
        List<VariableRelevance> result = run(fes, new File("testdata/config_relevancy/test.csv"));

        VariableRelevance r1 = result.get(0);
        assertThat(r1.getVariable(), is("VAR_1"));
        assertThat(r1.getValue(), nullValue());
        assertThat(r1.getRelevance(), is(Relevance.NOT_SET_AND_RELEVANT));
        assertThat(r1.getFeatureEffect(), is(or("A", "B=1")));
        
        VariableRelevance r2 = result.get(1);
        assertThat(r2.getVariable(), is("A"));
        assertThat(r2.getValue(), is(1));
        assertThat(r2.getRelevance(), is(Relevance.NOT_FOUND_IN_CODE));
        assertThat(r2.getFeatureEffect(), is(False.INSTANCE));
        
        VariableRelevance r3 = result.get(2);
        assertThat(r3.getVariable(), is("B"));
        assertThat(r3.getValue(), is(2));
        assertThat(r3.getRelevance(), is(Relevance.NOT_FOUND_IN_CODE));
        assertThat(r3.getFeatureEffect(), is(False.INSTANCE));
        
        assertThat(result.size(), is(3));
    }
    
    /**
     * Tests whether a not fully defined configuration can still be evaluated correctly. The result is still a defined
     * value.
     * 
     * @throws SetUpException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testUnkownFeatureEffectVariableButStillDefined() throws SetUpException, IOException {
        List<VariableWithFeatureEffect> fes = new LinkedList<>();
        fes.add(new VariableWithFeatureEffect("VAR_1", or("A=1", "C=4")));
        
        List<VariableRelevance> result = run(fes, new File("testdata/config_relevancy/test.csv"));

        VariableRelevance r1 = result.get(0);
        assertThat(r1.getVariable(), is("VAR_1"));
        assertThat(r1.getValue(), nullValue());
        assertThat(r1.getRelevance(), is(Relevance.NOT_SET_AND_RELEVANT));
        assertThat(r1.getFeatureEffect(), is(or("A=1", "C=4")));

        VariableRelevance r2 = result.get(1);
        assertThat(r2.getVariable(), is("A"));
        assertThat(r2.getValue(), is(1));
        assertThat(r2.getRelevance(), is(Relevance.NOT_FOUND_IN_CODE));
        assertThat(r2.getFeatureEffect(), is(False.INSTANCE));
        
        VariableRelevance r3 = result.get(2);
        assertThat(r3.getVariable(), is("B"));
        assertThat(r3.getValue(), is(2));
        assertThat(r3.getRelevance(), is(Relevance.NOT_FOUND_IN_CODE));
        assertThat(r3.getFeatureEffect(), is(False.INSTANCE));
        
        assertThat(result.size(), is(3));
    }
    
    /**
     * Tests whether a not fully defined configuration can still be evaluated correctly. The result of this is
     * undefined, since too many configuration values are missing.
     * 
     * @throws SetUpException unwanted.
     * @throws IOException unwanted.
     */
    @Test
    public void testUnkownFeatureEffectVariableAndUndefinedResult() throws SetUpException, IOException {
        List<VariableWithFeatureEffect> fes = new LinkedList<>();
        fes.add(new VariableWithFeatureEffect("VAR_1", and("A=1", "C=4")));
        
        List<VariableRelevance> result = run(fes, new File("testdata/config_relevancy/test.csv"));

        VariableRelevance r1 = result.get(0);
        assertThat(r1.getVariable(), is("VAR_1"));
        assertThat(r1.getValue(), nullValue());
        assertThat(r1.getRelevance(), is(Relevance.UNKOWN));
        assertThat(r1.getFeatureEffect(), is(and("A=1", "C=4")));

        VariableRelevance r2 = result.get(1);
        assertThat(r2.getVariable(), is("A"));
        assertThat(r2.getValue(), is(1));
        assertThat(r2.getRelevance(), is(Relevance.NOT_FOUND_IN_CODE));
        assertThat(r2.getFeatureEffect(), is(False.INSTANCE));
        
        VariableRelevance r3 = result.get(2);
        assertThat(r3.getVariable(), is("B"));
        assertThat(r3.getValue(), is(2));
        assertThat(r3.getRelevance(), is(Relevance.NOT_FOUND_IN_CODE));
        assertThat(r3.getFeatureEffect(), is(False.INSTANCE));
        
        assertThat(result.size(), is(3));
    }
    
    /**
     * Runs the {@link ConfigRelevancyChecker} on the given feature effects and input file.
     * 
     * @param fes The list of feature effects to run the checker on.
     * @param inputFile The input file containing the SPL configuration in CSV.
     * 
     * @return The result of the checker run.
     * 
     * @throws SetUpException If creating the checker fails.
     */
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
