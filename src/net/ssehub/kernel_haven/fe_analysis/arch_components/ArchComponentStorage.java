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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A storage for a mapping of variable -&gt; architecture component.
 * 
 * @author Adam
 */
public class ArchComponentStorage implements Iterable<Map.Entry<String, String>> {
    
    private @NonNull Map<String, String> components;
    
    /**
     * Creates an empty storage.
     */
    public ArchComponentStorage() {
        components = new HashMap<>();
    }
    
    /**
     * Sets the architecture component for a variable.
     * 
     * @param var The variable to set the component for.
     * @param component The architecture component. Must be a non-empty string.
     */
    public void setComponent(@NonNull String var, @NonNull String component) {
        if (!component.isEmpty()) {
            components.put(var, component);
        }
    }
    
    /**
     * Returns the architecture component of the given variable. If none is specified, an empty string is returned;
     * this should be interpreted as "different to everything".
     * 
     * @param var The variable to get the component for.
     * 
     * @return The component for the variable.
     */
    public @NonNull String getComponent(@NonNull String var) {
        return notNull(components.getOrDefault(var, ""));
    }
    
    /**
     * Returns the number of variables mapped to an architecture component.
     * 
     * @return The number of variables stored here.
     */
    public int getNumVariablesWithComponent() {
        return components.size();
    }
    
    /**
     * Checks whether the two given variables belong to the same architecture component.
     * 
     * @param var1 The first variable.
     * @param var2 The second variable.
     * 
     * @return Whether the two variables belong to the same architecture component.
     */
    public boolean isSameComponent(@NonNull String var1, @NonNull String var2) {
        String c1 = getComponent(var1);
        String c2 = getComponent(var2);
        
        boolean result = false;
        // empty strings are always different
        if (!c1.isEmpty() && !c2.isEmpty()) {
            result = c1.equals(c2);
        }
        
        return result;
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        return components.entrySet().iterator();
    }
    
}
