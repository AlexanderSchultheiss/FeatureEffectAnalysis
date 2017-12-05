package net.ssehub.kernel_haven.fe_analysis;

import java.lang.reflect.Method;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Formula;

/**
 * Wrapper to use the LogicUtils without hard dependencies to CNF-Utils package.
 * The core idea behind this wrapper is to use methods from the other plug-in if required, but do not implement a hard
 * dependency between both packages for situations this wrapper is not used.
 * @author El-Sharkawy
 *
 */
// TODO SE: @Adam Find a better solution if possible.
public class FormulaSimplifier {
    
    private Method simplifyMethod;
    
    /**
     * Sole constructor to initialize this wrapper.
     * @throws SetUpException If the CNF-Utils package is not available.
     */
    public FormulaSimplifier() throws SetUpException {
        try {
            // Do not make this part static, otherwise this wrapper becomes useless
            Class<?> clazz = Class.forName("net.ssehub.kernel_haven.logic_utils.LogicUtils");
            simplifyMethod = clazz.getDeclaredMethod("simplify", Formula.class);
        } catch (ReflectiveOperationException e) {
            throw new SetUpException("Could not load LogicUtils from CNF-Utils package: " + e.getLocalizedMessage(), e);
        }
    }
    
    /**
     * Simplifies the given {@link Formula}. The semantics do not change.
     * 
     * @param formula The formula to simplify. Must not be <code>null</code>.
     * 
     * @return The simplified formula. Not <code>null</code>.
     */
    public Formula simplify(Formula formula) {
        Formula result;
        try {
            result = (Formula) simplifyMethod.invoke(null, formula);
        } catch (ReflectiveOperationException e) {
            // Should not be possible
            Logger.get().logError(e.getMessage());
            result = formula;
        } catch (IllegalArgumentException e) {
            // Should not be possible
            Logger.get().logError(e.getMessage());
            result = formula;
        }
        
        return result;
    }
}
