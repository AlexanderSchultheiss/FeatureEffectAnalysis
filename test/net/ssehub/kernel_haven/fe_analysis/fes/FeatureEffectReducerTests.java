package net.ssehub.kernel_haven.fe_analysis.fes;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link FeatureEffectReducer}.
 * @author El-Sharkawy
 *
 */
public class FeatureEffectReducerTests {
    
    /**
     * Test the correct handling of an <tt>or</tt> expression as sub formula. It tests:
     * <ul>
     *   <li>Input PCs: A; A || B</li>
     *   <li>Expected result: A</li>
     * </ul>
     */
    @Test
    public void testORExpression() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aORb = new Disjunction(varA, varB);
        
        @SuppressWarnings("null")
        Collection<Formula> result = FeatureEffectReducer.simpleReduce(varA.getName(), Arrays.asList(varA, aORb));
        Assert.assertEquals(1, result.size());
        Formula actualResult = result.iterator().next();
        Assert.assertEquals(varA, actualResult);
    }
    
    /**
     * Test the correct handling of an <tt>and</tt> expression as sub formula. It tests:
     * <ul>
     *   <li>Input PCs: A; A && B</li>
     *   <li>Expected result: A</li>
     * </ul>
     */
    @Test
    public void testANDExpression() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Formula aANDb = new Conjunction(varA, varB);
        
        @SuppressWarnings("null")
        Collection<Formula> result = FeatureEffectReducer.simpleReduce(varA.getName(), Arrays.asList(varA, aANDb));
        Assert.assertEquals(1, result.size());
        Formula actualResult = result.iterator().next();
        Assert.assertEquals(varA, actualResult);
    }

}
