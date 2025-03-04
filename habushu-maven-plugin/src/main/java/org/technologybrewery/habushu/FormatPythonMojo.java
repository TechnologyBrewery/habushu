package org.technologybrewery.habushu;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.technologybrewery.habushu.exec.AbstractCommandHelper;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;
import org.technologybrewery.habushu.util.TomlUtils;

/**
 * Leverages the ruff formatter package to format both source and test Python
 * directories using Poetry's run command.
 */
@Mojo(name = "format-python", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class FormatPythonMojo extends AbstractHabushuMojo {

    protected static final String FORMATTER_PACKAGE = "ruff";
    protected static final String PYPROJECT_TOML_FORMATTER_HEADER = "[tool.ruff.format]";

    protected static final String DEFAULT_CONFIGS = String.join("",
            "\n[tool.ruff.format]\n",
            "indent-style=\"space\"\n",
            "line-ending=\"lf\"\n",
            "quote-style=\"preserve\"\n"
            );
        
    protected List<String> formatCommands = Arrays.asList("run", FORMATTER_PACKAGE, "format");

    /**
     * Toggle for whether the habushu project should automatically leverage ruff formatting.
     */
   @Parameter(property = "habushu.useFormatter", defaultValue = "true")
   protected boolean useFormatter = true;

    @Override
    public void doExecute() throws HabushuException {

        if (!useFormatter) {
            getLog().info(
                    String.format("Formatting disabled. Continuing without using %s to format...", FORMATTER_PACKAGE));
            return;
        }

        addDefaultFormatConfigsIfNotPreset(getPyProjectTomlFile());

        List<String> directoriesToFormat = new ArrayList<>();
        if (this.sourceDirectory.exists()) {
            directoriesToFormat.add(getCanonicalPathForFile(sourceDirectory));
        }
        if (this.testDirectory.exists()) {
            directoriesToFormat.add(getCanonicalPathForFile(testDirectory));
        }

        if (directoriesToFormat.isEmpty()) {
            getLog().warn(String.format("Neither configured source (%s) nor test (%s) directories exist - skipping...",
                    sourceDirectory, testDirectory));
        }

        AbstractCommandHelper helper = getCommandHelper();
        downloadFormatterIfNotPresent(helper);

        List<String> executeFormatterArgs = new ArrayList<>();
        executeFormatterArgs.addAll(formatCommands);
        executeFormatterArgs.addAll(directoriesToFormat);

        getLog().info(
                String.format("Formatting configured source and test directories using %s...", FORMATTER_PACKAGE));
        helper.executeAndLogOutput(executeFormatterArgs);
    }

    protected void downloadFormatterIfNotPresent(AbstractCommandHelper helper) {
        if (!helper.isDependencyInstalled(FORMATTER_PACKAGE)) {
            getLog().info(
                    String.format("%s dependency not specified in pyproject.toml - installing now...", FORMATTER_PACKAGE));
            helper.installDevelopmentDependency(FORMATTER_PACKAGE);
        }
    }

    protected void addDefaultFormatConfigsIfNotPreset(File pyprojectToml) throws HabushuException {
        try {
            if (pyprojectToml.exists()) {
                if(!containsRuffFormatterConfigs(pyprojectToml)) {
                    Files.write(
                            pyprojectToml.toPath(),
                            DEFAULT_CONFIGS.getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.APPEND
                    );
                }
            } else{
                throw new HabushuException(String.format("Config file %s not found", TomlUtils.PYPROJECT_TOML));
            }
        } catch(IOException e){
            getLog().error("Error adding default ruff configurations to pyproject.toml file");
            throw new HabushuException(e);
        }
    }

    protected boolean containsRuffFormatterConfigs(File pyprojectToml) throws IOException {
        Stream<String> configsStream = Files.lines(pyprojectToml.toPath());
        boolean configsFound = configsStream
                .anyMatch(line -> line.contains(PYPROJECT_TOML_FORMATTER_HEADER));
        configsStream.close();
        return configsFound;
    }
}