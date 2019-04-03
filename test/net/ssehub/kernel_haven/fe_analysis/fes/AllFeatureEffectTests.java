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
