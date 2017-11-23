package net.ssehub.kernel_haven.feature_effects;

import java.util.regex.Pattern;

import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;

/**
 * Stores the settings provided by this plug-in.
 * @author El-Sharkawy
 *
 */
public class Settings {
    
    public static final Setting<Boolean> USE_VARMODEL_VARIABLES_ONLY
        = new Setting<>("analysis.consider_vm_vars_only", Type.BOOLEAN, true, "false", "Defines whether the analysis "
            + "should only consider variables that are present in the variability model.");

    public static final Setting<Pattern> RELEVANT_VARIABLES
        = new Setting<>("analysis.relevant_variables", Type.REGEX, true, ".*", "Defines a regular expression that "
            + "specifies which variables should be present in the output.");
    
    /**
     * Different steps in the analysis where and whether to simplify results.
     */
    public static enum SimplificationType {
        NO_SIMPLIFICATION,
        PRESENCE_CONDITIONS,
        FEATURE_EFFECTS,
    }
    
    public static final Setting<SimplificationType> SIMPLIFIY
        = new EnumSetting<SimplificationType>("analysis.simplify_conditions", SimplificationType.class, true,
            SimplificationType.NO_SIMPLIFICATION,
            "Specifies whether and and which analysis step, results should be simplified:\n"
            + " - " + SimplificationType.NO_SIMPLIFICATION + ": Won't simplifiy results.\n"
            + " - " + SimplificationType.PRESENCE_CONDITIONS + ": Will simplifiy (indermediate) results of presence\n"
            + "   condition detection and all later steps.\n"
            + " - " + SimplificationType.FEATURE_EFFECTS + ": Will simplifiy the results of the feature effect "
            + "analysis.");
    
}