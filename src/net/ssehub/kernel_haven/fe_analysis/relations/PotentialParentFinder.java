package net.ssehub.kernel_haven.fe_analysis.relations;

import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.fe_analysis.relations.VariableWithPotentialParents.PotentialParent;
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
        VariableWithPcs varPcs;
        while ((varPcs = pcFinder.getNextResult()) != null) {
            
            VariableWithPotentialParents result = new VariableWithPotentialParents(varPcs.getVariable());
            
            Set<String> seenVariables = new HashSet<>();
            int numPcs = varPcs.getPcs().size();
            for (Formula pc : varPcs.getPcs()) {
                seenVariables.clear();
                seenVariables.add(varPcs.getVariable()); // don't visit self
                
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
            
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Potential Parents";
    }

}
