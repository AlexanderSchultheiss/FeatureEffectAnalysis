package net.ssehub.kernel_haven.fe_analysis.fes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.io.csv.CsvReader;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.parser.CStyleBooleanGrammar;
import net.ssehub.kernel_haven.util.logic.parser.ExpressionFormatException;
import net.ssehub.kernel_haven.util.logic.parser.Parser;
import net.ssehub.kernel_haven.util.logic.parser.VariableCache;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A component that reads {@link VariableWithFeatureEffect}s from a file specified in the configuration.
 * 
 * @author Adam
 */
public class FeatureEffectReader extends AnalysisComponent<VariableWithFeatureEffect> {

    public static final @NonNull Setting<@NonNull File> INPUT_FILE_SETTING
            = new Setting<>("analysis.feature_effect_file", Type.FILE, true, null,
                    "A CSV file containing the feature effects to be read by the "
                    + FeatureEffectReader.class.getName());
    
    private @NonNull File inputFile;
    
    /**
     * Creates this component.
     * 
     * @param config The pipeline configuration.
     * 
     * @throws SetUpException If reading the configuration for the input file fails.
     */
    public FeatureEffectReader(@NonNull Configuration config) throws SetUpException {
        super(config);
        
        config.registerSetting(INPUT_FILE_SETTING);
        this.inputFile = config.getValue(INPUT_FILE_SETTING);
    }

    @Override
    protected void execute() {
        VariableCache varCache = new VariableCache();
        Parser<@NonNull Formula> parser = new Parser<>(new CStyleBooleanGrammar(varCache));
        
        try (CsvReader in = new CsvReader(new FileInputStream(inputFile))) {
            
            in.readNextRow(); // skip first line (header)
            
            @NonNull String[] line;
            while ((line = in.readNextRow()) != null) {
                
                if (line.length != 2) {
                    LOGGER.logError("Line " + in.getLineNumber() + " in file " + inputFile + " has " + line.length
                            + " columns, instead of 2");
                    continue;
                }
                
                try {
                    String varName = line[0];
                    Formula fe = parser.parse(line[1]);
                    
                    addResult(new VariableWithFeatureEffect(varName, fe));
                    
                } catch (ExpressionFormatException e) {
                    LOGGER.logException("Can't parse formula in line " + in.getLineNumber() + " in file " + inputFile
                            + ": \"" + line[1] + "\"", e);
                }
                
            }
            
        } catch (IOException e) {
            LOGGER.logException("Can't read input file", e);
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Feature Effects (read from file)";
    }

}
