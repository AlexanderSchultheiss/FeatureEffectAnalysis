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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link StringUtils}.
 * @author El-Sharkawy
 *
 */
public class StringUtilsTests {
    
    /**
     * Tests that the last operator position is identified correctly.
     */
    @Test
    @SuppressWarnings("null")
    public void testFindLastOperator() {
        String varName = "Something";
        String[] validSamples = {varName + "=123", varName + ">123", varName + "<123", varName + "!=123"
                , varName + ">=123", varName + "<=123"};
        
        for (int i = 0; i < validSamples.length; i++) {
            int index = StringUtils.getLastOperatorIndex(validSamples[i]);
            if (-1 != index) {
                String extracteVar = validSamples[i].substring(0, index);
                Assert.assertEquals("Could not detect operator in " + validSamples[i], varName, extracteVar);
            } else {
                Assert.fail("Could not detect operator in " + validSamples[i]);
            }
        }
    }

}
