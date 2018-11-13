package net.ssehub.kernel_haven.fe_analysis.fes;

import java.util.HashMap;
import java.util.Map;

import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Stores processed, but still relevant variables during the analysis, Treats variables with same name as equal,
 * independent if they contain an operator in their names.
 * @author El-Sharkawy
 * @param <V> The variable type to use with the storage instance.
 */
abstract class AbstractFeatureStorage<V> {
    
    private @NonNull Map<String, V> effects = new HashMap<>();
    
    /**
     * Adds a new {@link VariableWithFeatureEffect} and removes all old variables, which are no longer needed.
     * @param variable the variable to add.
     */
    public synchronized void add(V variable) {
        removeUnlessSimilarVariablesExist(getVariableName(variable));
        effects.put(getVariableName(variable), variable);
    }
    
    /**
     * Retrieves the name of the variable.
     * @param variable The name for which the name should be retrieved from.
     * @return The name of the specified variable.
     */
    protected abstract String getVariableName(V variable);
    
    /**
     * Returns the {@link VariableWithFeatureEffect} with the specified name.
     * @param variable The name to consider, contains also the value assignment including an equal character if in
     *     non-Boolean mode.
     * @return The specified variable or <tt>null</tt> if it does not exist or was already removed.
     */
    public synchronized @Nullable V getFeatureEffect(String variable) {
        return effects.get(variable);
    }

    /**
     * Clears the storage.
     */
    public synchronized void clear() {
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
    public @Nullable V getBaseVariable(@NonNull String variable) {
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
    private synchronized void removeUnlessSimilarVariablesExist(String variable) {
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
