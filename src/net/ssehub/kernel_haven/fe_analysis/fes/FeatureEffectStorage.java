package net.ssehub.kernel_haven.fe_analysis.fes;

import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;

/**
 * Stores processed, but still relevant {@link VariableWithFeatureEffect}s.
 * @author El-Sharkawy
 *
 */
public class FeatureEffectStorage extends AbstractFeatureStorage<VariableWithFeatureEffect> {
    
    @Override
    protected String getVariableName(VariableWithFeatureEffect variable) {
        return variable.getVariable();
    }
}
