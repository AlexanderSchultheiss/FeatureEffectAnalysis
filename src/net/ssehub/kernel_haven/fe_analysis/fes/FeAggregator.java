package net.ssehub.kernel_haven.fe_analysis.fes;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.fe_analysis.FormulaSimplifier;
import net.ssehub.kernel_haven.fe_analysis.Settings;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.StringUtils;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.logic.DisjunctionQueue;
import net.ssehub.kernel_haven.util.logic.Formula;

/**
 * Aggregates feature effect constraints for values of the same variable. Only relevant in Pseudo-Boolean settings.
 * <br /><br />
 * Example:
 * <code><pre>
 * VAR=1 -> A
 * VAR=2 -> B
 * 
 * => Var -> A || B
 * </pre></code>
 * 
 * @author El-Sharkawy
 *
 */
public class FeAggregator extends AnalysisComponent<VariableWithFeatureEffect> {
    static final String OPERATOR_REGEX = "^.*(=|!=|<|<=|>|>=).*$";
    
    private AnalysisComponent<VariableWithFeatureEffect> feDetector;
    private boolean simplify = false;
    private SimplificationType simplifyType;
    private FormulaSimplifier simplifier = null;

    /**
     * Creates an {@link FeAggregator}, do create one constraint for the separated values of integer variables.
     * @param config The global configuration.
     * @param feDetector Probably {@link net.ssehub.kernel_haven.feature_effects.FeatureEffectFinder}.
     * @throws SetUpException If creating this component fails, probably if simplification should is turned on, but
     *     LogicUtils are not present in plug-ins directory.
     */
    public FeAggregator(Configuration config, AnalysisComponent<VariableWithFeatureEffect> feDetector)
        throws SetUpException {
        
        super(config);
        this.feDetector = feDetector;
        simplifyType = config.getValue(Settings.SIMPLIFIY);
        if (simplifyType.ordinal() >= SimplificationType.PRESENCE_CONDITIONS.ordinal()) {
            // Will throw an exception if CNF Utils are not present (but was selected by user in configuration file)
            simplifier = new FormulaSimplifier();
            simplify = true;
        }
    }

    @Override
    protected void execute() {
        DisjunctionQueue conditions;
        if (null != simplifier) {
            conditions = new DisjunctionQueue(simplify, f -> simplifier.simplify(f));
        } else {
            conditions = new DisjunctionQueue(simplify);
        }
        // The name of the variable/value pairs, which are grouped together
        String groupName = null;
        
        VariableWithFeatureEffect var;
        while ((var = feDetector.getNextResult()) != null) {
            if (null == groupName) {
                // Very first element
                conditions.add(var.getFeatureEffect());

                // Determine name of variables group
                String varName = var.getVariable();
                int lastIndex = StringUtils.getLastOperatorIndex(varName);
                if (-1 != lastIndex) {
                    groupName = varName.substring(0, lastIndex);
                } else {
                    groupName = varName;
                }
            } else {
                // Determine name of variables group
                String varName = var.getVariable();
                int lastIndex = StringUtils.getLastOperatorIndex(varName);
                if (-1 != lastIndex) {
                    varName = varName.substring(0, lastIndex);
                }
                
                if (varName.equals(groupName)) {
                    conditions.add(var.getFeatureEffect());
                } else {
                    // New group started
                    Formula completeFE = conditions.getDisjunction(groupName);
                    
                    // Send aggregated elements and reset current group
                    addResult(new VariableWithFeatureEffect(groupName, completeFE));
                    
                    // Start the new group
                    groupName = varName;
                    conditions.add(var.getFeatureEffect());
                }
            }
        }
        
        // Process last element
        Formula completeFE = conditions.getDisjunction(groupName);
        
        // Send aggregated elements and reset current group
        addResult(new VariableWithFeatureEffect(groupName, completeFE));
    }

    @Override
    public String getResultName() {
        return "FEs per Variable";
    }

}
