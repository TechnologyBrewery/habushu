package org.bitbucket.cpointe.habushu.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bitbucket.cpointe.habushu.HabushuException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PythonInstallUtil {

    private static final Logger logger = LoggerFactory.getLogger(PythonInstallUtil.class);
    
    private static final String PYTHON_PREFIX = "/Python-";
    private static final String HABUSHU_PYTHON = ".habushu/python";
    private static final String HABUSHU_HIDDEN_DIRECTORY = ".habushu";
    private static final String PYTHON_EXECUTABLE_LOCATION = "/bin/python3";

    public static void removeHabushuPythonInstalls(File workingDirectory) {
        VenvExecutor executor = createExecutorWithDirectory(getHomeDirectory(workingDirectory), "rm -rf " + HABUSHU_PYTHON);
        executor.executeAndRedirectOutput(logger);
    }
    
    /**
     * This method will install python for this system if python is not already installed to .habushu/python
     */
    public static String installPython(String pythonVersion, File workingDirectory) {
        if (!PythonVersionManager.checkPythonVersion(pythonVersion)) {
            throw new HabushuException("Expected Version " + PythonVersionManager.EXPECTED_VERSION
                    + " but found version " + pythonVersion + ". Please update specified Python version to "
                    + PythonVersionManager.EXPECTED_VERSION + " and try again.");
        }
        File homeDirectory = getHomeDirectory(workingDirectory);

        createDirectoryUnderHome(HABUSHU_HIDDEN_DIRECTORY, workingDirectory);
        createDirectoryUnderHome(HABUSHU_PYTHON, workingDirectory);

        Boolean pythonAlreadyInstalled = createDirectoryUnderHome(HABUSHU_PYTHON + PYTHON_PREFIX + pythonVersion, workingDirectory);
        String pythonInstallationDirectory = homeDirectory.getAbsolutePath() + "/"
                + HABUSHU_PYTHON;
        String fullPythonInstallDirectory = pythonInstallationDirectory + PYTHON_PREFIX + pythonVersion;
        
        if (!pythonAlreadyInstalled) {
            logger.info("Python install not found for specified version, starting install now, this may take a while...");
            
            String tarLocation = downloadPython(pythonVersion, pythonInstallationDirectory, pythonVersion, workingDirectory);
            
            unzipPython(tarLocation, pythonInstallationDirectory, workingDirectory);
            
            configurePythonInstall(fullPythonInstallDirectory);

            makePython(fullPythonInstallDirectory);
            
            removeExtraFile(pythonInstallationDirectory, pythonVersion, homeDirectory);
        }
        
        return fullPythonInstallDirectory + PYTHON_EXECUTABLE_LOCATION;
    }
    
    /**
     * This downloads python from python.org and saves it to a local tar file
     * @param version
     * @param installDirectory
     * @return
     */
    private static String downloadPython(String version, String installDirectory, String pythonVersion, File workingDirectory) {
        String outputLocation = installDirectory + "/python-" + pythonVersion + ".tgz";
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("curl https://www.python.org/ftp/python/");
        strBuilder.append(version);
        strBuilder.append(PYTHON_PREFIX);
        strBuilder.append(version);
        strBuilder.append(".tgz -o ");
        strBuilder.append(outputLocation);
        VenvExecutor executor = createExecutorWithDirectory(workingDirectory, strBuilder.toString());
        
        executor.executeAndRedirectOutput(logger);
        logger.info("finished downloading python from remote to file: {}", outputLocation);
        return outputLocation;
    }
    
    /**
     * This unzips the downloaded python tgz file so it can be installed
     * @param fileLocation
     * @param outputLocation
     */
    private static void unzipPython(String fileLocation, String outputLocation, File workingDirectory) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("tar -xvvf ");
        strBuilder.append(fileLocation);
        strBuilder.append(" -C ");
        strBuilder.append(outputLocation);
        
        VenvExecutor executor = createExecutorWithDirectory(workingDirectory, strBuilder.toString());
        
        executor.executeAndRedirectOutput(logger);
        logger.info("finished unzipping python");
    }
    
    /**
     * This uses the downloaded configure file to configure python to work from the given location
     * @param configureLocation
     */
    private static void configurePythonInstall(String configureLocation) {
        logger.info("Configuring python...");
        File pythonDirectory = new File(configureLocation);
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("./configure --prefix=");
        strBuilder.append(configureLocation);
        VenvExecutor executor = createExecutorWithDirectory(pythonDirectory, strBuilder.toString());
        
        executor.executeAndRedirectOutput(logger);
    }
    
    /**
     * Uses "make" to setup python
     * @param pythonLocation is where python make file was created
     */
    private static void makePython(String pythonMakeLocation) {
        File pythonDirectory = new File(pythonMakeLocation);
        // Note: big Sur does not provide a couple C functions anymore so we have to 
        //        manually update one of the module files.
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("sed -i'.bak' '115i\\");
        strBuilder.append("\n");
        strBuilder.append("#undef _POSIX_C_SOURCE");
        strBuilder.append("\n");
        strBuilder.append("' ");
        strBuilder.append(pythonDirectory + "/Modules/posixmodule.c");
        VenvExecutor updateFileExecutor = createExecutorWithDirectory(pythonDirectory, strBuilder.toString());
        updateFileExecutor.executeAndRedirectOutput(logger);

        VenvExecutor executor = createExecutorWithDirectory(pythonDirectory, "make; make install");
        
        executor.executeAndRedirectOutput(logger);
    }
    
    /**
     * This removes the downloaded tgz file
     * @param installDirectory
     */
    private static void removeExtraFile(String installDirectory, String pythonVersion, File homeDirectory) {
        String outputLocation = installDirectory + "/python-" + pythonVersion + ".tgz";
        VenvExecutor executor = createExecutorWithDirectory(homeDirectory, "rm -rf " + outputLocation);
        
        executor.executeAndRedirectOutput(logger);
    }
    
    /**
     * This method will attempt to create a directory underneath home for the given directory name
     * @param directoryName
     * @return if the path already existed or not
     */
    private static Boolean createDirectoryUnderHome(String directoryName, File workingDirectory) {
        File homeDirectory = getHomeDirectory(workingDirectory);
        VenvExecutor executor = createExecutorWithDirectory(homeDirectory, 
                "if test -d " + directoryName + "; then echo 'found directory';fi");
        String result = executor.executeAndGetResult(logger);
        
        Boolean directoryExisted = StringUtils.isNotEmpty(result);
        if (!directoryExisted) {
            logger.warn("Created directory: {}/{}", homeDirectory.getAbsolutePath(), directoryName);
            VenvExecutor createExecutor = createExecutorWithDirectory(homeDirectory, "mkdir " + directoryName);
            createExecutor.executeAndGetResult(logger);
        }
        
        return directoryExisted;
    }
    
    /**
     * Gets the home directory via echo command
     * @return the home directory if it can be found
     */
    private static File getHomeDirectory(File workingDirectory) {
        VenvExecutor executor = createExecutorWithDirectory(workingDirectory, "echo $HOME");
        String home = executor.executeAndGetResult(logger);
        File homeDirectory = new File(home);
        logger.info("Found home directory: {}", home);
        return homeDirectory;
    }
    
    /**
     * Creates and returns an executor for Venv tied to a specified directory.
     * 
     * @param directoryForVenv the directory for the virtual environment
     * @param command          the command to invoke
     * @return Venv Executor
     */
    private static VenvExecutor createExecutorWithDirectory(File directoryForVenv, String command) {
        List<String> commands = new ArrayList<>();
        commands.add("/bin/bash");
        commands.add("-c");

        commands.add(command);
        return new VenvExecutor(directoryForVenv, commands, Platform.guess(), new HashMap<>());
    }
}
