package net.ssehub.kernel_haven.fe_analysis.relations;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.relations.PotentialParentRelationFinder.PotentialParentRelation;
import net.ssehub.kernel_haven.fe_analysis.relations.VariableWithPotentialParents.PotentialParent;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Analysis component to convert the results of the {@link PotentialParentFinder} into a relational data base scheme.
 * @author El-Sharkawy
 *
 */
public class PotentialParentRelationFinder extends AnalysisComponent<PotentialParentRelation> {

    /**
     * Data object to store a single {@link PotentialParent} as Strings and manageable by a relation DB.
     * @author El-Sharkawy
     *
     */
    @TableRow(isRelation = true)
    public static class PotentialParentRelation {
        
        private String feature;
        
        private String parent;
        
        private double probability;

        /**
         * Creates this object.
         * 
         * @param feature Value 1.
         * @param parent Value 2.
         * @param probability Value 3.
         */
        public PotentialParentRelation(String feature, String parent, double probability) {
            this.feature = feature;
            this.parent = parent;
            this.probability = probability;
        }

        /**
         * The (dependent) feature variable.
         * 
         * @return The (dependent) feature variable.
         */
        @TableElement(index = 1, name = "Feature")
        public String getFeature() {
            return feature;
        }
        
        /**
         * A feature variable from which the first feature depends on.
         * 
         * @return A feature variable from which the first feature depends on.
         */
        @TableElement(index = 2, name = "Parent")
        public String getParent() {
            return parent;
        }
        
        /**
         * Returns the probability that this parent-child relation is correct.
         * 
         * @return The probability.
         */
        @TableElement(index = 3, name = "Probability")
        public double getProbability() {
            return probability;
        }
        
    }

    private @NonNull AnalysisComponent<VariableWithPotentialParents> parentFinder;
    
    /**
     * Sole constructor.
     * @param config The pipeline configuration
     * @param parentFinder The actual analysis computing the parent probabilities.
     */
    public PotentialParentRelationFinder(@NonNull Configuration config,
        @NonNull AnalysisComponent<VariableWithPotentialParents> parentFinder) {
        
        super(config);
        this.parentFinder = parentFinder;
    }


    @Override
    protected void execute() {
        VariableWithPotentialParents var;
        while ((var = parentFinder.getNextResult()) != null) {
            for (PotentialParent pp : var) {
                addResult(new PotentialParentRelation(var.getVariable(), pp.getVariable(), pp.getProbability()));
            }
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Potential Parent Relations";
    }
}
