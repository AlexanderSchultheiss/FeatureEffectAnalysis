/*
 * Copyright 2019 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.kernel_haven.fe_analysis;

import static net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType.FEATURE_EFFECTS;
import static net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType.NO_SIMPLIFICATION;
import static net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType.PRESENCE_CONDITIONS;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.not;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * Tests the {@link PresenceConditionAnalysisHelper}.
 *
 * @author Adam
 */
public class PresenceConditionAnalysisHelperTest {
    
    /**
     * An {@link PresenceConditionAnalysisHelper} that can supply a custom {@link VariabilityModel} for test cases.
     */
    public static class PresenceConditionAnalysisHelperForTests extends PresenceConditionAnalysisHelper {

        private static @Nullable VariabilityModel varModel;
        
        /**
         * Creates this instance.
         * 
         * @param config The configuration.
         * 
         * @throws SetUpException If instantiating fails.
         */
        public PresenceConditionAnalysisHelperForTests(@NonNull Configuration config) throws SetUpException {
            super(config);
        }
        
        @Override
        protected @Nullable VariabilityModel getVariabilityModel() {
            return varModel;
        }
        
    }

    /**
     * Creates a {@link PresenceConditionAnalysisHelper} with the given settings.
     * 
     * @param relevantVariablesPattern The relevant variables setting.
     * @param considerVmVarsOnly The consider VM variables only setting.
     * @param simplificationType The simplification setting.
     * @param nonBooeanPreperationClassPresent Whether a non-boolean preparation is present.
     * @param fuzzyParsing The fuzzy parsing setting.
     * @param varModel The {@link VariabilityModel} to use.
     * 
     * @return The {@link PresenceConditionAnalysisHelper} with the given configuration.
     * 
     * @throws SetUpException If creating the component fails.
     */
    // CHECKSTYLE:OFF // too many argument
    private PresenceConditionAnalysisHelper create(String relevantVariablesPattern, boolean considerVmVarsOnly,
            SimplificationType simplificationType, boolean nonBooeanPreperationClassPresent, boolean fuzzyParsing,
            VariabilityModel varModel)
            throws SetUpException {
     // CHECKSTYLE:ON

        Properties props = new Properties();
        props.put(Settings.RELEVANT_VARIABLES.getKey(), relevantVariablesPattern);
        props.put(DefaultSettings.ANALYSIS_USE_VARMODEL_VARIABLES_ONLY.getKey(), Boolean.toString(considerVmVarsOnly));
        props.put(Settings.SIMPLIFIY.getKey(), simplificationType.name());
        if (nonBooeanPreperationClassPresent) {
            props.put(DefaultSettings.PREPARATION_CLASSES.getKey() + ".0", "some.NonBooleanPreperation");
        }
        props.put(DefaultSettings.FUZZY_PARSING.getKey(), Boolean.toString(fuzzyParsing));
        
        TestConfiguration config = new TestConfiguration(props);
        
        PresenceConditionAnalysisHelperForTests.varModel = varModel;
        return new PresenceConditionAnalysisHelperForTests(config);
    }
    
    /**
     * Tests that the helper correctly determines when to revert non boolean replacements.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testNonBooleanReplacementsDetection() throws SetUpException {
        // relevantVarsPatter, considerVmVarsOnly, simplification, preparation, fuzzyParsing, varModel
        assertThat(create("", false, NO_SIMPLIFICATION, false, false, null).isNonBooleanReplacements(), is(false));
        assertThat(create("", false, NO_SIMPLIFICATION, true, false, null).isNonBooleanReplacements(), is(true));
        assertThat(create("", false, NO_SIMPLIFICATION, false, true, null).isNonBooleanReplacements(), is(true));
        assertThat(create("", false, NO_SIMPLIFICATION, true, true, null).isNonBooleanReplacements(), is(true));
    }
    
    /**
     * Tests that the helper correctly determines when a non-boolean replacement was done.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testNonBooleanModeDetection() throws SetUpException {
        // relevantVarsPatter, considerVmVarsOnly, simplification, preparation, fuzzyParsing, varModel
        assertThat(create("", false, NO_SIMPLIFICATION, false, false, null).isNonBooleanMode(), is(false));
        assertThat(create("", false, NO_SIMPLIFICATION, true, false, null).isNonBooleanMode(), is(true));
        assertThat(create("", false, NO_SIMPLIFICATION, false, true, null).isNonBooleanMode(), is(false));
        assertThat(create("", false, NO_SIMPLIFICATION, true, true, null).isNonBooleanMode(), is(true));
    }
    
    /**
     * Tests that the simplification mode is correctly detected.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testSimplificationDetection() throws SetUpException {
        // relevantVarsPatter, considerVmVarsOnly, simplification, preparation, fuzzyParsing, varModel
        assertThat(create("", false, NO_SIMPLIFICATION, false, false, null).getSimplificationMode(),
                is(NO_SIMPLIFICATION));
        assertThat(create("", false, PRESENCE_CONDITIONS, false, false, null).getSimplificationMode(),
                is(PRESENCE_CONDITIONS));
        assertThat(create("", false, FEATURE_EFFECTS, false, false, null).getSimplificationMode(),
                is(FEATURE_EFFECTS));
    }
    
    /**
     * Tests that an exception is thrown if no variability model is supplied but needed.
     * 
     * @throws SetUpException wanted.
     */
    @Test(expected = SetUpException.class)
    public void testMissingVarModel() throws SetUpException {
        // relevantVarsPatter, considerVmVarsOnly, simplification, preparation, fuzzyParsing, varModel
        create("", true, NO_SIMPLIFICATION, false, false, null);
    }
    
    /**
     * Tests the {@link PresenceConditionAnalysisHelper#findVars} method.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testFindVars() throws SetUpException {
        PresenceConditionAnalysisHelper helper = create("", false, NO_SIMPLIFICATION, false, false, null);
        
        Set<@NonNull Variable> result = new HashSet<>();
        helper.findVars(or(and(not("A"), "B"), or(not("A"), and(True.INSTANCE, False.INSTANCE))), result);
        
        assertThat(result, is(new HashSet<>(Arrays.asList(new Variable("A"), new Variable("B")))));
    }
    
    /**
     * Tests the {@link PresenceConditionAnalysisHelper#isRelevant(String)} method.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testVariableNameIsRelevant() throws SetUpException {
        // relevantVarsPatter, considerVmVarsOnly, simplification, preparation, fuzzyParsing, varModel
        
        // allows any variable names
        PresenceConditionAnalysisHelper helper = create(".*", false, NO_SIMPLIFICATION, false, false, null);
        assertThat(helper.isRelevant("A"), is(true));
        assertThat(helper.isRelevant("B"), is(true));
        assertThat(helper.isRelevant("C_eq_1"), is(true));
        
        // everything that starts with CONFIG_
        helper = create("CONFIG_.+", false, NO_SIMPLIFICATION, false, false, null);
        assertThat(helper.isRelevant("CONFIG_A"), is(true));
        assertThat(helper.isRelevant("B"), is(false));
        assertThat(helper.isRelevant("CONFIG_C_eq_1"), is(true));
        
        // variability model only, no non-boolean stuff
        VariabilityModel varModel = new VariabilityModel(new File("."), new HashSet<>(Arrays.asList(
            new VariabilityVariable("A", "bool"),
            new VariabilityVariable("C", "bool")
        )));
        helper = create("", true, NO_SIMPLIFICATION, false, false, varModel);
        assertThat(helper.isRelevant("A"), is(true));
        assertThat(helper.isRelevant("B"), is(false));
        assertThat(helper.isRelevant("C_eq_1"), is(false));
        
        // variability model only, non-boolean allowed
        helper = create("", true, NO_SIMPLIFICATION, false, true, varModel);
        assertThat(helper.isRelevant("A"), is(true));
        assertThat(helper.isRelevant("B"), is(false));
        assertThat(helper.isRelevant("C_eq_1"), is(true));
    }
    
    /**
     * Tests the {@link PresenceConditionAnalysisHelper#isRelevant(Formula)} method.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testFormulaIsRelevant() throws SetUpException {
        // relevantVarsPatter, considerVmVarsOnly, simplification, preparation, fuzzyParsing, varModel
        
        // allows any variable names
        PresenceConditionAnalysisHelper helper = create(".*", false, NO_SIMPLIFICATION, false, false, null);
        assertThat(helper.isRelevant(or("A", "B")), is(true));
        assertThat(helper.isRelevant(or("C", "D")), is(true));
        assertThat(helper.isRelevant(or("A", "C")), is(true));
        
        // everything that starts with CONFIG_
        helper = create("CONFIG_.+", false, NO_SIMPLIFICATION, false, false, null);
        assertThat(helper.isRelevant(or("CONFIG_A", "CONFIG_B")), is(true));
        assertThat(helper.isRelevant(or("C", "D")), is(false));
        assertThat(helper.isRelevant(or("CONFIG_A", "C")), is(true));
        
        // variability model only
        VariabilityModel varModel = new VariabilityModel(new File("."), new HashSet<>(Arrays.asList(
            new VariabilityVariable("A", "bool"),
            new VariabilityVariable("B", "bool")
        )));
        helper = create("", true, NO_SIMPLIFICATION, false, false, varModel);
        assertThat(helper.isRelevant(or("A", "B")), is(true));
        assertThat(helper.isRelevant(or("C", "D")), is(false));
        assertThat(helper.isRelevant(or("A", "C")), is(true));
    }
    
    /**
     * Tests the {@link PresenceConditionAnalysisHelper#doReplacements(String)} method.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testDoStringReplacements() throws SetUpException {
        // relevantVarsPatter, considerVmVarsOnly, simplification, preparation, fuzzyParsing, varModel
        
        // does no replacements, because non-boolean mode is deactivated
        PresenceConditionAnalysisHelper helper = create(".*", false, NO_SIMPLIFICATION, false, false, null);
        assertThat(helper.doReplacements("A"), is("A"));
        assertThat(helper.doReplacements("A_eq_1"), is("A_eq_1"));
        assertThat(helper.doReplacements("A_ne_1"), is("A_ne_1"));
        assertThat(helper.doReplacements("A_gt_1"), is("A_gt_1"));
        assertThat(helper.doReplacements("A_ge_1"), is("A_ge_1"));
        assertThat(helper.doReplacements("A_lt_1"), is("A_lt_1"));
        assertThat(helper.doReplacements("A_le_1"), is("A_le_1"));
        
        // does replacements, because non-boolean mode is activated
        helper = create(".*", false, NO_SIMPLIFICATION, false, true, null);
        assertThat(helper.doReplacements("A"), is("A"));
        assertThat(helper.doReplacements("A_eq_1"), is("A=1"));
        assertThat(helper.doReplacements("A_ne_1"), is("A!=1"));
        assertThat(helper.doReplacements("A_gt_1"), is("A>1"));
        assertThat(helper.doReplacements("A_ge_1"), is("A>=1"));
        assertThat(helper.doReplacements("A_lt_1"), is("A<1"));
        assertThat(helper.doReplacements("A_le_1"), is("A<=1"));
    }
    
    /**
     * Tests the {@link PresenceConditionAnalysisHelper#doReplacements(Formula)} method.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testDoFormulaReplacements() throws SetUpException {
        // relevantVarsPatter, considerVmVarsOnly, simplification, preparation, fuzzyParsing, varModel
        
        // does no replacements, because non-boolean mode is deactivated
        PresenceConditionAnalysisHelper helper = create(".*", false, NO_SIMPLIFICATION, false, false, null);
        assertThat(helper.doReplacements(or("A", "B")), is(or("A", "B")));
        assertThat(helper.doReplacements(and("A_eq_1", "B")), is(and("A_eq_1", "B")));
        assertThat(helper.doReplacements(or("A_eq_1", "B_eq_2")), is(or("A_eq_1", "B_eq_2")));
        assertThat(helper.doReplacements(and(or("A_eq_1", False.INSTANCE), not(and("B_eq_2", True.INSTANCE)))),
                is(and(or("A_eq_1", False.INSTANCE), not(and("B_eq_2", True.INSTANCE)))));
        
        // does replacements, because non-boolean mode is activated
        helper = create(".*", false, NO_SIMPLIFICATION, false, true, null);
        assertThat(helper.doReplacements(or("A", "B")), is(or("A", "B")));
        assertThat(helper.doReplacements(and("A_eq_1", "B")), is(and("A=1", "B")));
        assertThat(helper.doReplacements(or("A_eq_1", "B_eq_2")), is(or("A=1", "B=2")));
        assertThat(helper.doReplacements(and(or("A_eq_1", False.INSTANCE), not(and("B_eq_2", True.INSTANCE)))),
                is(and(or("A=1", False.INSTANCE), not(and("B=2", True.INSTANCE)))));
    }
    
    /**
     * Tests the {@link PresenceConditionAnalysisHelper#removeReplacements(String)} method.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testRemoveStringReplacements() throws SetUpException {
        // relevantVarsPatter, considerVmVarsOnly, simplification, preparation, fuzzyParsing, varModel
        
        PresenceConditionAnalysisHelper helper = create(".*", false, NO_SIMPLIFICATION, false, false, null);
        assertThat(helper.removeReplacements("A"), is("A"));
        assertThat(helper.removeReplacements("A_eq_1"), is("A"));
        assertThat(helper.removeReplacements("A_ne_1"), is("A"));
        assertThat(helper.removeReplacements("A_gt_1"), is("A"));
        assertThat(helper.removeReplacements("A_ge_1"), is("A"));
        assertThat(helper.removeReplacements("A_lt_1"), is("A"));
        assertThat(helper.removeReplacements("A_le_1"), is("A"));
    }
    
    /**
     * Tests the {@link PresenceConditionAnalysisHelper#removeReplacements(Formula)} method.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testRemoveFormulaReplacements() throws SetUpException {
        // relevantVarsPatter, considerVmVarsOnly, simplification, preparation, fuzzyParsing, varModel
        
        PresenceConditionAnalysisHelper helper = create(".*", false, NO_SIMPLIFICATION, false, false, null);
        assertThat(helper.removeReplacements(or("A", "B")), is(or("A", "B")));
        assertThat(helper.removeReplacements(and("A_eq_1", "B")), is(and("A", "B")));
        assertThat(helper.removeReplacements(or("A_eq_1", "B_eq_2")), is(or("A", "B")));
        assertThat(helper.removeReplacements(and(or("A_eq_1", False.INSTANCE), not(and("B_eq_2", True.INSTANCE)))),
                is(and(or("A", False.INSTANCE), not(and("B", True.INSTANCE)))));
    }
    
}
