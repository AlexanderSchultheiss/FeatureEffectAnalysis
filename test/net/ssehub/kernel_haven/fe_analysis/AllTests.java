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
    ConfigRelevancyCheckerTest.class,
    AllFeatureEffectTests.class,
    AllPresenceConditionTests.class,
    AllRelationsTests.class,
    
    PresenceConditionAnalysisHelperTest.class,
    StringUtilsTests.class,
    })
public class AllTests {

}
