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
package net.ssehub.kernel_haven.fe_analysis.relations;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectComputer;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.fe_analysis.relations.VariableWithPotentialParents.PotentialParent;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.IVoidFormulaVisitor;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * An analysis component that finds potential "parent" variables for variability variables. This is done by checking
 * how often a variable depends on another one.
 * 
 * @author Adam
 */
public class PotentialParentFinder extends AnalysisComponent<VariableWithPotentialParents> {

    private @NonNull AnalysisComponent<VariableWithPcs> pcFinder;
    
    /**
     * Creates a new {@link PotentialParentFinder}.
     * 
     * @param config The pipeline configuration.
     * @param pcFinder The component to get the presence conditions from.
     */
    public PotentialParentFinder(@NonNull Configuration config, @NonNull AnalysisComponent<VariableWithPcs> pcFinder) {
        super(config);
        this.pcFinder = pcFinder;
    }

    @Override
    protected void execute() {
        FeatureEffectComputer computer = new FeatureEffectComputer(true);
        
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        VariableWithPcs varPcs;
        while ((varPcs = pcFinder.getNextResult()) != null) {
            
            VariableWithPotentialParents result = new VariableWithPotentialParents(varPcs.getVariable());
            
            Set<String> seenVariables = new HashSet<>();
            int numPcs = varPcs.getPcs().size();
            for (Formula pc : varPcs.getPcs()) {
                seenVariables.clear();
                seenVariables.add(varPcs.getVariable()); // don't visit self
                
                // create a temporary VariableWithPcs to calculate the FE for a single PC
                Set<Formula> tmpPc = new HashSet<>();
                tmpPc.add(pc);
                VariableWithPcs tmp = new VariableWithPcs(varPcs.getVariable(), tmpPc);
                pc = computer.buildFeatureEffefct(tmp);
                
                pc.accept(new IVoidFormulaVisitor() {
                    
                    @Override
                    public void visitVariable(@NonNull Variable variable) {
                        if (seenVariables.add(variable.getName())) {
                            
                            PotentialParent pp = result.getOrCreatePotentialParent(variable.getName());
                            pp.setProbability(pp.getProbability() + (1.0 / numPcs));
                        }
                    }
                    
                    @Override
                    public void visitTrue(@NonNull True trueConstant) {
                    }
                    
                    @Override
                    public void visitNegation(@NonNull Negation formula) {
                        formula.getFormula().accept(this);
                    }
                    
                    @Override
                    public void visitFalse(@NonNull False falseConstant) {
                    }
                    
                    @Override
                    public void visitDisjunction(@NonNull Disjunction formula) {
                        formula.getLeft().accept(this);
                        formula.getRight().accept(this);
                    }
                    
                    @Override
                    public void visitConjunction(@NonNull Conjunction formula) {
                        formula.getLeft().accept(this);
                        formula.getRight().accept(this);
                    }
                });
            }
            
            result.sort();
            addResult(result);

            progress.processedOne();
        }
        
        progress.close();
    }

    @Override
    public @NonNull String getResultName() {
        return "Potential Parents";
    }

}
