package net.ssehub.kernel_haven.fe_analysis.relations;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Tests the {@link PotentialParentFinder}.
 * 
 * @author Adam
 */
public class PotentialParentFinderTest {

    /**
     * Tests a simple case.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    @SuppressWarnings("null")
    public void testSimple() throws SetUpException {
        Set<@NonNull Formula> pcs = new HashSet<>();
        pcs.add(new Variable("A"));
        pcs.add(and("A", "B"));
        VariableWithPcs a = new VariableWithPcs("A", pcs);
        
        pcs = new HashSet<>();
        pcs.add(and("A", "B"));
        pcs.add(and(and("A", "B"), "C"));
        pcs.add(and("B", "C"));
        VariableWithPcs b = new VariableWithPcs("B", pcs);

        pcs = new HashSet<>();
        pcs.add(and(and("A", "B"), "C"));
        VariableWithPcs c = new VariableWithPcs("C", pcs);
        
        pcs = new HashSet<>();
        pcs.add(and("D", "A"));
        pcs.add(and("D", and("A", "B")));
        pcs.add(and("D", and(and("A", "B"), "C")));
        VariableWithPcs d = new VariableWithPcs("D", pcs);
        
        TestConfiguration config = new TestConfiguration(new Properties());
        List<VariableWithPotentialParents> result =
                AnalysisComponentExecuter.executeComponent(PotentialParentFinder.class, config,
                new VariableWithPcs[] {a, b, c, d});
        
        assertThat(result.size(), is(4));
        
        VariableWithPotentialParents pp = result.get(0);
        assertThat(pp.getVariable(), is("A"));
        assertThat(pp.getPotentialParent("A"), nullValue());
        assertThat(pp.getPotentialParent("B").getVariable(), is("B"));
        assertThat(pp.getPotentialParent("B").getProbability(), is(0.5));
        assertThat(pp.getPotentialParent("C"), nullValue());
        
        pp = result.get(1);
        assertThat(pp.getVariable(), is("B"));
        assertThat(pp.getPotentialParent("A").getVariable(), is("A"));
        assertThat(pp.getPotentialParent("A").getProbability(), is(2.0 / 3.0));
        assertThat(pp.getPotentialParent("B"), nullValue());
        assertThat(pp.getPotentialParent("C").getVariable(), is("C"));
        assertThat(pp.getPotentialParent("C").getProbability(), is(2.0 / 3.0));
        
        pp = result.get(2);
        assertThat(pp.getVariable(), is("C"));
        assertThat(pp.getPotentialParent("A").getVariable(), is("A"));
        assertThat(pp.getPotentialParent("A").getProbability(), is(1.0));
        assertThat(pp.getPotentialParent("B").getVariable(), is("B"));
        assertThat(pp.getPotentialParent("B").getProbability(), is(1.0));
        assertThat(pp.getPotentialParent("C"), nullValue());
        
        pp = result.get(3);
        assertThat(pp.getVariable(), is("D"));
        assertThat(pp.getPotentialParent("A").getVariable(), is("A"));
        assertThat(pp.getPotentialParent("A").getProbability(), is(1.0));
        assertThat(pp.getPotentialParent("B").getVariable(), is("B"));
        assertThat(pp.getPotentialParent("B").getProbability(), is(2.0 / 3.0));
        assertThat(pp.getPotentialParent("C").getVariable(), is("C"));
        assertThat(pp.getPotentialParent("C").getProbability(), is(1.0 / 3.0));
        
        // test sorting:
        Locale old = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        Assert.assertEquals("PotentialParents for D: [A (100,00%), B (66,67%), C (33,33%)]", pp.toString());
        Locale.setDefault(old);
    }
    
}
