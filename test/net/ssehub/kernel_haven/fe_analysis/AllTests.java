package net.ssehub.kernel_haven.fe_analysis;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.ssehub.kernel_haven.fe_analysis.arch_components.AllArchComponentTests;
import net.ssehub.kernel_haven.fe_analysis.config_relevancy.ConfigRelevancyCheckerTest;
import net.ssehub.kernel_haven.fe_analysis.fes.AllFeatureEffectTests;
import net.ssehub.kernel_haven.fe_analysis.pcs.AllPresenceConditionTests;
import net.ssehub.kernel_haven.fe_analysis.relations.AllRelationsTests;

/**
 * Tests suite to load all tests of this plug-in (entry point).
 * @author El-Sharkawy
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
    AllArchComponentTests.class,
    AllPresenceConditionTests.class,
    AllFeatureEffectTests.class,
    AllRelationsTests.class,
    
    StringUtilsTests.class,
    ConfigRelevancyCheckerTest.class,
    })
public class AllTests {

}
