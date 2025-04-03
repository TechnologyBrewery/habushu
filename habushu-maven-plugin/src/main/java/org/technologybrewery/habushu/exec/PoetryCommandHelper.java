package org.technologybrewery.habushu.exec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.habushu.HabushuException;

/**
 * Facilitates the execution of Poetry commands.
 */
public class PoetryCommandHelper extends AbstractCommandHelper {

    private static final String POETRY_COMMAND = "poetry";
    private static final String BREAKING_POETRY_VERSION = "2.0.0";
    private static final Logger logger = LoggerFactory.getLogger(PoetryCommandHelper.class);

    public static final String VERSION_DELIMITER = "@";
    private static final int EXIT_SUCCESS = 0;

    private static final String EXTRACT_VERSION_REGEX = "[^0-9\\.]";

    protected volatile String showPluginsResult;
    protected static final Object pluginSystemLock = new Object();


    public PoetryCommandHelper(File workingDirectory) {
        super(workingDirectory, POETRY_COMMAND);
        this.workingDirectory = workingDirectory;
    }

    /**
     * Returns a {@link Boolean} and {@link String} {@link Pair} indicating whether
     * Poetry is installed and if so, the version of Poetry that is installed. If
     * Poetry is not installed, the returned {@link String} part of the {@link Pair}
     * will be {@code null}.
     *
     * @return Pair of installed and version
     */
    public Pair<Boolean, String> getIsPoetryInstalledAndVersion() {
        try {
            ProcessExecutor executor = createPackageManagerExecutor(List.of("--version"));
            String versionResult = executor.executeAndGetResult(logger);

            // Extracts version number from output, whether it's "Poetry version 1.1.15" or
            // "Poetry (version 1.2.1)"
            String version = versionResult.replaceAll(EXTRACT_VERSION_REGEX, "");
            return new ImmutablePair<>(true, version);
        } catch (Throwable e) {
            return new ImmutablePair<>(false, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDependencyInstalled(String packageName) {
        try {
            String result = execute(Arrays.asList("show", packageName)).trim();
            return !result.isEmpty();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installDevelopmentDependency(String packageName) {
        execute(Arrays.asList("add", packageName, "--group", "dev"));
    }

    /**
     * Executes a Poetry command with the given arguments and logs a warning message
     * if the command has not yet completed after the specified timeout period. This
     * may be useful for providing input to developers when certain Poetry commands
     * are running for longer than expected and may need to be manually halted due
     * to cache-related issues.<br>
     * <b>NOTE:</b>The executed Poetry command will *not* be halted nor terminated
     * when the timeout expires. After the timeout expires, this method will
     * continue to wait until underlying Poetry command completes.
     *
     * @param arguments list of arguments for poetry commands
     * @param timeout time durations
     * @param timeUnit granularity for the time durations
     * @return execution value
     */
    public Integer executePoetryCommandAndLogAfterTimeout(List<String> arguments, int timeout, TimeUnit timeUnit) {
        return executeAndLogAfterTimeout(arguments, timeout, timeUnit,
                "poetry cache clear . --all");
    }

    /**
     * Installs a Poetry plugin with the given name. Poetry plugins install into Poetry's `pyproject.toml` file
     * directly.  As such, it can lead to threading issues without proper care.  We protect against this scenario via
     * double-checked locking execution of the `poetry self add <plugin>` call and avoidance of the add altogether if
     * the plugin already exists.
     *
     * @param name name of the plugin to install
     * @return execution value
     */
    public int installPoetryPlugin(String name) {
        int result = EXIT_SUCCESS;
        if (pluginNeedsInstalling(name)) {
            result = performInstallPoetryPlugin(name);
        }

        return result;
    }

    private int performInstallPoetryPlugin(String name) {
        int result = EXIT_SUCCESS;

        List<String> args = new ArrayList<>();
        args.add("self");
        args.add("add");
        args.add(name);
        synchronized (pluginSystemLock) {
            if (pluginNeedsInstalling(name)) {
                result = this.executeAndLogOutput(args);

                // un-cache known set of plugins:
                showPluginsResult = null;
            }
        }

        return result;
    }

    private boolean pluginNeedsInstalling(String name) {
        executeShowPlugins();

        String pluginNameWithoutVersion = name;
        if (name.contains(VERSION_DELIMITER)) {
            pluginNameWithoutVersion = pluginNameWithoutVersion.substring(0, name.indexOf(VERSION_DELIMITER));
        }

        return !showPluginsResult.contains(pluginNameWithoutVersion);
    }

    private void executeShowPlugins() {
        if (StringUtils.isBlank(showPluginsResult)) {
            List<String> args = new ArrayList<>();
            args.add("self");
            args.add("show");
            args.add("plugins");
            try {
                if (StringUtils.isBlank(showPluginsResult)) {
                    showPluginsResult = this.execute(args);
                }
            } catch (HabushuException e) {
                logger.info("Plugin status could not be determined. This is normally a race condition on another"
                        + " thread touching poetry's underlying pyproject.toml, trying one more time...");
                // let's be more careful round two and assume that there was a plugin being installed, so we need to
                // respect that the lock could be engaged and synchronize on it to ensure ordered execution:
                synchronized (pluginSystemLock) {
                    if (StringUtils.isBlank(showPluginsResult)) {
                        showPluginsResult = this.execute(args);
                    }
                }

            }
        }
    }

    /**
     * Returns a {@link boolean} indicating whether the specified Poetry version is at least the minimum required
     * version.
     *
     * @return boolean true if the version is equal to or greater than the minimum version
     */
    public boolean isPoetryVersionAtLeastMinimumVersion(){
        String poetryVersion = getIsPoetryInstalledAndVersion().getRight();
        DefaultArtifactVersion currentVersion = new DefaultArtifactVersion(poetryVersion);
        DefaultArtifactVersion minimumVersion = new DefaultArtifactVersion(BREAKING_POETRY_VERSION);
        return currentVersion.compareTo(minimumVersion) >= 0;
    }


    public List<String> createLockCommand() {
        return createLockCommand(true);
    }

    public List<String> createLockCommand(boolean skipPoetryLockUpdate) {
        List<String> arguments = new ArrayList<>();
        arguments.add("lock");

        if (isPoetryVersionAtLeastMinimumVersion()) {
            if (!skipPoetryLockUpdate) {
                arguments.add("--regenerate");
            }
        } else {
            if (skipPoetryLockUpdate) {
                arguments.add("--no-update");
            }
        }
        return arguments;
    }

    public List<String> createEnvListFullPathCommand() {
        List<String> arguments = new ArrayList<>();
        arguments.add("env");
        arguments.add("list");
        arguments.add("--full-path");

        return arguments;

    }

    public List<String> createInstallCommand(boolean forceSync) {
        List<String> arguments = new ArrayList<>();

        if(!forceSync){
            arguments.add("install");
        } else if (isPoetryVersionAtLeastMinimumVersion()) {
            arguments.add("sync");
        } else {
            arguments.add("install");
            arguments.add("--sync");
        }
        return arguments;
    }

    public List<String> createUsePyenvCommand(){
        List<String> arguments = new ArrayList<>();
        arguments.add("config");
        arguments.add("--local");

        if(isPoetryVersionAtLeastMinimumVersion()){
            arguments.add("virtualenvs.use-poetry-python");
            arguments.add("false");
        } else {
            arguments.add("virtualenvs.prefer-active-python");
            arguments.add("true");
        }
        return arguments;
    }
}
