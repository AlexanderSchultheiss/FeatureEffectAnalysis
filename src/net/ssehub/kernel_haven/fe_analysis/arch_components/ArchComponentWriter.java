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
package net.ssehub.kernel_haven.fe_analysis.arch_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.Map;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.fe_analysis.arch_components.ArchComponentWriter.SingleArchComponentMapping;

/**
 * A helper analysis component that can be used to write a {@link ArchComponentStorage} as a table.
 * 
 * @author Adam
 */
public class ArchComponentWriter extends AnalysisComponent<SingleArchComponentMapping> {

    /**
     * A single architecture component mapping from a {@link ArchComponentStorage}.
     */
    @TableRow
    public static class SingleArchComponentMapping {
        
        private @NonNull String var;
        
        private @NonNull String component;

        /**
         * Creates a {@link SingleArchComponentMapping}.
         * 
         * @param var The variable that the component is stored for.
         * @param component The architecture component of that variable.
         */
        public SingleArchComponentMapping(@NonNull String var, @NonNull String component) {
            this.var = var;
            this.component = component;
        }
        
        /**
         * Returns the variable that the component is stored for.
         * 
         * @return The variable name.
         */
        @TableElement(index = 0, name = "Variable")
        public @NonNull String getVar() {
            return var;
        }
        
        /**
         * Returns the architecture component of the variable.
         * 
         * @return The component.
         */
        @TableElement(index = 1, name = "Architecture Component")
        public @NonNull String getComponent() {
            return component;
        }
        
    }
    
    private @NonNull AnalysisComponent<ArchComponentStorage> input;
    
    /**
     * Creates an {@link ArchComponentWriter}.
     * 
     * @param config The pipeline configuration.
     * @param input The {@link ArchComponentStorage} input.
     */
    public ArchComponentWriter(@NonNull Configuration config,
            @NonNull AnalysisComponent<ArchComponentStorage> input) {
        
        super(config);
        this.input = input;
    }

    @Override
    protected void execute() {
        ArchComponentStorage storage = input.getNextResult();
        
        if (storage == null) {
            LOGGER.logError("ArchComponentStorage is null");
        } else {
            
            for (Map.Entry<String, String> entry : storage) {
                addResult(new SingleArchComponentMapping(notNull(entry.getKey()), notNull(entry.getValue())));
            }
            
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Architecture Components";
    }
    
}
