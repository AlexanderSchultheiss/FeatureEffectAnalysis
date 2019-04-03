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
package net.ssehub.kernel_haven.fe_analysis.arch_components;

import java.io.File;
import java.io.IOException;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.util.io.ITableCollection;
import net.ssehub.kernel_haven.util.io.ITableReader;
import net.ssehub.kernel_haven.util.io.TableCollectionReaderFactory;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * An component for reading architecture components from an input file.
 * 
 * @author Adam
 */
public class ArchComponentReader extends AnalysisComponent<ArchComponentStorage> {

    public static final @NonNull Setting<@NonNull File> INPUT_SETTING
            = new Setting<>("analysis.arch_components_file", Type.FILE,
            true, null, "Defines the input file to read architecture components from. Should be a table "
                    + "(e.g. Excel or CSV file) with variable names in the first column and another column with the "
                    + "header \"Architecture Component\".");
    
    private @NonNull File input;
    
    /**
     * Creates an {@link ArchComponentReader}.
     * 
     * @param config The pipeline configuration.
     * 
     * @throws SetUpException If reading the input file setting fails.
     */
    public ArchComponentReader(@NonNull Configuration config) throws SetUpException {
        super(config);
        
        config.registerSetting(INPUT_SETTING);
        input = config.getValue(INPUT_SETTING);
    }

    /**
     * Returns the header name of the column where architecture components are read from.
     * 
     * @return The column name.
     */
    protected @NonNull String getArchComponentHeader() {
        return "Architecture Component";
    }
    
    /**
     * Checks if the given component name is valid.
     * 
     * @param component The component name to check.
     * 
     * @return Whether the given component name is valid.
     */
    protected boolean isValidComponent(@NonNull String component) {
        return true;
    }
    
    @Override
    protected void execute() {
        
        try (ITableCollection collection = TableCollectionReaderFactory.INSTANCE.openFile(input)) {
            
            if (collection.getTableNames().size() != 1) {
                throw new IOException("Expecting a table collection with exactly one table, got"
                        + collection.getTableNames().size());
            }
            
            try (ITableReader in = collection.getReader(collection.getTableNames().iterator().next())) {
                
                // find index of column "Architecture Component" (getArchComponentHeader())
                String[] header = in.readNextRow();
                if (header == null) {
                    throw new IOException("Expected at least one header row");
                }
                
                int componentIndex;
                for (componentIndex = 1; componentIndex < header.length; componentIndex++) {
                    if (header[componentIndex].equalsIgnoreCase(getArchComponentHeader())) {
                        break;
                    }
                }
                
                if (componentIndex == header.length) {
                    throw new IOException("Couldn't find column  with header \"" + getArchComponentHeader() + "\"");
                }
                
                ArchComponentStorage storage = new ArchComponentStorage();
                
                @NonNull String[] row;
                while ((row = in.readNextRow()) != null) {
                    String variable = row[0];
                    String component = row[componentIndex];
                    
                    if (!component.isEmpty() && isValidComponent(component)) {
                        storage.setComponent(variable, component);
                    }
                }
                
                addResult(storage);
                
            }
            
        } catch (IOException e) {
            LOGGER.logException("Can't read architecture component file " + input, e);
        }
    }

    @Override
    public @NonNull String getResultName() {
        return "Architecture Components";
    }

}
