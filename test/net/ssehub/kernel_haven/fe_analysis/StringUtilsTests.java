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
