package net.ssehub.kernel_haven.fe_analysis.pcs;

import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.and;
import static net.ssehub.kernel_haven.util.logic.FormulaBuilder.or;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.test_utils.AnalysisComponentExecuter;
import net.ssehub.kernel_haven.test_utils.TestConfiguration;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.True;
import net.ssehub.kernel_haven.util.logic.Variable;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Tests the {@link PcReader}.
 * 
 * @author Adam
 */
public class PcReaderTest {

    private static final File TESTDATA = new File("testdata/pcs");
    
    /**
     * Tests reading a valid file.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testReadValid() throws SetUpException {
        List<VariableWithPcs> effects = run(new File(TESTDATA, "valid.csv"));
        
        VariableWithPcs var;
        Iterator<VariableWithPcs> it = effects.iterator();
        Set<@NonNull Formula> pcs = new HashSet<>();

        var = it.next();
        pcs.add(new Variable("VAR_A"));
        assertThat(var.getVariable(), is("VAR_A"));
        assertThat(var.getPcs(), is(pcs));
        
        var = it.next();
        pcs.clear();
        pcs.add(and(or("VAR_A", "VAR_C"), "VAR_B"));
        pcs.add(new Variable("VAR_B"));
        assertThat(var.getVariable(), is("VAR_B"));
        assertThat(var.getPcs(), is(pcs));
        
        var = it.next();
        pcs.clear();
        pcs.add(True.INSTANCE);
        assertThat(var.getVariable(), is("VAR_C"));
        assertThat(var.getPcs(), is(pcs));
        
        assertThat(it.hasNext(), is(false));
    }
    
    /**
     * Tests reading a file with an invalid formula.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testInvalidFormula() throws SetUpException {
        List<VariableWithPcs> effects = run(new File(TESTDATA, "invalid_formula.csv"));
        
        VariableWithPcs var;
        Iterator<VariableWithPcs> it = effects.iterator();
        Set<@NonNull Formula> pcs = new HashSet<>();

        var = it.next();
        pcs.add(new Variable("VAR_A"));
        assertThat(var.getVariable(), is("VAR_A"));
        assertThat(var.getPcs(), is(pcs));
        
        var = it.next();
        pcs.clear();
        pcs.add(True.INSTANCE);
        assertThat(var.getVariable(), is("VAR_C"));
        assertThat(var.getPcs(), is(pcs));
        
        assertThat(it.hasNext(), is(false));
    }
    
    /**
     * Tests reading a file with a missing PC list.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testMissingPcs() throws SetUpException {
        List<VariableWithPcs> effects = run(new File(TESTDATA, "missing_pcs.csv"));
        
        VariableWithPcs var;
        Iterator<VariableWithPcs> it = effects.iterator();
        Set<@NonNull Formula> pcs = new HashSet<>();

        var = it.next();
        pcs.add(new Variable("VAR_A"));
        assertThat(var.getVariable(), is("VAR_A"));
        assertThat(var.getPcs(), is(pcs));
        
        var = it.next();
        pcs.clear();
        pcs.add(True.INSTANCE);
        assertThat(var.getVariable(), is("VAR_C"));
        assertThat(var.getPcs(), is(pcs));
        
        assertThat(it.hasNext(), is(false));
    }
    
    /**
     * Tests reading a file with an invalid list.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testInvalidList() throws SetUpException {
        List<VariableWithPcs> effects = run(new File(TESTDATA, "invalid_list.csv"));
        
        VariableWithPcs var;
        Iterator<VariableWithPcs> it = effects.iterator();
        Set<@NonNull Formula> pcs = new HashSet<>();

        var = it.next();
        pcs.add(new Variable("VAR_A"));
        assertThat(var.getVariable(), is("VAR_A"));
        assertThat(var.getPcs(), is(pcs));
        
        assertThat(it.hasNext(), is(false));
    }
    
    /**
     * Tests reading a valid file with a line that was split up when writing.
     * 
     * @throws SetUpException unwanted.
     */
    @Test
    public void testSplitLongLine() throws SetUpException {
        List<VariableWithPcs> effects = run(new File(TESTDATA, "valid_split.csv"));
        
        VariableWithPcs var;
        Iterator<VariableWithPcs> it = effects.iterator();
        Set<@NonNull Formula> pcs = new HashSet<>();

        var = it.next();
        pcs.add(new Variable("VAR_A"));
        assertThat(var.getVariable(), is("VAR_A"));
        assertThat(var.getPcs(), is(pcs));
        
        var = it.next();
        pcs.clear();
        pcs.add(and(or("VAR_A", "VAR_C"), "VAR_B"));
        pcs.add(new Variable("VAR_B"));
        assertThat(var.getVariable(), is("VAR_B"));
        assertThat(var.getPcs(), is(pcs));
        
        var = it.next();
        pcs.clear();
        pcs.add(True.INSTANCE);
        assertThat(var.getVariable(), is("VAR_C"));
        assertThat(var.getPcs(), is(pcs));
        
        assertThat(it.hasNext(), is(false));
    }
    
    /**
     * Runs the {@link PcReader} on the given input file.
     * 
     * @param file The input file for the {@link PcReader}.
     * 
     * @return The list with the results.
     * 
     * @throws SetUpException If creating the {@link PcReader} fails.
     */
    private @NonNull List<@NonNull VariableWithPcs> run(@NonNull File file) throws SetUpException {
        TestConfiguration config = new TestConfiguration(new Properties());
        config.registerSetting(PcReader.INPUT_FILE_SETTING);
        config.setValue(PcReader.INPUT_FILE_SETTING, file);
        
        List<@NonNull VariableWithPcs> result =
                AnalysisComponentExecuter.executeComponent(PcReader.class, config);
        
        return result;
    }

}
