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
    FeatureEffectFinderWithSimplificationTests.class
    })
public class AllFeatureEffectTests {

}
