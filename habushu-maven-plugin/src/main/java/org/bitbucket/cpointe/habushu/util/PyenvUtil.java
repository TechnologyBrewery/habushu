package org.bitbucket.cpointe.habushu.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bitbucket.cpointe.habushu.HabushuException;

public class PyenvUtil {
    private static final Logger logger = LoggerFactory.getLogger(PyenvUtil.class);
    
    /**
     * Checks if pyenv is installed. Throws an error if it is not.
     * @param workingDirectory
     */
    public static void checkPyEnvInstall(File workingDirectory) {
        List<String> commands = new ArrayList<>();
        commands.add("pyenv");
        commands.add("--version");
        try {
            VenvExecutor executor = new VenvExecutor(workingDirectory, commands, Platform.guess(), new HashMap<>());
            executor.executeAndGetResult(logger);
        }
        catch (Exception e) {
            throw new HabushuException("'pyenv' is not currently installed! Please install pyenv and try again. Checkout https://github.com/pyenv/pyenv for more.");
        }
    }
    
    /**
     * Checks if pyenv has the given python version already, and if not we install it
     * @param expectedVersion
     * @param changeVersionScript
     * @param workingDirectory
     */
    public static void updatePythonVersion(String expectedVersion, File changeVersionScript, File workingDirectory) {
        if (!attemptVersionSwap(expectedVersion, workingDirectory)) {
            installPythonVersion(expectedVersion, workingDirectory, changeVersionScript);
        }
        
        boolean success = attemptVersionSwap(expectedVersion, workingDirectory);
        if (!success) {
            throw new HabushuException("Failed to swap to python version " + expectedVersion + " usng pyenv!");
        }
    }
    
    /**
     * Installs pythoon using pyenv install <version>, if this fails we try too patch what may be the issue.
     * @param pythonVersion
     * @param workingDirectory
     * @param changeVersionScript
     */
    private static void installPythonVersion(String pythonVersion, File workingDirectory, File changeVersionScript) {
        List<String> commands = new ArrayList<>();
        
        commands.add("pyenv");
        commands.add("install");
        commands.add(pythonVersion);
        VenvExecutor executor = new VenvExecutor(workingDirectory, commands, Platform.guess(), new HashMap<>());
        try {
            executor.executeAndGetResult(logger);
        } catch (Exception e) {
            logger.warn("Could not install python via normal install, attempting install with patch...");
            HabushuUtil.createFileAndGivePermissions(changeVersionScript);
            writeCommandsToUpdateScript(pythonVersion, changeVersionScript);
            
            try {
            	HabushuUtil.runBashScript(changeVersionScript.getAbsolutePath());
            } catch (Exception exception) {
            	throw new HabushuException("Failed to install python version with patch: ", exception);
            }
        }
    }
    
    /**
     * Attempts to change the python version.
     * @param pythonVersion
     * @param workingDirectory
     * @return if successful or not
     */
    private static boolean attemptVersionSwap(String pythonVersion, File workingDirectory) {
        List<String> commands = new ArrayList<>();
        
        commands.add("pyenv");
        commands.add("global");
        commands.add(pythonVersion);
        VenvExecutor executor = new VenvExecutor(workingDirectory, commands, Platform.guess(), new HashMap<>());
        boolean pass = true;
        try {
            executor.executeAndGetResult(logger);
        } catch (Exception e) {
            logger.warn("Could not find specified version with pyenv. Attempting install using pyenv now...");
            pass = false;
        }
        
        return pass;
    }
    
    /**
     * Writes commands to instally python with pyenv with a patch that fixes a compilation error.
     * @param pythonVersion
     * @param changeVersionScript
     */
    private static void writeCommandsToUpdateScript(String pythonVersion, File changeVersionScript) {
        StringBuilder commandList = new StringBuilder();
        commandList.append("#!/bin/bash" + "\n");
        commandList.append("pyenv install --patch ");
        commandList.append(pythonVersion);
        commandList.append(" < <(curl -sSL https://github.com/python/cpython/commit/8ea6353.patch\\?full_index\\=1) " + "\n");

        HabushuUtil.writeLinesToFile(commandList.toString(), changeVersionScript.getAbsolutePath());
    }
}
