package net.ssehub.kernel_haven.fe_analysis.arch_components;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.not;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;

/**
 * Tests the {@link ArchComponentResolver} with made-up data.
 * 
 * @author Adam
 */
@SuppressWarnings("null")
public class ArchComponentResolverTest {

    /**
     * Tests a simple case with all three trivial cases covered.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testAllTypesSimple() throws SetUpException {
        ArchComponentStorage components = new ArchComponentStorage();
        components.setComponent("A", "RSP1 (components)");
        components.setComponent("B", "RSP1 (components)");
        components.setComponent("C", "");
        
        // three parts: B, B && C, C
        Formula fe = or("B", or(and("B", "C"), "C"));
        
        VariableWithFeatureEffect varFe = new VariableWithFeatureEffect("A", fe);
        
        List<FeatureEffectWithArchComponent> result = 
            AnalysisComponentExecuter.executeComponent(ArchComponentResolver.class,
            new TestConfiguration(new Properties()),
            new VariableWithFeatureEffect[] {varFe}, new ArchComponentStorage[] {components});
        
        assertThat(result.size(), is(1));
        
        FeatureEffectWithArchComponent feComp = result.get(0);
        assertThat(feComp.getVariable(), is("A"));
        assertThat(feComp.getSameComponent(), is(new Variable("B")));
        assertThat(feComp.getMixedComponent(), is(and("B", "C")));
        assertThat(feComp.getOtherComponent(), is(new Variable("C")));
    }
    
    /**
     * Tests that multiple parts in the same category are joined correctly.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testSameTypeJoined() throws SetUpException {
        ArchComponentStorage components = new ArchComponentStorage();
        components.setComponent("A", "RSP1 (components)");
        components.setComponent("B", "RSP1 (components)");
        components.setComponent("C", "");
        
        // four parts: B, B && C, C && B, !B
        Formula fe = or("B", or(and("B", "C"), or(and("C", "B"), not("B"))));
        
        VariableWithFeatureEffect varFe = new VariableWithFeatureEffect("A", fe);
        
        List<FeatureEffectWithArchComponent> result = 
                AnalysisComponentExecuter.executeComponent(ArchComponentResolver.class,
                        new TestConfiguration(new Properties()),
                        new VariableWithFeatureEffect[] {varFe}, new ArchComponentStorage[] {components});
        
        assertThat(result.size(), is(1));
        
        FeatureEffectWithArchComponent feComp = result.get(0);
        assertThat(feComp.getVariable(), is("A"));
        assertThat(feComp.getSameComponent(), is(or("B", not("B"))));
        assertThat(feComp.getMixedComponent(), is(or(and("B", "C"), and("C", "B"))));
        assertThat(feComp.getOtherComponent(), nullValue());
    }
    
    /**
     * Tests that empty parts are left null.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testEmptyIsNull() throws SetUpException {
        ArchComponentStorage components = new ArchComponentStorage();
        components.setComponent("A", "RSP1 (components)");
        components.setComponent("B", "RSP1 (components)");
        components.setComponent("C", "");
        
        // four parts: B, B && C, C && B, !B
        Formula fe = True.INSTANCE;
        
        VariableWithFeatureEffect varFe = new VariableWithFeatureEffect("A", fe);
        
        List<FeatureEffectWithArchComponent> result = 
                AnalysisComponentExecuter.executeComponent(ArchComponentResolver.class,
                        new TestConfiguration(new Properties()),
                        new VariableWithFeatureEffect[] {varFe}, new ArchComponentStorage[] {components});
        
        assertThat(result.size(), is(1));
        
        FeatureEffectWithArchComponent feComp = result.get(0);
        assertThat(feComp.getVariable(), is("A"));
        assertThat(feComp.getSameComponent(), is(True.INSTANCE));
        assertThat(feComp.getMixedComponent(), nullValue());
        assertThat(feComp.getOtherComponent(), nullValue());
    }
    
    /**
     * Tests that a formula with no variable (i.e. the literal true) is considered to be the same component.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testNoVarIsSame() throws SetUpException {
        ArchComponentStorage components = new ArchComponentStorage();
        components.setComponent("A", "RSP1 (components)");
        components.setComponent("B", "RSP1 (components)");
        components.setComponent("C", "");
        
        // three parts: B, TRUE, C
        Formula fe = or("B", or(True.INSTANCE, "C"));
        
        VariableWithFeatureEffect varFe = new VariableWithFeatureEffect("A", fe);
        
        List<FeatureEffectWithArchComponent> result = 
                AnalysisComponentExecuter.executeComponent(ArchComponentResolver.class,
                        new TestConfiguration(new Properties()),
                        new VariableWithFeatureEffect[] {varFe}, new ArchComponentStorage[] {components});
        
        assertThat(result.size(), is(1));
        
        FeatureEffectWithArchComponent feComp = result.get(0);
        assertThat(feComp.getVariable(), is("A"));
        assertThat(feComp.getSameComponent(), is(or("B", True.INSTANCE)));
        assertThat(feComp.getOtherComponent(), is(new Variable("C")));
    }
    
    /**
     * Tests that non-boolean operators do not interfere with component detection.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testNonBooleanOperators() throws SetUpException {
        ArchComponentStorage components = new ArchComponentStorage();
        components.setComponent("A", "RSP1 (components)");
        components.setComponent("B", "RSP1 (components)");
        components.setComponent("C", "");
        
        // three parts: B, B && C, C
        Formula fe = or("B>1", or(and("B=2", "C<2"), "C=5"));
        
        VariableWithFeatureEffect varFe = new VariableWithFeatureEffect("A", fe);
        
        List<FeatureEffectWithArchComponent> result = 
            AnalysisComponentExecuter.executeComponent(ArchComponentResolver.class,
                    new TestConfiguration(new Properties()),
            new VariableWithFeatureEffect[] {varFe}, new ArchComponentStorage[] {components});
        
        assertThat(result.size(), is(1));
        
        FeatureEffectWithArchComponent feComp = result.get(0);
        assertThat(feComp.getVariable(), is("A"));
        assertThat(feComp.getSameComponent(), is(new Variable("B>1")));
        assertThat(feComp.getMixedComponent(), is(and("B=2", "C<2")));
        assertThat(feComp.getOtherComponent(), is(new Variable("C=5")));
    }
    
}
