package net.ssehub.kernel_haven.fe_analysis;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.ssehub.kernel_haven.fe_analysis.fes.AllFeatureEffectTests;
import net.ssehub.kernel_haven.fe_analysis.pcs.AllPresenceConditionTests;

/**
 * Tests suite to load all tests of this plug-in (entry point).
 * @author El-Sharkawy
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
    AllPresenceConditionTests.class,
    AllFeatureEffectTests.class,
    StringUtilsTests.class
    })
public class AllTests {

}
