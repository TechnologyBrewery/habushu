package org.technologybrewery.habushu.util;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.HabushuException;
import org.technologybrewery.habushu.PythonPackageAndDependencyManagerFactory;
import org.technologybrewery.habushu.PythonPackageAndDependencyManagerSetup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains utility functionality for Habushu, including bash script execution
 * and accessing username/password credentials that may be defined within the
 * relevant settings.xml configuration.
 */
public final class HabushuUtil {

    private static final Logger logger = LoggerFactory.getLogger(HabushuUtil.class);

    /**
     * Specifies the semver compliant requirement for the default version of Python that
     * must be installed and available for Habushu to use.
     */
    public static final String PYTHON_DEFAULT_VERSION_REQUIREMENT = "3.12.9";

    public static final String HABUSHU = "habushu";

    public static final String SNAPSHOT = "-SNAPSHOT";

    /**
     * Pattern to match pre-release versions in both SemVer 1 and SemVer 2 formats.
     * Supports versions with 3+ numeric segments (e.g., 1.2.3, 1.2.3.4) and optional SNAPSHOT suffix.
     * Matches: 1.2.3-rc.4, 1.2.3-RC4, 1.2.3.4-alpha1, 1.2.3-beta.2-SNAPSHOT
     * Group 1: base version (e.g., "1.2.3" or "1.2.3.4")
     * Group 2: qualifier (e.g., "rc", "alpha", "beta")
     * Group 3: pre-release number (e.g., "4")
     * Group 4: optional SNAPSHOT suffix (e.g., "-SNAPSHOT" or null)
     */
    public static final Pattern PRERELEASE_PATTERN = Pattern.compile(
            "^(\\d+(?:\\.\\d+){2,})-(rc|alpha|beta)\\.?(\\d+)(-SNAPSHOT)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Mapping of pre-release qualifiers to their PEP-440 normalized short forms.
     * See https://peps.python.org/pep-0440/#pre-releases
     */
    public static final Map<String, String> PEP440_QUALIFIER_MAP;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("alpha", "a");
        map.put("beta", "b");
        map.put("rc", "rc");
        PEP440_QUALIFIER_MAP = Collections.unmodifiableMap(map);
    }

    private HabushuUtil() {
    }

    /**
     * Normalizes a pre-release qualifier to its PEP-440 short form.
     *
     * @param qualifier the qualifier to normalize (e.g., "alpha", "BETA", "rc")
     * @return the PEP-440 normalized form (e.g., "a", "b", "rc"), or the lowercase
     *         qualifier if no mapping exists
     */
    public static String normalizePep440Qualifier(String qualifier) {
        String lowerQualifier = qualifier.toLowerCase();
        return PEP440_QUALIFIER_MAP.getOrDefault(lowerQualifier, lowerQualifier);
    }

    /**
     * Run the bash script found at the given location without parameters.
     *
     * @param bashScriptPath absolute path to the bash script
     */
    public static void runBashScript(String bashScriptPath) {
        runBashScript(bashScriptPath, null, true);
    }

    /**
     * Run the bash script found at the given location with the provided parameters.
     *
     * @param bashScriptPath absolute path to the bash script
     * @param parameters     script parameters
     * @param debug          true to log script output as DEBUG, otherwise logged as
     *                       INFO
     */
    public static void runBashScript(String bashScriptPath, String[] parameters, boolean debug) {
        logger.debug("Running bash script located at {}.", bashScriptPath);

        try {
            String[] command;
            if (parameters != null && parameters.length > 0) {
                command = new String[parameters.length + 1];

                for (int i = 0; i < parameters.length; i++) {
                    command[i + 1] = parameters[i];
                }
            } else {
                command = new String[1];
            }
            command[0] = bashScriptPath;

            Process process = Runtime.getRuntime().exec(command);

            StringBuilder output = new StringBuilder();
            String line;

            BufferedReader stdInReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = stdInReader.readLine()) != null) {
                output.append(line + "\n");
            }

            BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = stdErrReader.readLine()) != null) {
                output.append(line + "\n");
            }

            if (debug) {
                logger.debug(output.toString());
            } else {
                logger.info(output.toString());
            }

            int exitVal = process.waitFor();
            if (exitVal != 0) {
                throw new HabushuException("Error encountered when running bash script located at " + bashScriptPath
                    + "\n    Can run maven build with -X to see the output of the failed script.");
            }
        } catch (IOException | InterruptedException e) {
            throw new HabushuException("Could not run bash script.", e);
        }
    }

    /**
     * Writes a given list of lines to the file located at the provided file path.
     *
     * @param commands the newline-delineated list of String file lines
     * @param filePath the path to the file
     */
    public static void writeLinesToFile(String commands, String filePath) {
        logger.debug("Writing lines to file located at {}.", filePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(commands);
        } catch (IOException e) {
            throw new HabushuException("Could not write to file.", e);
        }
    }

    /**
     * Writes the given content to the given file
     * @param targetFile The file to write content to
     * @param fileContent The content to write to file
     * @throws IOException
     */
    public static void writeFile(File targetFile, String fileContent) throws IOException {
        if (fileContent != null) {
            try (Writer writer = new FileWriter(targetFile)) {
                writer.write(fileContent);
            }
        }
    }

    /**
     * Creates a new file at the targeted file location and gives full file
     * permissions to the current user.
     *
     * @param newFile the file location
     */
    public static void createFileAndGivePermissions(File newFile) {
        logger.debug("Creating new file at {}.", newFile.getAbsolutePath());

        newFile = new File(newFile.getAbsolutePath());

        if (!newFile.exists()) {
            try {
                newFile.createNewFile();
            } catch (IOException e) {
                throw new HabushuException("Could not create new file.", e);
            }
        }

        giveFullFilePermissions(newFile.getAbsolutePath());
    }

    /**
     * Gives full read, write, and execute permissions to a file.
     *
     * @param filePath the path to the file
     */
    public static void giveFullFilePermissions(String filePath) {
        File file = new File(filePath);

        if (file.exists()) {
            file.setExecutable(true, false);
            file.setReadable(true, false);
            file.setWritable(true, false);
        }
    }

    /**
     * Copies specified file into specified path.
     *
     * @param sourceFilePath the path to the file to copy
     * @param destinationFilePath the path to where the new copy should be created
     */
    public static void copyFile(String sourceFilePath, String destinationFilePath) {
        try{
            File sourceFile = new File(sourceFilePath);
            File destinationFile = new File(destinationFilePath);
            FileUtils.copyFile(sourceFile, destinationFile);
        } catch(IOException ioe){
            throw new HabushuException("Could not copy the file ["+ sourceFilePath +"] to [" + destinationFilePath +"]!", ioe);
        }

    }

    /**
     * Returns the full path for a .venv in-project virtual environment.
     *
     * @param workingDirectory the base directory of the current project
     * @return the current project's in-project virtual environment path
     */
    public static String getInProjectVirtualEnvironmentPath(File workingDirectory) {
        return workingDirectory.getAbsolutePath() + "/.venv";
    }

    /**
     * Removes (Activated) from path in 1.3.x and higher versions.
     *
     * @param virtualEnvFullPath path to clean
     * @return cleaned path
     */
    public static String getCleanVirtualEnvironmentPath(String virtualEnvFullPath) {
        return StringUtils.replace(virtualEnvFullPath, " (Activated)", StringUtils.EMPTY);

    }

    /**
     * Determines which package manager should be used in a module, either Poetry or uv
     * @param pythonPackageAndDependencyManager
     * @param pythonVersion
     * @param isPythonVersionConfigurationSet
     * @param defaultPythonStrategy
     * @param baseDir
     * @param rewriteLocalPathDepsInArchives
     * @param log
     * @param usePyenv
     * @param patchInstallScript
     * @param poetryMonorepoDependencyPluginVersion
     * @return
     * @throws MojoExecutionException
     */
    public static PythonPackageAndDependencyManagerSetup getPythonPackageAndDependencyManagerSetup(
            PackageManager pythonPackageAndDependencyManager, String pythonVersion, boolean isPythonVersionConfigurationSet,
            String defaultPythonStrategy, File baseDir, boolean rewriteLocalPathDepsInArchives, Log log,
            Boolean usePyenv, File patchInstallScript, String poetryMonorepoDependencyPluginVersion) {
        // Set the poetry-based parameters to null
        if (pythonPackageAndDependencyManager == PackageManager.UV) {
            usePyenv = null;
            patchInstallScript = null;
        }

        return PythonPackageAndDependencyManagerFactory.createPythonPackageAndDependencyManagerSetup(
                pythonVersion, isPythonVersionConfigurationSet, defaultPythonStrategy, baseDir, rewriteLocalPathDepsInArchives, log,
                pythonPackageAndDependencyManager, usePyenv, patchInstallScript, poetryMonorepoDependencyPluginVersion);
    }

    /**
     * Finds and returns Python Package Manager.
     *
     * @return PackageManager returns which package manager Habushu uses based on build-backend.
     */
    public static PackageManager checkPythonPackageManager(File pyProjectTomlFile) {
        String buildBackend;
        try (FileConfig toml = FileConfig.of(pyProjectTomlFile)) {
            toml.load();
            buildBackend = toml.get("build-system.build-backend");
        } catch (ParsingException e) {
            throw new HabushuException("Could not parse pyproject.toml file: " + pyProjectTomlFile.getAbsolutePath(), e);
        }

        if (buildBackend != null && buildBackend.contains("poetry")) {
            return PackageManager.POETRY;
        } else {
            return PackageManager.UV;
        }
    }

    /**
     * Finds and returns the value for the provided environmentVariable
     * @param environmentVariable an environment variable
     * @return the value of the environment variable
     */
    public static String getEnvironmentVariable(String environmentVariable){
        return System.getenv(environmentVariable);
    }

    /**
     * Finds and returns the path the user's home directory
     * @return the user's home directory file path
     */
    public static String getHomeDirectory() { 
        return System.getProperty("user.home");
    }

    /**
     * Determine if the user is using a zsh or bash config file on his/her machine
     * @return the path to the appropriate config file
     */
    public static File getShellConfigFile() {
        String shell = getEnvironmentVariable("SHELL");
        String homeDir = getHomeDirectory();
        File shellConfigFile = null;
        if (shell.contains("zsh")) { 
            shellConfigFile = new File(homeDir + "/.zshrc");
        } else if  (shell.contains("bash")) { 
            shellConfigFile = new File(homeDir + "/.bashrc");
        }
        return shellConfigFile;
    }


    /**
     * Finds current Python Package Manager and return whether it is UV.
     *
     * @return true if current Package Manager is UV.
     */
    public static boolean isCurrentPackageManagerUv(File pyProjectTomlFile) {
        return checkPythonPackageManager(pyProjectTomlFile)  == PackageManager.UV;
    }

    /**
     * Returns whether the given POM version is a SNAPSHOT version.
     *
     * @param pomVersion
     * @return
     */
    public static boolean isPomVersionSnapshot(String pomVersion) {
        return pomVersion.endsWith(SNAPSHOT);
    }


    public static String replaceSnapshotWithDev(String pomVersion) {
        return pomVersion.substring(0, pomVersion.indexOf(SNAPSHOT)) + ".dev";
    }

    /**
     * Gets the PEP-440 compliant Python package version associated with the given
     * POM version.
     * <p>
     * <b>Pre-release versions</b> ({@code rc}, {@code alpha}, {@code beta}) are normalized
     * to PEP-440 format. Both SemVer 1 ({@code 1.2.3-RC4}) and SemVer 2 ({@code 1.2.3-rc.4})
     * formats are supported, with any number of version segments (e.g., {@code 1.2.3.4-rc1}).
     * Qualifiers are normalized to their PEP-440 short forms:
     * <ul>
     *   <li>{@code alpha} → {@code a} (e.g., {@code 1.2.3-alpha1} → {@code 1.2.3a1})</li>
     *   <li>{@code beta} → {@code b} (e.g., {@code 1.2.3-beta2} → {@code 1.2.3b2})</li>
     *   <li>{@code rc} → {@code rc} (e.g., {@code 1.2.3-RC4} → {@code 1.2.3rc4})</li>
     * </ul>
     * <p>
     * <b>SNAPSHOT versions</b> are converted into their corresponding developmental release
     * version, with its numeric component optionally included based on the given
     * {@code addSnapshotNumber} and {@code snapshotNumberDateFormatPattern} parameters.
     * For example, given the POM version of {@code 1.2.3-SNAPSHOT}, a Python package version
     * of {@code 1.2.3.dev} will be returned if {@code addSnapshotNumber} is false. If
     * {@code addSnapshotNumber} is true, the numeric component will be added and defaults
     * to the number of seconds from the epoch (i.e. {@code 1.2.3.dev1658238063}). The format
     * of the snapshot number may be modified by providing a date format pattern (i.e.
     * "YYYYMMddHHmm" would yield {@code 1.2.3.dev202207191002}).
     * <p>
     * <b>Pre-release with SNAPSHOT</b> combinations are also supported (e.g.,
     * {@code 1.2.3-rc1-SNAPSHOT} → {@code 1.2.3rc1.dev}).
     * <p>
     * Final release versions without pre-release qualifiers or SNAPSHOT suffix are
     * returned unmodified.
     *
     * @param pomVersion POM version of the encapsulating module in which Habushu is
     *                   being executed.
     * @param addSnapshotNumber whether to add a numeric component to SNAPSHOT versions.
     * @param snapshotNumberDateFormatPattern optional date format pattern for the snapshot number.
     * @return version number of the encapsulated Python package, appropriately
     *         formatted per PEP-440.
     * @see <a href="https://peps.python.org/pep-0440/">PEP 440 - Version Identification</a>
     */
    public static String getPythonPackageVersion(String pomVersion, boolean addSnapshotNumber,
                                                    String snapshotNumberDateFormatPattern) {
        String pythonPackageVersion = pomVersion;
        boolean hasSnapshotSuffix = false;

        // Handle pre-release versions (rc, alpha, beta) - normalize to PEP-440 format
        Matcher matcher = PRERELEASE_PATTERN.matcher(pomVersion);
        if (matcher.matches()) {
            String baseVersion = matcher.group(1);      // e.g., "1.5.0" or "1.5.0.1"
            String qualifier = matcher.group(2);        // e.g., "rc", "alpha", "beta"
            String number = matcher.group(3);           // e.g., "2"
            String snapshotSuffix = matcher.group(4);   // e.g., "-SNAPSHOT" or null

            String normalizedQualifier = normalizePep440Qualifier(qualifier);
            pythonPackageVersion = baseVersion + normalizedQualifier + number;
            hasSnapshotSuffix = snapshotSuffix != null;
        }

        // Handle SNAPSHOT versions (either standalone or combined with pre-release)
        if (hasSnapshotSuffix || isPomVersionSnapshot(pythonPackageVersion)) {
            if (hasSnapshotSuffix) {
                // Pre-release with SNAPSHOT: suffix was already stripped, just append .dev
                pythonPackageVersion += ".dev";
            } else {
                // Standalone SNAPSHOT: use existing helper which replaces -SNAPSHOT with .dev
                pythonPackageVersion = replaceSnapshotWithDev(pythonPackageVersion);
            }

            if (addSnapshotNumber) {
                String snapshotNumber;
                LocalDateTime currentTime = LocalDateTime.now();

                if (StringUtils.isNotEmpty(snapshotNumberDateFormatPattern)) {
                    snapshotNumber = currentTime.format(DateTimeFormatter.ofPattern(snapshotNumberDateFormatPattern));
                } else {
                    snapshotNumber = String.valueOf(currentTime.toEpochSecond(ZoneOffset.UTC));
                }
                pythonPackageVersion += snapshotNumber;
            }
        }

        return pythonPackageVersion;
    }

    public static String addTrailingSlash(String inputUrl) {
        if (StringUtils.isNotBlank(inputUrl) && !Strings.CS.endsWith(inputUrl, "/")) {
            // PEP-0694 likes a trailing slash:
            inputUrl += "/";
        }

        return inputUrl;
    }

    /**
     * Validates the existence of the .python-version file.
     *
     * @param baseDir the base directory of the project
     * @return true if .python-version file exists in the project directory.
     */
    public static boolean validatePythonVersionFile(File baseDir) {
        File pythonVersionFile = new File(baseDir, ".python-version");
        return pythonVersionFile.exists();
    }

}
