package net.ssehub.kernel_haven.fe_analysis.relations;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.List;

import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Stores a variable and it's potential parents.
 * 
 * @author Adam
 */
@TableRow
public class VariableWithPotentialParents {

    /**
     * Stores a parent variable and the probability that this is a parent.
     */
    public static class PotentialParent {
        
        private @NonNull String variable;
        
        private double probability;
        
        /**
         * Creates a new potential parent with the probability of 0%.
         * 
         * @param variable The potential parent variable name.
         */
        public PotentialParent(@NonNull String variable) {
            this.variable = variable;
        }
        
        /**
         * Returns the name of this potential parent.
         * 
         * @return The variable name of this.
         */
        public String getVariable() {
            return variable;
        }
        
        /**
         * Returns the probability that this a potential parent.
         * 
         * @return The probability that this is a potential parent.
         */
        public double getProbability() {
            return probability;
        }
        
        /**
         * Sets the probability that this a potential parent.
         * 
         * @param probability The probability that this a potential parent.
         */
        public void setProbability(double probability) {
            this.probability = probability;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%.2f%%)", variable, probability * 100);
        }
        
    }
    
    private @NonNull String variable;
    
    private @NonNull List<@NonNull PotentialParent> potentialParents;
    
    /**
     * Creates a new and empty {@link PotentialParent} container for the given variable.
     * 
     * @param variable The variable for which the potential parents are stored.
     */
    public VariableWithPotentialParents(@NonNull String variable) {
        this.variable = variable;
        this.potentialParents = new ArrayList<>();
    }
    
    /**
     * Returns the variable that the potential parents are stored for.
     * 
     * @return The variable.
     */
    @TableElement(index = 1, name = "Variable")
    public String getVariable() {
        return variable;
    }
    
    /**
     * Returns the potential parents list as a string.
     * 
     * @return The potential parents.
     */
    @TableElement(index = 2, name = "Potential Parents")
    public @NonNull String getPotentialParentsString() {
        return notNull(potentialParents.toString());
    }
    
    /**
     * Adds a potential parent to this container.
     * 
     * @param parent The new potential parent.
     */
    public void addPotentialParent(@NonNull PotentialParent parent) {
        this.potentialParents.add(parent);
    }
    
    /**
     * Sorts the list of potential parents with descending probability. This should be called after all
     * {@link PotentialParent}s are added and their probabilities are set.
     */
    public void sort() {
        this.potentialParents.sort((pp1, pp2) -> Double.compare(pp2.probability, pp1.probability));
    }
    
    /**
     * Retrieves the {@link PotentialParent} with the given name.
     * 
     * @param name The name of the {@link PotentialParent} to retrieve.
     * 
     * @return The {@link PotentialParent}, or <code>null</code> if none with this name exists.
     */
    @SuppressWarnings("null")
    public @Nullable PotentialParent getPotentialParent(@NonNull String name) {
        return this.potentialParents.stream().filter(pp -> pp.variable.equals(name)).findFirst().orElse(null);
    }
    
    /**
     * Retrieves or creates the {@link PotentialParent} with the given name.
     * 
     * @param name The name of the {@link PotentialParent} to retrieve or create.
     * 
     * @return The {@link PotentialParent}.
     */
    public @NonNull PotentialParent getOrCreatePotentialParent(@NonNull String name) {
        PotentialParent parent = getPotentialParent(name);
        if (parent == null) {
            parent = new PotentialParent(name);
            addPotentialParent(parent);
        }
        return parent;
    }
    
    @Override
    public String toString() {
        return "PotentialParents for " + variable + ": " + getPotentialParentsString();
    }
    
}
