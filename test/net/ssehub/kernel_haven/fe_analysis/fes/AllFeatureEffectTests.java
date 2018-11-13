package net.ssehub.kernel_haven.fe_analysis.fes;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Tests suite to load tests specific to the detection of presence conditions.
 * @author El-Sharkawy
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
    FeatureEffectFinderTests.class,
    FeatureEffectReaderTest.class,
    ThreadedFeatureEffectFinderTest.class,
    NonBooleanFeExpanderTest.class,
    
    // Sub formula elimination tests
    SubFormulaCheckerTests.class,
    SubFormulaReplacerTests.class,
    FeatureEffectReducerTests.class,
    
    // Simplification & FEAggregation Tests
    FeatureEffectFinderWithSimplificationTests.class,
    FeAggregatorTest.class,
    FeAggregatorWithSimplificationTest.class,
    
    // Dependency Graph
    FeatureRelationsTests.class
    })
public class AllFeatureEffectTests {

}
