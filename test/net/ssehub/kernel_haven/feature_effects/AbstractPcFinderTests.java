package net.ssehub.kernel_haven.feature_effects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.feature_effects.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.feature_effects.PresenceConditionAnalysisHelper.SimplificationType;
import net.ssehub.kernel_haven.test_utils.TestAnalysisComponentProvider;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;

/**
 * Common part for the two different kind of {@link PcFinder} tests.
 * @author El-Sharkawy
 *
 */
class AbstractPcFinderTests {
    
    /**
     * Runs the {@link PcFinder} on the passed element and returns the result for testing.
     * @param element A mocked element, which should be analyzed by the {@link PcFinder}.
     * @param simplification The simplification strategy to apply. Anything, except for
     * {@link SimplificationType#NO_SIMPLIFICATION}, works only from ANT. 
     * @return The detected presence conditions.
     */
    protected List<VariableWithPcs> detectPCs(CodeElement element, SimplificationType simplification) {
        // Generate configuration
        TestConfiguration tConfig = null;
        Properties config = new Properties();
        if (null != simplification) {
            config.setProperty(PresenceConditionAnalysisHelper.SIMPLIFIY.getKey(), simplification.name());
        }
        try {
            tConfig = new TestConfiguration(config);
        } catch (SetUpException e) {
            Assert.fail("Could not generate test configuration: " + e.getMessage());
        }
        
        // Create virtual files
        File file1 = new File("file1.c");
        SourceFile sourceFile1 = new SourceFile(file1);
        if (element != null) {
            sourceFile1.addElement(element);
        }
        
        List<VariableWithPcs> results = new ArrayList<>();
        try {
            AnalysisComponent<SourceFile> cmComponent = new TestAnalysisComponentProvider<SourceFile>(sourceFile1);
            PcFinder finder = new PcFinder(tConfig, cmComponent);
            finder.execute();
            VariableWithPcs result;
            do {
                result = finder.getNextResult();
                if (null != result) {
                    results.add(result);
                }
            } while (result != null);
        } catch (SetUpException e) {
            Assert.fail("Setting up the " + PcFinder.class.getSimpleName() + " failed: " + e.getMessage());
        }   

        return results;
    }
}
