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
     *   <li>Nested: {@code A}</li>
     *   <li>Other: {@code A || B}</li>
     *   <li>Expected result: {@code A}</li>
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
     *   <li>Nested: {@code A}</li>
     *   <li>Other: {@code A && B}</li>
     *   <li>Expected result: {@code A}</li>
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
