/*
 * Copyright 2017-2019 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.kernel_haven.fe_analysis.pcs;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.fe_analysis.Settings;
import net.ssehub.kernel_haven.fe_analysis.Settings.SimplificationType;
import net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder.VariableWithPcs;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.io.ITableCollection;
import net.ssehub.kernel_haven.util.io.ITableReader;
import net.ssehub.kernel_haven.util.io.TableCollectionReaderFactory;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.FormulaSimplifier;
import net.ssehub.kernel_haven.util.logic.parser.CStyleBooleanGrammar;
import net.ssehub.kernel_haven.util.logic.parser.ExpressionFormatException;
import net.ssehub.kernel_haven.util.logic.parser.Parser;
import net.ssehub.kernel_haven.util.logic.parser.VariableCache;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * A component that reads {@link VariableWithPcs} from a file specified in the configuration.
 * 
 * @author Adam
 */
public class PcReader extends AnalysisComponent<VariableWithPcs> {

    public static final @NonNull Setting<@NonNull File> INPUT_FILE_SETTING
        = new Setting<>("analysis.presence_conditions.file", Type.FILE, true, null,
            "A file containing the presence conditions to be read by the "
            + PcReader.class.getName());

    private @NonNull File inputFile;
    
    private @NonNull VariableCache varCache;
    
    private @NonNull Parser<@NonNull Formula> parser;
    
    private boolean simplify;
    
    /**
     * Creates this component. No input required since the input file is read from the configuration.
     * 
     * @param config The pipeline configuration.
     * 
     * @throws SetUpException If reading the configuration for the input file fails.
     */
    public PcReader(@NonNull Configuration config) throws SetUpException {
        super(config);
        
        config.registerSetting(INPUT_FILE_SETTING);
        this.inputFile = config.getValue(INPUT_FILE_SETTING);
        
        this.varCache = new VariableCache();
        this.parser = new Parser<>(new CStyleBooleanGrammar(varCache));
        
        config.registerSetting(Settings.SIMPLIFIY);
        this.simplify = config.getValue(Settings.SIMPLIFIY) == SimplificationType.PRESENCE_CONDITIONS;
    }

    @Override
    protected void execute() {
        try (ITableCollection collection
                = TableCollectionReaderFactory.INSTANCE.openFile(inputFile)) {
            
            String tableName;
            if (collection.getTableNames().size() == 1) {
                // if we just have one sheet / table, use it
                // this is the case for CSV files
                tableName = notNull(collection.getTableNames().iterator().next());
                
            } else {
                // use the table name "Presence Conditions"
                tableName = "Presence Conditions";
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
    private void readFile(@NonNull ITableReader in) throws IOException {
        
        in.readNextRow(); // skip first line (header)
        
        @NonNull String[] line;
        while ((line = in.readNextRow()) != null) {
            
            if (line.length < 2) {
                LOGGER.logError("Line " + in.getLineNumber() + " in file " + inputFile + " has " + line.length
                        + " columns, instead of 2");
                continue;
            }
            
            // Sometimes an entry is too long to be written into a single cell
            if (line.length > 2) {
                StringBuilder concat = new StringBuilder(line[1]);
                for (int i = 2; i < line.length; i++) {
                    concat.append(line[i]);
                }
                line[1] = notNull(concat.toString());
            }
            
            try {
                addResult(readSingleLine(line[0], line[1]));
            } catch (FormatException e) {
                LOGGER.logException("Line " + in.getLineNumber() + " can not be read", e);
            }
            
        }
    }
    
    /**
     * Reads a single line from the sheet.
     * 
     * @param name The name of the variable (first column).
     * @param pcList The list of presence conditions (second column).
     * 
     * @return The result of parsing the line.
     * 
     * @throws FormatException If the presence condition list has an invalid format.
     */
    private @NonNull VariableWithPcs readSingleLine(@NonNull String name, @NonNull String pcList)
            throws FormatException {

        if (!pcList.startsWith("[") || !pcList.endsWith("]")) {
            throw new FormatException("List does not start with '[' or does not end with ']'");
        }
        
        @SuppressWarnings("null") // String.split() returns @NonNull String @NonNull []
        @NonNull String[] pcStrs = pcList.substring(1, pcList.length() - 1).split(",");
        
        Set<@NonNull Formula> pcs = new HashSet<>((int) (pcStrs.length * 1.5));
        
        for (String pcStr : pcStrs) {
            try {
                Formula pc = parser.parse(pcStr);
                
                if (simplify) {
                    pc = FormulaSimplifier.simplify(pc);
                }
                
                pcs.add(pc);
                
            } catch (ExpressionFormatException e) {
                throw new FormatException(e);
            }
        }
        
        return new VariableWithPcs(name, pcs);
    }

    @Override
    public @NonNull String getResultName() {
        return "Presence Conditions (read from file)";
    }

}
