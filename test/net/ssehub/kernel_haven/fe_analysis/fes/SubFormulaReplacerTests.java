package net.ssehub.kernel_haven.fe_analysis.fes;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link SubFormulaReplacer}.
 * @author El-Sharkawy
 *
 */
public class SubFormulaReplacerTests {

    /**
     * Test the correct handling of an <tt>or</tt> expression as sub formula. It tests:
     * <ul>
     *   <li>Nested: A</li>
     *   <li>Other: A || B</li>
     *   <li>Expected result: A</li>
     * </ul>
     */
    @Ignore
    @Test
    public void testORExpression() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aORb = new Disjunction(varA, varB);
        
        SubFormulaReplacer replacer = new SubFormulaReplacer(varA);
        Formula result = replacer.minimize(aORb);
        
        Assert.assertEquals(varA, result);
    }
    
    /**
     * Test the correct handling of an <tt>and</tt> expression as sub formula. It tests:
     * <ul>
     *   <li>Nested: A</li>
     *   <li>Other: A && B</li>
     *   <li>Expected result: A</li>
     * </ul>
     */
    @Ignore
    @Test
    public void testANDExpression() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aANDb = new Conjunction(varA, varB);
        
        SubFormulaReplacer replacer = new SubFormulaReplacer(varA);
        Formula result = replacer.minimize(aANDb);
        
        Assert.assertEquals(varA, result);
    }
}
