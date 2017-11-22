package net.ssehub.kernel_haven.feature_effects;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.PipelineConfigurator;
import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.config.EnumSetting;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Common functionality for the detection of presence conditions and the generation of feature effect constraints.
 * @author El-Sharkawy
 *
 */
class PresenceConditionAnalysisHelper {
    
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
    
    /**
     * Whether non-boolean replacements are enabled. This is true if the NonBooleanPreperation ran on the source tree.
     */
    protected boolean nonBooleanReplacements;
    
    private boolean replaceNonBooleanReplacements;
    
    private boolean considerVmVarsOnly;
    private SimplificationType simplificationType;
    
    private Pattern relevantVarsPattern;
    private VariabilityModel vm;

    /**
     * Sole constructor for this class.
     * @param config The Configuration to use.
     * 
     * @throws SetUpException If reading configuration options fails or if a build model was specified,
     * but exited abnormally.
     */
    public PresenceConditionAnalysisHelper(Configuration config) throws SetUpException {
        config.registerSetting(USE_VARMODEL_VARIABLES_ONLY);
        config.registerSetting(RELEVANT_VARIABLES);
        config.registerSetting(SIMPLIFIY);
        
        relevantVarsPattern = config.getValue(RELEVANT_VARIABLES);
        considerVmVarsOnly = config.getValue(USE_VARMODEL_VARIABLES_ONLY);
        simplificationType = config.getValue(SIMPLIFIY);
        
        vm = considerVmVarsOnly ? PipelineConfigurator.instance().getVmProvider().getResult() : null;
        if (null == vm && considerVmVarsOnly) {
            throw new SetUpException(USE_VARMODEL_VARIABLES_ONLY + "[true] was specified, but no variability model"
                + " was passed.");
        }
        
        this.nonBooleanReplacements = config.getValue(DefaultSettings.PREPARE_NON_BOOLEAN);
        this.replaceNonBooleanReplacements = nonBooleanReplacements || config.getValue(DefaultSettings.FUZZY_PARSING);
    }
    
    /**
     * Finds all variables in the given formula. This recursively walks through the whole tree.
     * 
     * @param formula The formula to find variables in.
     * @param result The resulting set to add variables to.
     */
    public void findVars(Formula formula, Set<Variable> result) {
        
        if (formula instanceof Variable) {
            result.add((Variable) formula);
            
        } else if (formula instanceof Negation) {
            findVars(((Negation) formula).getFormula(), result);
            
        } else if (formula instanceof Disjunction) {
            Disjunction dis = (Disjunction) formula;
            findVars(dis.getLeft(), result);
            findVars(dis.getRight(), result);
            
        } else if (formula instanceof Conjunction) {
            Conjunction con = (Conjunction) formula;
            findVars(con.getLeft(), result);
            findVars(con.getRight(), result);
        }
        // ignore true and false
        
    }
    
    /**
     * Checks if a complete formula should be considered.
     * @param formula The formula to check.
     * @return <tt>true</tt> if the formula should be kept, <tt>false</tt> if the formula should be discarded.
     */
    public boolean isRelevant(Formula formula) {
        boolean isRelevant = false;
        
        // Checks that at least one variable of the formula is relevant
        Set<Variable> variables = new HashSet<>();
        findVars(formula, variables);
        Iterator<Variable> varItr = variables.iterator();
        while (varItr.hasNext() && !isRelevant) {
            Variable variable = varItr.next();
            isRelevant = isRelevant(variable.getName());
        }
        
        return isRelevant;
    }
    
    /**
     * Helper function to determine which variables are relevant.
     * 
     * @param variable The variable to check.
     * @return Whether the variable is relevant or not.
     */
    public boolean isRelevant(String variable) {
        boolean isRelevant;
        if (considerVmVarsOnly) {
            isRelevant = vm.getVariableMap().containsKey(variable);
            if (!isRelevant && nonBooleanReplacements) {
                int index = variable.indexOf("_eq_");
                if (index > -1) {
                    isRelevant = vm.getVariableMap().containsKey(variable.substring(0, index));
                }
            }
        } else {
            isRelevant = relevantVarsPattern.matcher(variable).matches();
        }
        
        return isRelevant;
    }
    
    /**
     * Does the necessary replacements in the formula variable names, in case of non Boolean replacements.
     * The non boolean replacements will be translated back into human readable form. No-op in case non Boolean
     * replacements are switched off.
     * 
     * @param formula The formula to do replacements in.

     * @return The same formula, but with all necessary replacements in the variable names.
     */
    public Formula doReplacements(Formula formula) {
        Formula result = formula;
        
        if (replaceNonBooleanReplacements) {
            if (formula instanceof Variable) {
                result = new Variable(doReplacements(((Variable) formula).getName()));
                
            } else if (formula instanceof Negation) {
                result = new Negation(
                        doReplacements(((Negation) formula).getFormula()));
                
            } else if (formula instanceof Disjunction) {
                result = new Disjunction(
                        doReplacements(((Disjunction) formula).getLeft()),
                        doReplacements(((Disjunction) formula).getRight()));
                
            } else if (formula instanceof Conjunction) {
                result = new Conjunction(
                        doReplacements(((Conjunction) formula).getLeft()),
                        doReplacements(((Conjunction) formula).getRight()));
            }
            // ignore true and false
        }
        
        return result;
    }
    
    /**
     * Does the necessary replacements in the formula string, in case of non Boolean replacements.
     * The non boolean replacements will be translated back into human readable form. No-op in case non Boolean
     * replacements are switched off.
     * 
     * @param formula The formula to do replacements in.

     * @return The same formula, but with the replacements done.
     */
    public String doReplacements(String formula) {
        if (replaceNonBooleanReplacements) {
            formula = formula.replace("_eq_", "=");
            formula = formula.replace("_ne_", "!=");
            formula = formula.replace("_gt_", ">");
            formula = formula.replace("_ge_", ">=");
            formula = formula.replace("_lt_", ">");
            formula = formula.replace("_le_", ">=");
        }
        return formula;
    }

    /**
     * Whether non boolean replacements in variable names (e.g. _gt_) are used and should be turned back into the
     * human readable form.
     * 
     * @return Whether to do non boolean replcaments or not.
     */
    public boolean isNonBooleanReplacements() {
        return nonBooleanReplacements;
    }
    
    /**
     * Returns at which (and whether) analysis step conditions should be simplified.
     * @return At which (and whether) analysis step conditions should be simplified.
     */
    public SimplificationType getSimplificationMode() {
        return simplificationType;
    }

}
