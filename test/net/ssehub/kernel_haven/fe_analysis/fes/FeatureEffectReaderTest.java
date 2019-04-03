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
package net.ssehub.kernel_haven.fe_analysis.fes;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.not;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
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
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.True;
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
            new VariableWithFeatureEffect("VAR_A", or("VAR_B", not("VAR_C"))),
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
        
        Formula disjunctonFE = or("VAR_B", not("VAR_C"));
        
        assertThat(effects, is(Arrays.asList(new VariableWithFeatureEffect[] {
            new VariableWithFeatureEffect("VAR_A", disjunctonFE),
            new VariableWithFeatureEffect("VAR_C", True.INSTANCE),
            new VariableWithFeatureEffect("VAR_D", disjunctonFE),
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
            new VariableWithFeatureEffect("VAR_A", or("VAR_B", not("VAR_C"))),
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
