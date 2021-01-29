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
 * A plugin to help include Conda-based projects in Maven builds. This helps keep a single build command that can build
 * the entire system with common lifecycle needs like testings and packaging artifacts that are commonly skipped in
 * Python- and R-projects.
 */
@Mojo(name = "configure-environment", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class HabushuMojo extends AbstractHabushuMojo {

    private static final Logger logger = LoggerFactory.getLogger(HabushuMojo.class);

    /**
     * The conda configuration file checksum storage file.
     */
    @Parameter(property = "condaConfigurationChecksumFile", required = true, defaultValue = "${project.basedir}/target/config-checksum.md5")
    protected File condaConfigurationChecksumFile;

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
                
        boolean exists = currentEnvironments.contains("\n" + environmentName + " ");
        String condaConfigurationFileCanonicalPath = getCanonicalPathForFile(condaConfigurationFile);
        handleCondaEnvironmentSetup(condaConfigurationFileCanonicalPath, environmentName, exists);
        installUnpackedPythonDependencies();
    }

    private void installUnpackedPythonDependencies() {
        List<File> dependencies = getDependencies();
        for(File dependency : dependencies) {
            File setupPyFile = new File(dependency, "setup.py");
            logger.debug("Unpacking dependency: {}", dependency.getName());
            if(setupPyFile.exists()) {
                CondaExecutor executor = createExecutorWithDirectory(dependency, "run -n " + this.environmentName + " python setup.py install");
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

    private void handleCondaEnvironmentSetup(String condaConfigurationPath, String environmentName, boolean exists) {
        // TODO: if we have an update but no change in the Conda configuration, it would be nice to skip this step.
        // It's easy enough to do w/ a file hash, but we need to figure out where we want to store that hash since
        // target will be blown away by default during builds. Likely we will override clean to exclude the hash file
        // along with adding a "force clean" option
        if (exists) {
            logger.info("Updating existing Conda Environment: {}", environmentName);

        } else {
            logger.info("Creating new Conda Environment: {}", environmentName);

        }

        String action = (exists) ? "update" : "create";
        String environmentCreateResponse = invokeCondaCommand("env " + action + " --file " + condaConfigurationPath);

        logger.debug(environmentCreateResponse);
    }
    
    @Override
    protected Logger getLogger() {
        return logger;
    }
}
