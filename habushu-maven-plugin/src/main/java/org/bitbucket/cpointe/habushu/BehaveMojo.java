package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes behave for Cucumber testing in python following the standard behave structure of a features directory.
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class BehaveMojo extends AbstractHabushuMojo {

    private static final Logger logger = LoggerFactory.getLogger(BehaveMojo.class);

    /**
     * Folder in which python unit test files are located.
     */
    @Parameter(property = "pythonTestDirectory", required = true, defaultValue = "${project.basedir}/src/test/python")
    protected File pythonTestDirectory;

    /**
     * Additional options that should be passed to the behave command.
     */
    @Parameter(property = "cucumber.options", required = false)
    protected String cucumberOptions;

    /**
     * By default, exclude any scenario or feature file tagged with '@manual'.
     */
    @Parameter(property = "excludeManualTag", required = true, defaultValue = "true")
    protected boolean excludeManualTag;

    /**
     * Set this to "true" to skip running tests. Its use is NOT RECOMMENDED, but quite convenient on occasion.
     */
    @Parameter(property = "skipTests", defaultValue = "false")
    protected boolean skipTests;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();

        boolean hasTests = false;
        File behaveDirectory = new File(pythonTestDirectory, "features");
        if (!skipTests && behaveDirectory.exists()) {
            hasTests = hasTestArtifactsToProcess(behaveDirectory);
        }

        if (hasTests) {
            verifyBehaveExistsInEnvironment();

            logger.info("-------------------------------------------------------");
            logger.info("T E S T S");
            logger.info("-------------------------------------------------------");

            StringBuilder command = new StringBuilder();
            command.append("behave ").append(getCanonicalPathForFile(behaveDirectory));

            if (excludeManualTag) {
                command.append(" --tags ~@manual");
            }

            if (StringUtils.isNotBlank(cucumberOptions)) {
                command.append(" ").append(cucumberOptions);
            }

            String finalCommand = command.toString();
            logger.debug("To run command manually, use {}", finalCommand);
            runInCondaEnvironmentAndRedirectOutput(environmentName, finalCommand);

        } else if (skipTests) {
            logger.info("Tests are skipped.");

        } else {
            logger.info("No tests found in {}", getCanonicalPathForFile(behaveDirectory));

        }

    }

    private boolean hasTestArtifactsToProcess(File behaveDirectory) {
        boolean hasTests;
        IOFileFilter fileExtensionFilter = new WildcardFileFilter("*.*");
        IOFileFilter directoryFilter = TrueFileFilter.INSTANCE; // any directory
        Collection<File> testFiles = FileUtils.listFiles(behaveDirectory, fileExtensionFilter, directoryFilter);
        logger.debug("{} test artifacts found", testFiles.size());
        hasTests = !testFiles.isEmpty();
        return hasTests;
    }

    private void verifyBehaveExistsInEnvironment() {        
        @SuppressWarnings("unchecked")
        List<String> dependencies = (List<String>) condaEnvironment.get("dependencies");
        if (dependencies == null || !dependencies.contains("behave")) {
            logger.error(
                    "Your conda environment MUST contain a dependency to the 'behave' package to support habushu's behave functionality.");
            logger.error("Please update {} as follows:", getCanonicalPathForFile(condaConfigurationFile));
            logger.error("");
            logger.error("\tdependencies:");
            logger.error("\t    - ...");
            logger.error("\t    - behave   <----------  ******** ADD THIS ********");
            logger.error("");

            throw new HabushuException(
                    "'behave' package MUST be a dependency in your conda environment configuration!");
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

}
