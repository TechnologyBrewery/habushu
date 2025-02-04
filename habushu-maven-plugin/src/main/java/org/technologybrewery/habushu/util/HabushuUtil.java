package org.technologybrewery.habushu.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.habushu.HabushuException;
import org.technologybrewery.habushu.PyenvAndPoetrySetup;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Contains utility functionality for Habushu, including bash script execution
 * and accessing username/password credentials that may be defined within the
 * relevant settings.xml configuration.
 */
public final class HabushuUtil {

    private static final Logger logger = LoggerFactory.getLogger(HabushuUtil.class);

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
     * @param filePath the path to the file to copy
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


	public static String findCurrentVirtualEnvironmentFullPath(String pythonVersion, String pythonPackageAndDependencyManager, boolean usePyenv,
		   File patchInstallScript, File workingDirectory, boolean rewriteLocalPathDepsInArchives, Log log)
            throws MojoExecutionException {
		String virtualEnvFullPath = null;

		if (pythonPackageAndDependencyManager.equals("poetry")) {
            virtualEnvFullPath = PoetryUtil.findCurrentVirtualEnvironmentFullPathForPoetry(pythonVersion, pythonPackageAndDependencyManager,
        		usePyenv, patchInstallScript, workingDirectory, rewriteLocalPathDepsInArchives, log);
        } else {
            // todo: call corresponding uv functionality 
			virtualEnvFullPath = UvUtil.findCurrentVirtualEnvironmentFullPathForUv(pythonVersion, pythonPackageAndDependencyManager,
        		usePyenv, patchInstallScript, workingDirectory, rewriteLocalPathDepsInArchives, log);
        }

		return virtualEnvFullPath;
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
}