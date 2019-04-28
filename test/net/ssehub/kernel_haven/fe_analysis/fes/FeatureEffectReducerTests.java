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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Ignore;
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
     *   <li>Input PCs: {@code A; A || B}</li>
     *   <li>Expected result: {@code A}</li>
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
     *   <li>Input PCs: {@code A; A && B}</li>
     *   <li>Expected result: {@code A}</li>
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
    
    /**
     * Test the correct handling of an <tt>and</tt> expression as sub formula. It tests:
     * <ul>
     *   <li>Input PCs: {@code A && B, A && B && C}</li>
     *   <li>Expected result: {@code A && B}</li>
     * </ul>
     */
    @Test
    public void testANDContainmentExpression() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Variable varC = new Variable("C");
        Formula aANDb = new Conjunction(varA, varB);
        Formula allThree = new Conjunction(aANDb, varC);
        
        @SuppressWarnings("null")
        Collection<Formula> result = FeatureEffectReducer.simpleReduce(varA.getName(), Arrays.asList(allThree, aANDb));
        Assert.assertEquals(1, result.size());
        Formula actualResult = result.iterator().next();
        Assert.assertEquals(aANDb, actualResult);
    }
    
    /**
     * Test the correct handling of an <tt>and</tt> expression as sub formula. It tests:
     * <ul>
     *   <li>Input PCs: {@code A || (B && C), (A || (B && C) || D}</li>
     *   <li>Expected result: Same as input</li>
     * </ul>
     */
    @Ignore
    @Test
    public void testComplexSubFormula() {
        Variable varA = new Variable("A");
        Variable varB = new Variable("B");
        Variable varC = new Variable("C");
        Variable varD = new Variable("D");
        Formula bANDc = new Conjunction(varB, varC);
        Formula first3 = new Disjunction(varA, bANDc);
        Formula all4 = new Disjunction(first3, varD);
        
        @SuppressWarnings("null")
        Collection<Formula> result = FeatureEffectReducer.simpleReduce(varA.getName(), Arrays.asList(first3, all4));
        Assert.assertEquals(2, result.size());
    }

}
