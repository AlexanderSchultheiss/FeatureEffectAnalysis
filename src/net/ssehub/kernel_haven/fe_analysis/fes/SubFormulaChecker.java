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

import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A {@link net.ssehub.kernel_haven.util.logic.parser.SubFormulaChecker}, which does <b>not</b> consider negated
 * formulas.
 * @author El-Sharkawy
 *
 */
public class SubFormulaChecker extends net.ssehub.kernel_haven.util.logic.parser.SubFormulaChecker {

    private boolean isNegated;
    
    /**
     * Sole constructor for this visitor.
     * The accept method must still be called.
     * @param nestedFormula The formula to check if it is nested inside the visited formula.
     */
    public SubFormulaChecker(@NonNull Formula nestedFormula) {
        super(nestedFormula);
        isNegated = false;
    }
    
    @Override
    public void visitNegation(@NonNull Negation formula) {
        setNested(formula.equals(getNestedFormula()));
        
        if (!isNested()) {
            boolean oldStatus = isNegated;
            
            // Change the status only fur current part of formula
            isNegated = !isNegated;
            visit(formula.getFormula());
            
            isNegated = oldStatus;
        }
    }
    
    @Override
    public boolean isNested() {
        return super.isNested() && !isNegated;
    }

}
