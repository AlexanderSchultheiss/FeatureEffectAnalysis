package net.ssehub.kernel_haven.fe_analysis;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.PipelineConfigurator;
import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.util.logic.Conjunction;
import net.ssehub.kernel_haven.util.logic.Disjunction;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Negation;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * Common functionality for the detection of presence conditions and the generation of feature effect constraints.
 * @author El-Sharkawy
 *
 */
public class PresenceConditionAnalysisHelper {
    
    /**
     * Whether non-boolean replacements are enabled. This is true if the NonBooleanPreperation ran on the source tree.
     */
    protected boolean nonBooleanReplacements;
    
    private boolean replaceNonBooleanReplacements;
    
    private boolean considerVmVarsOnly;
    private @NonNull SimplificationType simplificationType;
    
    private @NonNull Pattern relevantVarsPattern;
    private @Nullable VariabilityModel vm;

    /**
     * Sole constructor for this class.
     * @param config The Configuration to use.
     * 
     * @throws SetUpException If reading configuration options fails or if a build model was specified,
     * but exited abnormally.
     */
    public PresenceConditionAnalysisHelper(@NonNull Configuration config) throws SetUpException {
        config.registerSetting(DefaultSettings.ANALYSIS_USE_VARMODEL_VARIABLES_ONLY);
        config.registerSetting(Settings.RELEVANT_VARIABLES);
        config.registerSetting(Settings.SIMPLIFIY);
        
        relevantVarsPattern = config.getValue(Settings.RELEVANT_VARIABLES);
        considerVmVarsOnly = config.getValue(DefaultSettings.ANALYSIS_USE_VARMODEL_VARIABLES_ONLY);
        simplificationType = config.getValue(Settings.SIMPLIFIY);
        
        vm = considerVmVarsOnly ? notNull(PipelineConfigurator.instance().getVmProvider()).getResult() : null;
        if (null == vm && considerVmVarsOnly) {
            throw new SetUpException(DefaultSettings.ANALYSIS_USE_VARMODEL_VARIABLES_ONLY + "[true] was specified,"
                + "but no variability model was passed.");
        }
        
        // Check if ANY NonBooleanPreparation-Class is specified/used
        this.nonBooleanReplacements = false;
        List<String> preparationClasses = config.getValue(DefaultSettings.PREPARATION_CLASSES);
        for (int i = 0; i < preparationClasses.size() && !nonBooleanReplacements; i++) {
            if (preparationClasses.get(i).endsWith("NonBooleanPreperation")) {
                nonBooleanReplacements = true;
            }
        }
        this.replaceNonBooleanReplacements = nonBooleanReplacements || config.getValue(DefaultSettings.FUZZY_PARSING);
    }
    
    /**
     * Finds all variables in the given formula. This recursively walks through the whole tree.
     * 
     * @param formula The formula to find variables in.
     * @param result The resulting set to add variables to.
     */
    public void findVars(@NonNull Formula formula, @NonNull Set<@NonNull Variable> result) {
        
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
    public boolean isRelevant(@NonNull Formula formula) {
        boolean isRelevant = false;
        
        // Checks that at least one variable of the formula is relevant
        Set<@NonNull Variable> variables = new HashSet<>();
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
    public boolean isRelevant(@NonNull String variable) {
        boolean isRelevant;
        if (considerVmVarsOnly) {
            // vm != since considerVmVarsOnly == true
            isRelevant = notNull(vm).getVariableMap().containsKey(variable);
            if (!isRelevant && nonBooleanReplacements) {
                int index = variable.indexOf("_eq_");
                if (index > -1) {
                    isRelevant = notNull(vm).getVariableMap().containsKey(variable.substring(0, index));
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
    public @NonNull Formula doReplacements(@NonNull Formula formula) {
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
    public @NonNull String doReplacements(@NonNull String formula) {
        if (replaceNonBooleanReplacements) {
            formula = notNull(formula.replace("_eq_", "="));
            formula = notNull(formula.replace("_ne_", "!="));
            formula = notNull(formula.replace("_gt_", ">"));
            formula = notNull(formula.replace("_ge_", ">="));
            formula = notNull(formula.replace("_lt_", ">"));
            formula = notNull(formula.replace("_le_", ">="));
        }
        return formula;
    }

    /**
     * Whether non boolean replacements in variable names (e.g. _gt_) are used and should be turned back into the
     * human readable form.
     * 
     * @return Whether to do non boolean replacements or not.
     */
    public boolean isNonBooleanReplacements() {
        return nonBooleanReplacements;
    }
    
    /**
     * Returns at which (and whether) analysis step conditions should be simplified.
     * @return At which (and whether) analysis step conditions should be simplified.
     */
    public @NonNull SimplificationType getSimplificationMode() {
        return simplificationType;
    }

}
