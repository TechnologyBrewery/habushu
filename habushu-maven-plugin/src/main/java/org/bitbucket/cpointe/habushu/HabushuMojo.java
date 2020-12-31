package org.bitbucket.cpointe.habushu;

import java.io.File;

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
@Mojo(name = "habushu-maven-plugin", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
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
