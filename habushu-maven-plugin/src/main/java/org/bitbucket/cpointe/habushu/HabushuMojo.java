package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A plugin to help include Venv-based projects in Maven builds. This helps keep a single build command that can build
 * the entire system with common lifecycle needs like testings and packaging artifacts that are commonly skipped in
 * Python- and R-projects.
 */
@Mojo(name = "configure-environment", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class HabushuMojo extends AbstractHabushuMojo {

    private static final Logger logger = LoggerFactory.getLogger(HabushuMojo.class);

    /**
     * Folder in which python source files are located.
     */
    @Parameter(property = "pythonSourceDirectory", required = true, defaultValue = "${project.basedir}/src/main/python")
    protected File pythonSourceDirectory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
                
        installUnpackedPythonDependencies();
        installVenvDependencies();
    }

    private void installUnpackedPythonDependencies() {
        List<File> dependencies = getDependencies();
        for(File dependency : dependencies) {
            File setupPyFile = new File(dependency, "setup.py");
            logger.debug("Unpacking dependency: {}", dependency.getName());
            if(setupPyFile.exists()) {
                VenvExecutor executor = createExecutorWithDirectory(dependency, PYTHON_COMMAND + " setup.py install");
                executor.executeAndRedirectOutput(logger);
            }
        }
    }

    private List<File> getDependencies() {
        List<File> dependencies = new ArrayList<>();

        File dependencyDirectory = new File(workingDirectory, "dependency");
        if(dependencyDirectory.exists()){
 
            dependencies = Arrays.asList(dependencyDirectory.listFiles());
        }

        return dependencies;
    }
    
    private void installVenvDependencies() {
    	String pathToPip = pathToVirtualEnvironment + "/bin/pip";
    	VirtualEnvFileHelper venvFileHelper = new VirtualEnvFileHelper(venvDependencyFile);
    	List<String> dependencies = venvFileHelper.readDependencyListFromFile();

    	for (String dependency : dependencies) {
    		logger.debug("Installing dependency listed in dependency file: {}", dependency);
    		
    		VenvExecutor executor = createExecutorWithDirectory(venvDirectory, pathToPip + " install " + dependency);
    		executor.executeAndRedirectOutput(logger);
    	}
    }
    
    @Override
    protected Logger getLogger() {
        return logger;
    }
}
