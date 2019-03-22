package net.ssehub.kernel_haven.fe_analysis.arch_components;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.util.List;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.SourceLocation;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityModelDescriptor.Attribute;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * An {@link AnalysisComponent} that creates dummy architecture components based on the location of
 * {@link VariabilityVariable}s. This requires that the {@link VariabilityVariable}s are annotated with source
 * locations. 
 * 
 * @author Adam
 */
public class DummyArchComponentStorageCreator extends AnalysisComponent<ArchComponentStorage> {

    private @NonNull AnalysisComponent<VariabilityModel> varModelInput;
    
    private @NonNull String preferredArch;
    
    /**
     * Creates a new {@link DummyArchComponentStorageCreator}.
     * 
     * @param config The pipeline configuration.
     * @param varModelInput The component to get the {@link VariabilityModel} from.
     */
    public DummyArchComponentStorageCreator(@NonNull Configuration config,
            @NonNull AnalysisComponent<VariabilityModel> varModelInput) {
        super(config);
        
        this.varModelInput = varModelInput;
        
        String arch = config.getValue(DefaultSettings.ARCH);
        if (arch == null) {
            arch = "x86";
        }
        this.preferredArch = arch;
    }

    /**
     * Returns the first two path components (directories) for the given file. If there are less than two, the full
     * file is returned. For example, <code>a/b/c/d -&gt; a/b</code>.
     * 
     * @param in The file to get the first two components from.
     * @return The first two components.
     */
    private static File getAppropriateFile(File in) {
        File result;
        if (in.getParentFile() == null || in.getParentFile().getParentFile() == null) {
            result = in;
        } else {
            result = getAppropriateFile(in.getParentFile());
        }
        return result;
    }
    
    @Override
    protected void execute() {
        VariabilityModel varModel = varModelInput.getNextResult();
        
        if (varModel == null || !varModel.getDescriptor().hasAttribute(Attribute.SOURCE_LOCATIONS)) {
            LOGGER.logError("VariabilityModel is null or has no source locations",
                    "Can't create dummy architecture components");
            return;
        }
        
        ArchComponentStorage storage = new ArchComponentStorage();
        
        for (VariabilityVariable var : varModel.getVariables()) {
            String component = null;
            
            List<SourceLocation> sourceLocations = var.getSourceLocations();
            if (sourceLocations != null && !sourceLocations.isEmpty()) {
                File f = notNull(sourceLocations.get(0)).getSource();
                
                // prefer the arch setting variant if multiple source locations are found
                if (sourceLocations.size() > 1) {
                    for (SourceLocation l : sourceLocations) {
                        if (l.getSource().getPath().contains(preferredArch)) {
                            f = l.getSource();
                            break;
                        }
                    }
                }
                
                if (f.getParentFile() != null) {
                    f = f.getParentFile();
                } else {
                    f = new File(".");
                }
                
                component = getAppropriateFile(f).getPath().replace(File.separatorChar, '/');
            }
            
            if (component != null) {
                storage.setComponent(var.getName(), component);
            }
        }
        
        addResult(storage);
    }

    @Override
    public @NonNull String getResultName() {
        return "Dummy Architecture Components";
    }

}
