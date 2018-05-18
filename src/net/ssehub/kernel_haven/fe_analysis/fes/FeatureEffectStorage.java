package net.ssehub.kernel_haven.fe_analysis.fes;

import java.util.HashMap;
import java.util.Map;

import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Stores processed, but still relevant {@link VariableWithFeatureEffect}s.
 * @author El-Sharkawy
 *
 */
public class FeatureEffectStorage {
    
    private @NonNull Map<String, VariableWithFeatureEffect> effects = new HashMap<>();
    
    /**
     * Adds a new {@link VariableWithFeatureEffect} and removes all old variables, which are no longer needed.
     * @param variable the variable to add.
     */
    public void add(VariableWithFeatureEffect variable) {
        removeUnlessSimilarVariablesExist(variable.getVariable());
        effects.put(variable.getVariable(), variable);
    }
    
    /**
     * Returns the {@link VariableWithFeatureEffect} with the specified name.
     * @param variable The name to consider, contains also the value assignment including an equal character if in
     *     non-Boolean mode.
     * @return The specified variable or <tt>null</tt> if it does not exist or was already removed.
     */
    public @Nullable VariableWithFeatureEffect getFeatureEffect(String variable) {
        return effects.get(variable);
    }

    /**
     * Clears the storage.
     */
    public void clear() {
        effects.clear();
    }
    
    /**
     * Returns the base variable as it is used in define-expressions. This is either
     * <ul>
     *   <li>The variable it self, if in Boolean model (by default)</li>
     *   <li>The variable without any equality assignment if in non-Boolean mode</li>
     * </ul>
     * @param variable The variable name (including value assignments) for which the define variable shall be returned
     *     for.
     * @return The variable without a value assignment and its feature effect.
     */
    public @Nullable VariableWithFeatureEffect getBaseVariable(@NonNull String variable) {
        int index = variable.lastIndexOf("=");
        if (-1 != index) {
            variable = NullHelpers.notNull(variable.substring(0, index));
        }
        
        return getFeatureEffect(variable);
    }
    
    /**
     * {@link #clear()}s the storage if no relevant variables are stored anymore. Relevant variables are variables which
     * start with the same name.
     * <ul>
     *   <li>variables are alphabetically sorted</li>
     *   <li>define variables are without a suffix</li>
     *   <li>value assignments are sorted at the end</li>
     *   <li>Critical: some times other variables which start with a similar name may be sorted between the define
     *       variable and the assignment variables</li>
     * </ul>
     * @param variable A variable to be added, which serves as a reference for which variables must not be deleted.
     */
    private void removeUnlessSimilarVariablesExist(String variable) {
        boolean containsSimilarVariables = false;
        for (String otherVariable : effects.keySet()) {
            if (variable.startsWith(otherVariable)) {
                containsSimilarVariables = true;
                break;
            }
        }
        
        if (!containsSimilarVariables) {
            clear();
        }
    }
}
