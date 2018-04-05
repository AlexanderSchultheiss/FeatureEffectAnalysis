package net.ssehub.kernel_haven.fe_analysis.pcs;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Tests suite to load tests specific to the computation of feature effect constraints.
 * @author El-Sharkawy
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
    PcFinderTests.class,
    PcReaderTest.class,
    PcFinderTestsWithSimplificationTests.class
    })
public class AllPresenceConditionTests {

}
