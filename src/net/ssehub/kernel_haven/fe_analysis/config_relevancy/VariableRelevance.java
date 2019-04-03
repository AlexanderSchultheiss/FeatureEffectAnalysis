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
package net.ssehub.kernel_haven.fe_analysis.config_relevancy;

import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Data type / result of the {@link ConfigRelevancyChecker}. Contains:
 * <ul>
 *     <li>The name of the checked variable</li>
 *     <li>The Integer value of a past configuration (may be <tt>null</tt>)</li>
 *     <li>The (evaluated) feature effect constraint</li>
 *     <li>A description/rating of the situation</li>
 * </ul>
 * 
 * @author Adam
 */
@TableRow
public class VariableRelevance {

    /**
     * Description/rating of a past configuration of a single variable.
     */
    public static enum Relevance {
        
        SET_AND_RELEVANT("Configured and relevant"),
        SET_AND_IRRELEVANT("Configured, but not relevant"),
        NOT_SET_AND_RELEVANT("Not Configured, but relevant"),
        NOT_SET_AND_IRRELEVANT("Not Configured, not relevant"),
        NOT_FOUND_IN_CODE("Not found in any code condition"),
        UNKOWN("Unkown (couldn't solve feature effect)");
        
        private @NonNull String name;
        
        /**
         * Enum constructor to specify the description.
         * @param name The description.
         */
        private Relevance(@NonNull String name) {
            this.name = name;
        }
        
        @Override
        public @NonNull String toString() {
            return name;
        }
        
    }
    
    private @NonNull String variable;
    
    private @NonNull Relevance relevance;
    
    private @NonNull Formula featureEffect;
    
    private @Nullable Integer value;
    
    /**
     * Sole constructor.
     * 
     * @param variable The name of the variable
     * @param relevance The description of the situation.
     * @param featureEffect The (evaluated) feature effect constraint.
     * @param value The historical configuration of the variable.
     */
    public VariableRelevance(@NonNull String variable, @NonNull Relevance relevance, @NonNull Formula featureEffect,
            @Nullable Integer value) {
        
        this.variable = variable;
        this.relevance = relevance;
        this.featureEffect = featureEffect;
        this.value = value;
    }

    /**
     * Returns the name of the variable.
     * 
     * @return The name of the variable.
     */
    @TableElement(index = 0, name = "Variable")
    public @NonNull String getVariable() {
        return variable;
    }
    
    /**
     * Returns the historical value of the variable.
     * 
     * @return The historical value of the variable.
     */
    @TableElement(index = 1, name = "Value")
    public @Nullable Integer getValue() {
        return value;
    }
    
    /**
     * Returns the (evaluated) feature effect constraint of the variable.
     * 
     * @return The h(evaluated) feature effect constraint of the variable.
     */
    @TableElement(index = 3, name = "Feature Effect")
    public @NonNull Formula getFeatureEffect() {
        return featureEffect;
    }
    
    /**
     * Returns the description of the situation.
     * 
     * @return The description of the situation.
     */
    @TableElement(index = 2, name = "Relevance")
    public @NonNull Relevance getRelevance() {
        return relevance;
    }
    
}
