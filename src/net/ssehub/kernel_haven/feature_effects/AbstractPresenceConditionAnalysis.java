package net.ssehub.kernel_haven.feature_effects;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.ssehub.kernel_haven.PipelineConfigurator;
import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AbstractAnalysis;
import net.ssehub.kernel_haven.config.Configuration;
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
abstract class AbstractPresenceConditionAnalysis extends AbstractAnalysis {
    
    public static final String USE_VARMODEL_VARIABLES_ONLY = "analysis.consider_vm_vars_only";
    
    /**
     * Whether non-boolean replacements are enabled. This is true if the NonBooleanPreperation ran on the source tree.
     */
    protected boolean nonBooleanReplacements;
    
    /**
     * Whether non boolean replacements in variable names (e.g. _gt_) are used and should be turned back into the
     * human readable form.
     */
    private boolean replaceNonBooleanReplacements;
    
    private boolean considerVmVarsOnly;
    
    private Pattern relevantVarsPattern;
    private VariabilityModel vm;

    /**
     * Sole constructor for this class.
     * @param config The Configuration to use.
     * 
     * @throws SetUpException If reading configuration options fails or if a build model was specified,
     * but exited abnormally.
     */
    public AbstractPresenceConditionAnalysis(Configuration config) throws SetUpException {
        super(config);
        
        String relevant = config.getProperty("analysis.relevant_variables", ".*");
        try {
            relevantVarsPattern = Pattern.compile(relevant);
        } catch (PatternSyntaxException e) {
            throw new SetUpException(e);
        }
        
        considerVmVarsOnly = config.getBooleanProperty(USE_VARMODEL_VARIABLES_ONLY, false);
        vm = considerVmVarsOnly ? PipelineConfigurator.instance().getVmProvider().getResult() : null;
        if (null == vm && considerVmVarsOnly) {
            throw new SetUpException(USE_VARMODEL_VARIABLES_ONLY + "[true] was specified, but no variability model"
                + " was passed.");
        }
        
        this.nonBooleanReplacements = Boolean.parseBoolean(config.getProperty("prepare_non_boolean"));
        this.replaceNonBooleanReplacements = nonBooleanReplacements
            || Boolean.parseBoolean(config.getProperty("code.extractor.fuzzy_parsing"));
    }
    
    /**
     * Finds all variables in the given formula. This recursively walks through the whole tree.
     * 
     * @param formula The formula to find variables in.
     * @param result The resulting set to add variables to.
     */
    protected void findVars(Formula formula, Set<Variable> result) {
        
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
    protected boolean isRelevant(Formula formula) {
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
    protected boolean isRelevant(String variable) {
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
     * Converts the formula into a string representation.
     * In case of non Boolean replacements used, the non boolean replacements will be translated back into human
     * readable form.
     * 
     * @param formula The formula to translate.

     * @return A string representation of this formula, in a C-style like format. 
     */
    protected String toString(Formula formula) {
        return toString(formula.toString());
    }
    
    /**
     * Converts the formula into a string representation.
     * In case of non Boolean replacements used, the non boolean replacements will be translated back into human
     * readable form.
     * 
     * @param formula {@link Formula#toString()}

     * @return A string representation of this formula, in a C-style like format. 
     */
    protected String toString(String formula) {
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


}
