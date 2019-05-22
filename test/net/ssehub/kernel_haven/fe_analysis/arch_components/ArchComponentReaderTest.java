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
package net.ssehub.kernel_haven.fe_analysis.arch_components;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Tests the {@link ArchComponentReader}.
 *
 * @author Adam
 */
public class ArchComponentReaderTest {

    private static final @NonNull File TESTDATA = new File("testdata/arch_components");
    
    /**
     * Tests a simple case with 3 variables.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testSimple() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(ArchComponentReader.INPUT_SETTING);
        config.setValue(ArchComponentReader.INPUT_SETTING, new File(TESTDATA, "simple.csv"));
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                ArchComponentReader.class, config);
        assertThat(result.size(), is(1));
        
        ArchComponentStorage storage = result.get(0);
        
        assertThat(storage.getComponent("A"), is("comp1"));
        assertThat(storage.getComponent("B"), is("comp1"));
        assertThat(storage.getComponent("C"), is("comp2"));
        assertThat(storage.getNumVariablesWithComponent(), is(3));
    }
    
    /**
     * Tests that nothing is returned when the "Architecture Component" column can not be found.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testMissingColumn() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(ArchComponentReader.INPUT_SETTING);
        config.setValue(ArchComponentReader.INPUT_SETTING, new File(TESTDATA, "renamed_column.csv"));
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                ArchComponentReader.class, config);
        assertThat(result.size(), is(0));
    }
    
    /**
     * Tests that nothing is returned when no single row is found.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testEmptyFile() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(ArchComponentReader.INPUT_SETTING);
        config.setValue(ArchComponentReader.INPUT_SETTING, new File(TESTDATA, "empty.csv"));
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                ArchComponentReader.class, config);
        assertThat(result.size(), is(0));
    }
    
    /**
     * An {@link ArchComponentReader} with a renamed column name.
     */
    public static class RenamedColumnReader extends ArchComponentReader {

        /**
         * Creates this.
         * 
         * @param config The config.
         * 
         * @throws SetUpException if creation fails.
         */
        public RenamedColumnReader(@NonNull Configuration config) throws SetUpException {
            super(config);
        }
        
        @Override
        protected @NonNull String getArchComponentHeader() {
            return "Component";
        }
        
    }
    
    /**
     * Tests that a renamed column is found, too.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testRenamedColumn() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(ArchComponentReader.INPUT_SETTING);
        config.setValue(ArchComponentReader.INPUT_SETTING, new File(TESTDATA, "renamed_column.csv"));
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                RenamedColumnReader.class, config);
        assertThat(result.size(), is(1));
        
        ArchComponentStorage storage = result.get(0);
        
        assertThat(storage.getComponent("A"), is("comp1"));
        assertThat(storage.getComponent("B"), is("comp1"));
        assertThat(storage.getComponent("C"), is("comp2"));
        assertThat(storage.getNumVariablesWithComponent(), is(3));
    }
    
    /**
     * An {@link ArchComponentReader} which checks for valid component names.
     */
    public static class ComponentCheckingReader extends ArchComponentReader {

        /**
         * Creates this.
         * 
         * @param config The config.
         * 
         * @throws SetUpException if creation fails.
         */
        public ComponentCheckingReader(@NonNull Configuration config) throws SetUpException {
            super(config);
        }
        
        @Override
        protected boolean isValidComponent(@NonNull String component) {
            return component.startsWith("comp");
        }
        
    }
    
    /**
     * Tests that invalid components are ignored.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testInvalidComponents() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(ArchComponentReader.INPUT_SETTING);
        config.setValue(ArchComponentReader.INPUT_SETTING, new File(TESTDATA, "invalid_component.csv"));
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                ComponentCheckingReader.class, config);
        assertThat(result.size(), is(1));
        
        ArchComponentStorage storage = result.get(0);
        
        assertThat(storage.getComponent("A"), is("comp1"));
        assertThat(storage.getComponent("C"), is("comp2"));
        assertThat(storage.getNumVariablesWithComponent(), is(2));
    }
    
}
