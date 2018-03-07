package net.ssehub.kernel_haven.fe_analysis.fes;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Tests the {@link FeatureEffectReader}.
 * 
 * @author Adam
 */
public class FeatureEffectReaderTest {
    
    private static final File TESTDATA = new File("testdata/feature_effects");

    /**
     * Tests the result of a valid input file.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testValidFile() throws SetUpException {
        List<VariableWithFeatureEffect> effects = run(new File(TESTDATA, "valid.csv"));
        
        assertThat(effects, is(Arrays.asList(new VariableWithFeatureEffect[] {
            new VariableWithFeatureEffect("VAR_A",
                    new Disjunction(new Variable("VAR_B"), new Negation(new Variable("VAR_C")))),
            new VariableWithFeatureEffect("VAR_B", False.INSTANCE),
            new VariableWithFeatureEffect("VAR_C", True.INSTANCE)
        })));
    }
    
    /**
     * Tests the result of an input file with some rows having the wrong number of columns.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testWrongNumberColumns() throws SetUpException {
        List<VariableWithFeatureEffect> effects = run(new File(TESTDATA, "wrong_columns.csv"));
        
        assertThat(effects, is(Arrays.asList(new VariableWithFeatureEffect[] {
            new VariableWithFeatureEffect("VAR_A",
                    new Disjunction(new Variable("VAR_B"), new Negation(new Variable("VAR_C")))),
            new VariableWithFeatureEffect("VAR_C", True.INSTANCE),
            new VariableWithFeatureEffect("VAR_E", True.INSTANCE)
        })));
    }
    
    /**
     * Tests the result of an input file with some rows having the wrong number of columns.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testInvalidFormula() throws SetUpException {
        List<VariableWithFeatureEffect> effects = run(new File(TESTDATA, "invalid_formula.csv"));
        
        assertThat(effects, is(Arrays.asList(new VariableWithFeatureEffect[] {
            new VariableWithFeatureEffect("VAR_A",
                    new Disjunction(new Variable("VAR_B"), new Negation(new Variable("VAR_C")))),
            new VariableWithFeatureEffect("VAR_C", True.INSTANCE),
            new VariableWithFeatureEffect("VAR_E", True.INSTANCE)
        })));
    }
    
    /**
     * Runs the {@link FeatureEffectReader} on the given input file.
     * 
     * @param file The input file for the {@link FeatureEffectReader}.
     * 
     * @return The list with the results.
     * 
     * @throws SetUpException If creating the {@link FeatureEffectReader} fails.
     */
    private @NonNull List<@NonNull VariableWithFeatureEffect> run(@NonNull File file) throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(FeatureEffectReader.INPUT_FILE_SETTING);
        config.setValue(FeatureEffectReader.INPUT_FILE_SETTING, file);
        
        FeatureEffectReader reader = new FeatureEffectReader(config);
        
        List<@NonNull VariableWithFeatureEffect> result = new LinkedList<>();
        
        VariableWithFeatureEffect read;
        while ((read = reader.getNextResult()) != null) {
            result.add(read);
        }
        
        return result;
    }
    
}
