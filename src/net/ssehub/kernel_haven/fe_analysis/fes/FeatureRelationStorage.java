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
