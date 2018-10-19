package net.ssehub.kernel_haven.fe_analysis.config_relevancy;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.fe_analysis.config_relevancy.VariableRelevance.Relevance;
import net.ssehub.kernel_haven.fe_analysis.fes.FeAggregator;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.ProgressLogger;
import net.ssehub.kernel_haven.util.io.csv.CsvReader;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.FormulaEvaluator;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Checks feature effects of variables against a given product configuration. This requires that variable names are
 * Boolean, i.e., do not contain pseudo-sat formulas. For this, you may require the {@link FeAggregator}
 * to normalize pseudo-sat input. The feature effect themselves, however, are treated as non-Boolean. This means, that
 * variable names in the feature effect formulas can have the format "VAR=2"; a product configuration
 * (SPL configuration) that is checked is a mapping <code>variable name -&gt; integer</code>.
 * <p>
 * Feature effects are not allowed to contain &gt;, &lt;, &gt;=, &lt;=, != comparisons
 * in their variable names. They must only contains variable names in the form of "VAR" or "VAR=4".
 * </p>
 * 
 * @author Adam
 */
public class ConfigRelevancyChecker extends AnalysisComponent<VariableRelevance> {

    public static final @NonNull Setting<@NonNull File> INPUT_FILE_PROPERTY
        = new Setting<>("analysis.config_relevancy_checker.configuration_file", Type.FILE, true, null,
            "Location an historical SPL configuration file, which should be analyses w.r.t."
            + "the relevance of the configured variables.");
    
    private @NonNull AnalysisComponent<VariableWithFeatureEffect> featureEffectFinder;
    
    private @NonNull File inputFile;
    
    /**
     * Sole constructor for this class.
     * 
     * @param config The pipeline configuration.
     * @param featureEffectFinder Probably {@link FeatureEffectFinder} or {@link FeAggregator}.
     * 
     * @throws SetUpException If configuration fails, e.g., if no SPL configuration was passed to this analysis.
     */
    public ConfigRelevancyChecker(@NonNull Configuration config,
            @NonNull AnalysisComponent<VariableWithFeatureEffect> featureEffectFinder) throws SetUpException {
        
        super(config);
        this.featureEffectFinder = featureEffectFinder;
        
        config.registerSetting(INPUT_FILE_PROPERTY);
        inputFile = config.getValue(INPUT_FILE_PROPERTY);
    }
    
    /**
     * Loads the historical SPL configuration file.
     * 
     * @param inputFile The file to load the SPL configuration from.
     * @return A map containing (name of a variable, configured integer value).
     * 
     * @throws IOException If the file could not be read.
     */
    protected @NonNull Map<String, Integer> loadFile(@NonNull File inputFile) throws IOException {
        Map<String, Integer> variableValues = new HashMap<>();
        
        final int nameIndex = 0;
        final int valueIndex = 1;
        
        try (CsvReader in = new CsvReader(new FileInputStream(inputFile))) {
            
            String[] line;
            while ((line = in.readNextRow()) != null) {
                try {
                    variableValues.put(line[nameIndex], Integer.parseInt(line[valueIndex]));
                } catch (NumberFormatException e) {
                    LOGGER.logWarning("Invalid integer value in " + inputFile + " at line "
                            + in.getLineNumber() + ": " + line[valueIndex]);
                }
            }
            
        }
        
        return variableValues;
    }
    
    /**
     * Solves the given constraint while using the configuration of the map.
     * 
     * @param featureEffect The constraint to solve.
     * @param variableValues The configuration to use.
     * 
     * @return 
     * <ul>
     *     <li><code>true</code>: Constraint was fulfilled</li>
     *     <li><code>false</code>: Constraint was <b>not</b> fulfilled</li>
     *     <li><code>null</code>: Constraint could not be solved since come values were missing</li>
     * </ul>
     */
    private @Nullable Boolean evaluateFeatureEffect(@NonNull Formula featureEffect,
            @NonNull Map<String, Integer> variableValues) {
        
        VariableFinder varFinder = new VariableFinder();
        varFinder.visit(featureEffect);
        
        Map<String, Boolean> variableMapping = new HashMap<>();
        
        List<String> variables = varFinder.getVariableNames();
        for (String variable : variables) {
            if (variable.contains(">") || variable.contains("<") || variable.contains("!")) {
                LOGGER.logWarning("Variable name contains operators that this component cannot handle: "
                    + variable, "Setting its value to undefined");
                
                continue;
            }
            
            String baseName = variable;
            int equalIndex = baseName.indexOf('=');
            
            if (equalIndex != -1) {
                // a variable with "a = ..." is true, if its right part is an integer equal to the configured value
                baseName = baseName.substring(0, equalIndex);
                try  {
                    int equalityValue = Integer.parseInt(variable.substring(equalIndex + 1));
                    Integer configuredValue = variableValues.get(baseName);
                    if (configuredValue != null) {
                        variableMapping.put(variable, equalityValue == configuredValue);
                    }
                
                } catch (NumberFormatException e) {
                    LOGGER.logExceptionWarning("Can't parse right side of variable " + variable
                            + "; setting its value to undefined", e);
                }
                
                
            } else {
                // a variable without = is true, if it has _some_ value
                // TODO: should 0 be treated like false here?
                
                if (variableValues.get(baseName) != null) {
                    variableMapping.put(baseName, true);
                }
            }
        }
        
        return new FormulaEvaluator(variableMapping).visit(featureEffect);
    }

    @Override
    protected void execute() {
        ProgressLogger progress = new ProgressLogger(notNull(getClass().getSimpleName()));
        
        try {
            Map<String, Integer> variableValues = loadFile(inputFile);
            
            VariableWithFeatureEffect var;
            while ((var = featureEffectFinder.getNextResult()) != null) {
                
                if (var.getVariable().contains("=")) {
                    LOGGER.logWarning("Variable name still contains a '=': " + var.getVariable(),
                            "You probably want to run the " + FeAggregator.class.getCanonicalName()
                            + " component before this one");
                }
                
                Boolean feEvaluation = evaluateFeatureEffect(var.getFeatureEffect(), variableValues);
                Integer value = variableValues.get(var.getVariable());
                
                Relevance relevance = Relevance.UNKOWN;
                if (feEvaluation == Boolean.FALSE && value == null) {
                    relevance = Relevance.NOT_SET_AND_IRRELEVANT;
                        
                } else if (feEvaluation == Boolean.FALSE && value != null) {
                    relevance = Relevance.SET_AND_IRRELEVANT;
                        
                } else if (feEvaluation == Boolean.TRUE && value == null) {
                    relevance = Relevance.NOT_SET_AND_RELEVANT;
                    
                } else if (feEvaluation == Boolean.TRUE && value != null) {
                    relevance = Relevance.SET_AND_RELEVANT;
                }
                
                VariableRelevance varRelevance = new VariableRelevance(var.getVariable(), relevance,
                        var.getFeatureEffect(), value);
                
                addResult(varRelevance);
                
                progress.processedOne();
            }
            
        } catch (IOException e) {
            LOGGER.logException("Can't read file with product configuration", e);
        } finally {
            progress.close();
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Configuration Relevancy";
    }

}
