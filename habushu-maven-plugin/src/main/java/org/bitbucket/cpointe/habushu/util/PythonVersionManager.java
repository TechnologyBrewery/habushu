package org.bitbucket.cpointe.habushu.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.bitbucket.cpointe.habushu.HabushuException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PythonVersionManager {
    
    public static final String EXPECTED_VERSION = "3.7.x";

    private static final String SCRIPT = "performCheck.py";
    
    private static final Pattern pattern = Pattern.compile("3\\.7.*");

    private String pythonCommand;

    private String pythonVersion;

    private boolean isExpectedVersion;

    private String buildScriptsDirectory;

    /**
     * Standard logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(PythonVersionManager.class);

    /**
     * Default constructor
     */
    public PythonVersionManager(File workingDirectory, String pythonCommand) {
        this.pythonCommand = pythonCommand;
        this.buildScriptsDirectory = workingDirectory.getAbsolutePath() + "/build/scripts/";
        checkVersion();
    }

    private void checkVersion() {
        File versionScript = writeVersionScript();
        pythonVersion = getPythonVersionFromScript(versionScript.getAbsolutePath());
        if (pythonVersion == null) {
            throw new HabushuException("Could not execute python version command. Please check that " + pythonCommand
                    + " corresponds to a python executable.");
        }

        isExpectedVersion = checkPythonVersion(pythonVersion);
    }
    
    public static boolean checkPythonVersion(String pythonVersion) {
    	Matcher matcher = pattern.matcher(pythonVersion);
        return matcher.matches();
    }

    public boolean isExpectedVersion() {
        return isExpectedVersion;
    }

    public String getPythonVersion() {
        return pythonVersion;
    }

    /**
     * This will write out a little python script we can run that prints out the
     * version.
     */
    private File writeVersionScript() {

        // Write out a little script to check the python version
        createBuildScriptsDirectory();
        File versionScript = new File(buildScriptsDirectory, SCRIPT);

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(versionScript));
            out.write(getVersionCommands());
            out.close();
        } catch (IOException e) {
            throw new HabushuException(
                    "Could not write out " + versionScript.getName() + " to " + buildScriptsDirectory, e);
        }
        return versionScript;
    }

    /**
     * Method that runs a simple python script that determines the version.
     * 
     * @param absolutePath
     * @return pythonVersion
     */
    private String getPythonVersionFromScript(String absolutePath) {

        String pythonVersion = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, absolutePath);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStreamReader results = new InputStreamReader(process.getInputStream());
            BufferedReader reader = new BufferedReader(results);
            pythonVersion = StringUtils.trimToNull(reader.readLine());
        } catch (IOException e) {
            throw new HabushuException("Error when executing python version script: ", e);
        }
        return pythonVersion;
    }

    private void createBuildScriptsDirectory() {
        File scriptsDirectory = new File(buildScriptsDirectory);
        scriptsDirectory.mkdirs();
        logger.debug("Writing out build scripts to {}", buildScriptsDirectory);
    }

    private String getVersionCommands() {
        StringBuilder commands = new StringBuilder();
        commands.append("import platform");
        commands.append(System.lineSeparator());
        commands.append("print(platform.python_version())");
        return commands.toString();
    }

}
