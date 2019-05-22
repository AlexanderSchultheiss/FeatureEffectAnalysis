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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.variability_model.SourceLocation;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;
import net.ssehub.kernel_haven.variability_model.VariabilityModelDescriptor.Attribute;

/**
 * Tests the {@link DummyArchComponentStorageCreator}.
 *
 * @author Adam
 */
public class DummyArchComponentStorageCreatorTest {

    /**
     * Tests a simple case with two variables.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testSimple() throws SetUpException {
        VariabilityVariable var1 = new VariabilityVariable("VAR_A", "bool");
        var1.addLocation(new SourceLocation(new File("folder1/folder2/file.c"), 15));
        
        VariabilityVariable var2 = new VariabilityVariable("VAR_B", "bool");
        var2.addLocation(new SourceLocation(new File("foldera/b/file.c"), 15));
        
        VariabilityModel varModel = new VariabilityModel(new File("."), new HashSet<>(Arrays.asList(var1, var2)));
        varModel.getDescriptor().addAttribute(Attribute.SOURCE_LOCATIONS);
        
        TestConfiguration config = new TestConfiguration(new Properties());
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                DummyArchComponentStorageCreator.class, config,
                new VariabilityModel[] {varModel});
        
        assertThat(result.size(), is(1));
        
        ArchComponentStorage storage = result.get(0);
        
        assertThat(storage.getComponent("VAR_A"), is("folder1/folder2"));
        assertThat(storage.getComponent("VAR_B"), is("foldera/b"));
        assertThat(storage.getNumVariablesWithComponent(), is(2));
    }
    
    /**
     * Tests that different amount of folder nestings are handled correctly.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testFolderDepth() throws SetUpException {
        VariabilityVariable var1 = new VariabilityVariable("VAR_A", "bool");
        var1.addLocation(new SourceLocation(new File("a/b/file.c"), 15));
        
        VariabilityVariable var2 = new VariabilityVariable("VAR_B", "bool");
        var2.addLocation(new SourceLocation(new File("c/file.c"), 15));
        
        VariabilityVariable var3 = new VariabilityVariable("VAR_C", "bool");
        var3.addLocation(new SourceLocation(new File("file.c"), 15));
        
        VariabilityVariable var4 = new VariabilityVariable("VAR_D", "bool");
        var4.addLocation(new SourceLocation(new File("def/ghi/jkl/mno/file.c"), 15));
        
        VariabilityModel varModel = new VariabilityModel(new File("."),
                new HashSet<>(Arrays.asList(var1, var2, var3, var4)));
        varModel.getDescriptor().addAttribute(Attribute.SOURCE_LOCATIONS);
        
        TestConfiguration config = new TestConfiguration(new Properties());
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                DummyArchComponentStorageCreator.class, config,
                new VariabilityModel[] {varModel});
        
        assertThat(result.size(), is(1));
        
        ArchComponentStorage storage = result.get(0);
        
        assertThat(storage.getComponent("VAR_A"), is("a/b"));
        assertThat(storage.getComponent("VAR_B"), is("c"));
        assertThat(storage.getComponent("VAR_C"), is("."));
        assertThat(storage.getComponent("VAR_D"), is("def/ghi"));
        assertThat(storage.getNumVariablesWithComponent(), is(4));
    }
    
    /**
     * Tests that a variable with no source location has no arch component.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testEmptySourceLocation() throws SetUpException {
        VariabilityVariable var1 = new VariabilityVariable("VAR_A", "bool");
        
        VariabilityModel varModel = new VariabilityModel(new File("."), new HashSet<>(Arrays.asList(var1)));
        varModel.getDescriptor().addAttribute(Attribute.SOURCE_LOCATIONS);
        
        TestConfiguration config = new TestConfiguration(new Properties());
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                DummyArchComponentStorageCreator.class, config,
                new VariabilityModel[] {varModel});
        
        assertThat(result.size(), is(1));
        
        ArchComponentStorage storage = result.get(0);
        
        assertThat(storage.getComponent("VAR_A"), is(""));
        assertThat(storage.getNumVariablesWithComponent(), is(0));
    }
    
    /**
     * Tests that for multiple source locations, the first one is used.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testMultipleLocations() throws SetUpException {
        VariabilityVariable var1 = new VariabilityVariable("VAR_A", "bool");
        var1.addLocation(new SourceLocation(new File("a/b/Kconfig"), 50));
        var1.addLocation(new SourceLocation(new File("c/d/Kconfig"), 50));
        
        VariabilityModel varModel = new VariabilityModel(new File("."), new HashSet<>(Arrays.asList(var1)));
        varModel.getDescriptor().addAttribute(Attribute.SOURCE_LOCATIONS);
        
        TestConfiguration config = new TestConfiguration(new Properties());
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                DummyArchComponentStorageCreator.class, config,
                new VariabilityModel[] {varModel});
        
        assertThat(result.size(), is(1));
        
        ArchComponentStorage storage = result.get(0);
        
        assertThat(storage.getComponent("VAR_A"), is("a/b"));
        assertThat(storage.getNumVariablesWithComponent(), is(1));
    }
    
    /**
     * Tests that for multiple source locations, the x86 (default arch setting) one is preferred.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testPreferredArchSetting() throws SetUpException {
        VariabilityVariable var1 = new VariabilityVariable("VAR_A", "bool");
        var1.addLocation(new SourceLocation(new File("arch/arm/Kconfig"), 50));
        var1.addLocation(new SourceLocation(new File("arch/x86/Kconfig"), 50));
        
        VariabilityModel varModel = new VariabilityModel(new File("."), new HashSet<>(Arrays.asList(var1)));
        varModel.getDescriptor().addAttribute(Attribute.SOURCE_LOCATIONS);
        
        TestConfiguration config = new TestConfiguration(new Properties());
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                DummyArchComponentStorageCreator.class, config,
                new VariabilityModel[] {varModel});
        
        assertThat(result.size(), is(1));
        
        ArchComponentStorage storage = result.get(0);
        
        assertThat(storage.getComponent("VAR_A"), is("arch/x86"));
        assertThat(storage.getNumVariablesWithComponent(), is(1));
    }
    
    /**
     * Tests that nothing is returned for a missing variability model.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testMissingVarModel() throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                DummyArchComponentStorageCreator.class, config,
                new VariabilityModel[] {});
        
        assertThat(result.size(), is(0));
    }
    
    /**
     * Tests that nothing is returned when no source locations are annotated.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testNoSourceLocations() throws SetUpException {
        VariabilityModel varModel = new VariabilityModel(new File("."), new HashSet<>());
        // precondition
        assertThat(varModel.getDescriptor().hasAttribute(Attribute.SOURCE_LOCATIONS), is(false));
        
        TestConfiguration config = new TestConfiguration(new Properties());
        
        List<ArchComponentStorage> result = AnalysisComponentExecuter.executeComponent(
                DummyArchComponentStorageCreator.class, config,
                new VariabilityModel[] {varModel});
        
        assertThat(result.size(), is(0));
    }
    
}
