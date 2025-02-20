package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;

/**
 * Abstract class that ensures pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations.
 */
public abstract class AbstractInitializeHabushu {
    /**
     * Base directory from which to write Python package and dependency management files.
     */
    protected File baseDir;

    /**
     * Logger from calling class to leverage.
     */
    protected Log log;

    /**
     * Boolean to check whether to override package version
     */
    protected boolean overridePackageVersion;

    /**
     * Expected PackageVersion from pom file.
     */
    protected String expectedPythonPackageVersion;


    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param baseDir                        base directory from which to operate for this module
     * @param log                            the logger to use for output
     * @param overridePackageVersion         whether we override package version
     * @param expectedPythonPackageVersion   expected PackageVersion from pom file
     */
    protected AbstractInitializeHabushu(File baseDir,  Log log, boolean overridePackageVersion,
                                String expectedPythonPackageVersion) {
        this.baseDir = baseDir;
        this.log = log;
        this.overridePackageVersion = overridePackageVersion;
        this.expectedPythonPackageVersion = expectedPythonPackageVersion;
    }

    public abstract void doExecute() throws MojoExecutionException, MojoFailureException;


}
