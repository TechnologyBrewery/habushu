package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Abstract class that ensures pre-requisite tools that Habushu leverages are installed and available on the
 * developer's machine to support the same functionality across multiple Mojo implementations.
 */
public abstract class AbstractBuildDeploymentArtifacts {
    /**
     * Base directory from which to write Python package and dependency management files.
     */
    protected File baseDir;

    /**
     * Logger from calling class to leverage.
     */
    protected Log log;

    /**
     * Instance of buildDeploymentArtifactsMojo
     */
    protected BuildDeploymentArtifactsMojo buildDeploymentArtifactsMojo;


    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param baseDir    base directory from which to operate for this module
     * @param log                            the logger to use for output
     * @param mojo Configurations for BuildDeploymentArtifactsMojo
     */
    protected AbstractBuildDeploymentArtifacts(File baseDir, Log log, BuildDeploymentArtifactsMojo mojo) {
        this.baseDir = baseDir;
        this.log = log;
        this.buildDeploymentArtifactsMojo = mojo;
    }


    public abstract void doExecute();

    /**
     * Returns a {@link File} representing this project's pyproject.toml
     * configuration.
     *
     * @return pyproject.toml file
     */
    protected File getPyProjectTomlFile() {
        return new File(baseDir, "pyproject.toml");
    }


    protected void setUpPlaceholderFileAsMavenArtifact() {
        File mavenArtifactFile = buildDeploymentArtifactsMojo.getMavenArtifactFile();
        mavenArtifactFile.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(mavenArtifactFile)) {
            writer.println("This is NOT the file you are looking for!");
            writer.println();
            writer.println("To take advantage of the Maven Reactor, we want to publish pom files for this artifact.");
            writer.println("But Maven isn't the right solution for managing Python dependencies.");
            writer.println();
            writer.println(String.format("Please check your appropriate Python repository for the %s files instead!",
                    buildDeploymentArtifactsMojo.getProjectArtifactId()));

        } catch (FileNotFoundException e) {
            throw new HabushuException("Could not create placeholder artifact file!", e);
        }

        buildDeploymentArtifactsMojo.getProjectArtifact().setFile(mavenArtifactFile);
    }

}