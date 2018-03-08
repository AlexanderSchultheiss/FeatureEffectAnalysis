package net.ssehub.kernel_haven.fe_analysis.fes;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.io.IOException;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder.VariableWithFeatureEffect;
import net.ssehub.kernel_haven.util.io.ITableCollection;
import net.ssehub.kernel_haven.util.io.ITableReader;
import net.ssehub.kernel_haven.util.io.TableCollectionReaderFactory;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.parser.CStyleBooleanGrammar;
import net.ssehub.kernel_haven.util.logic.parser.ExpressionFormatException;
import net.ssehub.kernel_haven.util.logic.parser.Parser;
import net.ssehub.kernel_haven.util.logic.parser.VariableCache;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.NullHelpers;

/**
 * A component that reads {@link VariableWithFeatureEffect}s from a file specified in the configuration.
 * 
 * @author Adam
 */
public class FeatureEffectReader extends AnalysisComponent<VariableWithFeatureEffect> {

    public static final @NonNull Setting<File> INPUT_FILE_SETTING
            = new Setting<>("analysis.feature_effect.file", Type.PATH, true, null,
                    "A CSV file containing the feature effects to be read by the "
                    + FeatureEffectReader.class.getName());
    
    private File inputFile;
    
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
        
        if (inputFile == null) {
            throw new SetUpException(INPUT_FILE_SETTING.getKey() + " was not specified, it must "
                + "point to input DIMACS file.");
        }
        if (!inputFile.exists()) {
            File srcDir = config.getValue(DefaultSettings.SOURCE_TREE);
            inputFile = new File(srcDir, inputFile.getPath());
            
            if (!inputFile.exists()) {
                throw new SetUpException(INPUT_FILE_SETTING.getKey() + " = "
                    + inputFile.getAbsolutePath() + " does not exist.");
            }
        }
    }

    @Override
    protected void execute() {
        try (ITableCollection collection
                = TableCollectionReaderFactory.INSTANCE.openFile(NullHelpers.notNull(inputFile))) {
            
            String tableName;
            if (collection.getTableNames().size() == 1) {
                // if we just have one sheet / table, use it
                // this is the case for CSV files
                tableName = notNull(collection.getTableNames().iterator().next());
                
            } else {
                // use the table name "Feature Effects"
                tableName = "Feature Effects";
            }
            
            try (ITableReader in = collection.getReader(tableName)) {
                readFile(in);
            }
            
        } catch (IOException e) {
            LOGGER.logException("Can't read input file", e);
        }
    }
    
    /**
     * Reads the file contents.
     * 
     * @param in The reader to use.
     * 
     * @throws IOException If reading the file fails.
     */
    @SuppressWarnings("null")
    private void readFile(@NonNull ITableReader in) throws IOException {
        VariableCache varCache = new VariableCache();
        Parser<@NonNull Formula> parser = new Parser<>(new CStyleBooleanGrammar(varCache));
        
        in.readNextRow(); // skip first line (header)
        
        @NonNull String[] line;
        while ((line = in.readNextRow()) != null) {
            
            if (line.length < 2) {
                LOGGER.logError("Line " + in.getLineNumber() + " in file " + inputFile + " has " + line.length
                        + " columns, instead of 2");
                continue;
            }
            
            // Sometimes an FE is too long to be written into a single cell
            if (line.length > 2) {
                StringBuffer concat = new StringBuffer(line[1]);
                for (int i = 2; i < line.length; i++) {
                    concat.append(line[i]);
                }
                line[1] = concat.toString();
            }
            
            try {
                String varName = line[0].replace("=", "_eq_");
                Formula fe = parser.parse(line[1].replace("=", "_eq_"));
                
                addResult(new VariableWithFeatureEffect(varName, fe));
                
            } catch (ExpressionFormatException e) {
                LOGGER.logException("Can't parse formula in line " + in.getLineNumber() + " in file " + inputFile
                        + ": \"" + line[1] + "\"", e);
            }
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Feature Effects (read from file)";
    }

}
