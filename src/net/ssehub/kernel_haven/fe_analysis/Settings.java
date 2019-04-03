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
package net.ssehub.kernel_haven.fe_analysis;

import java.util.regex.Pattern;

import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Stores the settings provided by this plug-in.
 * @author El-Sharkawy
 *
 */
public class Settings {
    
    public static final @NonNull Setting<@NonNull Pattern> RELEVANT_VARIABLES
        = new Setting<>("analysis.relevant_variables", Type.REGEX, true, ".*", "Defines a regular expression that "
            + "specifies which variables should be present in the output.");
    
    /**
     * Different steps in the analysis where and whether to simplify results.
     */
    public static enum SimplificationType {
        // Do not change order.
        
        /**
         * No simplification in any step.
         */
        NO_SIMPLIFICATION,
        
        /**
         * Simplification already in presence condition detection (recommended) and subsequent steps.
         */
        PRESENCE_CONDITIONS,
        
        /**
         * Simplification only in feature effects (and in subsequent steps if there are any).
         */
        FEATURE_EFFECTS,
    }
    
    public static final @NonNull Setting<@NonNull SimplificationType> SIMPLIFIY
        = new EnumSetting<>("analysis.simplify_conditions", SimplificationType.class, true,
            SimplificationType.NO_SIMPLIFICATION,
            "Specifies whether and and which analysis step, results should be simplified:\n"
            + " - " + SimplificationType.NO_SIMPLIFICATION + ": Won't simplifiy results.\n"
            + " - " + SimplificationType.PRESENCE_CONDITIONS + ": Will simplifiy (indermediate) results of presence\n"
            + "   condition detection and all later steps.\n"
            + " - " + SimplificationType.FEATURE_EFFECTS + ": Will simplifiy the results of the feature effect "
            + "analysis.");
    
    /**
     * Don't allow any instances.
     */
    private Settings() {
    }
    
}
