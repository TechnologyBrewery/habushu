package org.technologybrewery.habushu.util;

import com.moandjiezana.toml.Toml;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.maven.plugin.logging.Log;
import org.technologybrewery.habushu.AbstractPythonPackageAndDependencyManagerSetup;
import org.technologybrewery.habushu.HabushuException;
import org.technologybrewery.habushu.PythonPackageAndDependencyManagerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

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

    private HabushuUtil() {
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
     * @return
     * @throws MojoExecutionException
     */
    public static AbstractPythonPackageAndDependencyManagerSetup getPythonPackageAndDependencyManagerSetup(
            PackageManager pythonPackageAndDependencyManager, String pythonVersion, boolean isPythonVersionConfigurationSet,
            String defaultPythonStrategy, File baseDir, boolean rewriteLocalPathDepsInArchives, Log log,
            Boolean usePyenv, File patchInstallScript) {
        // Set the poetry-based parameters to null
        if (pythonPackageAndDependencyManager == PackageManager.UV) {
            usePyenv = null;
            patchInstallScript = null;
        }

        return PythonPackageAndDependencyManagerFactory.createPythonPackageAndDependencyManagerSetup(
                pythonVersion, isPythonVersionConfigurationSet, defaultPythonStrategy, baseDir, rewriteLocalPathDepsInArchives, log,
                pythonPackageAndDependencyManager, usePyenv, patchInstallScript);
    }

    /**
     * Finds and returns Python Package Manager.
     *
     * @return PackageManager returns which package manager Habushu uses based on build-backend.
     */
    public static PackageManager checkPythonPackageManager(File pyProjectTomlFile) {
        Toml toml = new Toml().read(pyProjectTomlFile);
        String buildBackend = toml.getString("build-system.build-backend");
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
}
