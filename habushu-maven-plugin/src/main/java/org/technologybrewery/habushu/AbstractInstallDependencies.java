package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;

/**
 * Abstract class that ensures pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations.
 */
public abstract class AbstractInstallDependencies {
    /**
     * Base directory from which to write Python package and dependency management files.
     */
    protected File baseDir;

    /**
     * Logger from calling class to leverage.
     */
    protected Log log;

    /**
     * Configuration class that is needed forInstallDependenciesMOJO
     */
    protected InstallDependenciesConfigurations installDependenciesConfigurations;


    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param baseDir    base directory from which to operate for this module
     * @param log                            the logger to use for output
     * @param installDependenciesConfigurations Configurations for installing Dependencies
     */
    protected AbstractInstallDependencies(File baseDir, Log log, InstallDependenciesConfigurations installDependenciesConfigurations) {
        this.baseDir = baseDir;
        this.log = log;
        this.installDependenciesConfigurations = installDependenciesConfigurations;
    }


    public abstract void doExecute() throws MojoExecutionException, MojoFailureException, IOException;

    /**
     * Returns a {@link File} representing this project's pyproject.toml
     * configuration.
     *
     * @return
     */
    protected File getPyProjectTomlFile() {
        return new File(baseDir, "pyproject.toml");
    }

    /**
     * log that package is not up to date with managed dependencies.
     */
    protected void logPackageMismatch(String packageName, String originalOperatorAndVersion, String updatedOperatorAndVersion) {
        log.warn(String.format("Package %s is not up to date with common project package definition guidance! "
                + "Currently %s, but should be %s!", packageName, originalOperatorAndVersion, updatedOperatorAndVersion));
    }


}