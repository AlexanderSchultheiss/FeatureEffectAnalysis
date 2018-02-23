package net.ssehub.kernel_haven.fe_analysis.fes;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link SubFormulaChecker}.
 * @author El-Sharkawy
 *
 */
public class SubFormulaCheckerTests {
    
    /**
     * Test the correct handling of an <tt>or</tt> expression as sub formula. It tests:
     * <ul>
     *   <li>Nested: A</li>
     *   <li>Other: A || B</li>
     *   <li>Expected result: true</li>
     * </ul>
     */
    @Test
    public void testORExpression() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aORb = new Disjunction(varA, varB);
        
        SubFormulaChecker checker = new SubFormulaChecker(varA);
        checker.visit(aORb);
        Assert.assertTrue(checker.isNested());
    }
    
    /**
     * Test the correct handling of an <tt>and</tt> expression as sub formula. It tests:
     * <ul>
     *   <li>Nested: A</li>
     *   <li>Other: A && B</li>
     *   <li>Expected result: false</li>
     * </ul>
     */
    @Test
    public void testANDExpression() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aANDb = new Conjunction(varA, varB);
        
        SubFormulaChecker checker = new SubFormulaChecker(varA);
        checker.visit(aANDb);
        Assert.assertTrue(checker.isNested());
    }

}
