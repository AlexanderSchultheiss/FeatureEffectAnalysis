package net.ssehub.kernel_haven.fe_analysis.arch_components;

import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.logic.False;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Represents a feature effect split-up based on architecture component mapping of used variables.
 * 
 * @author Adam
 */
@TableRow
public class FeatureEffectWithArchComponent {

    private @NonNull String variable;
    
    private @Nullable Formula sameComponent;
    
    private @Nullable Formula mixedComponent;
    
    private @Nullable Formula otherComponent;

    /**
     * Creates a new {@link FeatureEffectWithArchComponent}.
     * 
     * @param variable The variable that this object holds the split feature effect for.
     * @param sameComponent The feature effect part with only variables in the same architecture component.
     * @param mixedComponent The feature effect part with variables in both, the same and other architecture components.
     * @param otherComponent The feature effect part with only variables in other architecture components.
     */
    public FeatureEffectWithArchComponent(@NonNull String variable, @Nullable Formula sameComponent,
            @Nullable Formula mixedComponent, @Nullable Formula otherComponent) {
        
        this.variable = variable;
        if (mixedComponent != False.INSTANCE) {
            this.mixedComponent = mixedComponent;
        }
        if (otherComponent != False.INSTANCE) {
            this.otherComponent = otherComponent;
        }

        // only store sameComponent if it's not False, or if none of the others have a value 
        if (sameComponent != False.INSTANCE || (this.mixedComponent == null && this.otherComponent == null)) {
            this.sameComponent = sameComponent;
        }
    }
    
    /**
     * Returns the variable that this object stores the split feature effect for.
     * 
     * @return The variable name.
     */
    @TableElement(index = 0, name = "Variable")
    public @NonNull String getVariable() {
        return variable;
    }
    
    /**
     * Returns the feature effect part with only variables in the same architecture component.
     * 
     * @return The same component part.
     */
    @TableElement(index = 1, name = "Same Component")
    public @Nullable Formula getSameComponent() {
        return sameComponent;
    }
    
    /**
     * Returns the feature effect part with variables in both, the same and other architecture components.
     * 
     * @return The mixed component part.
     */
    @TableElement(index = 2, name = "Mixed Component")
    public @Nullable Formula getMixedComponent() {
        return mixedComponent;
    }
    
    /**
     * Returns the feature effect part with only variables in other architecture components.
     * 
     * @return The other component part.
     */
    @TableElement(index = 3, name = "Other Component")
    public @Nullable Formula getOtherComponent() {
        return otherComponent;
    }
    
}
