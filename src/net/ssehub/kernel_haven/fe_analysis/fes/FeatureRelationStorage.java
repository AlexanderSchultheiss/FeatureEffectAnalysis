package net.ssehub.kernel_haven.fe_analysis.fes;

import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Stores whether a relation between two features was already processed (assumes sorted list of input features).
 * @author El-Sharkawy
 *
 */
public class FeatureRelationStorage {

    /**
     * Stores a features and all of its processed dependensOn features.
     * @author El-Sharkawy
     *
     */
    private static class StorageElement {
        private String name;
        private Set<String> relations;
        
        /**
         * Sole constructor.
         * @param name The name of the feature.
         */
        private StorageElement(String name) {
            this.name = name;
            relations = new HashSet<>();
        }
        
    }
    
    /**
     * The storage to use.
     * @author El-Sharkawy
     *
     */
    private static class Storage extends AbstractFeatureStorage<StorageElement> {
    
        @Override
        protected String getVariableName(StorageElement variable) {
            return variable.name;
        }
    }
    
    private Storage storage = new Storage();

    /**
     * Checks whether the given feature dependency was already processed.
     * @param feature The feature (dependent variable)
     * @param dependensOn Another feature (the dependensOn variable).
     * @return <tt>true</tt> The relation was not processed so far, <tt>false</tt> the relation was already processed.
     */
    public boolean elementNotProcessed(@NonNull String feature, String dependensOn) {
        // 1st check: If feature was already processed
        StorageElement element = storage.getBaseVariable(feature);
        if (null == element) {
            element = new StorageElement(feature);
            storage.add(element);
        }
        
        // 2nd check: If relation was already processed
        boolean processed = element.relations.contains(dependensOn);
        if (!processed) {
            element.relations.add(dependensOn);
        }
        
        return processed;
    }
}
