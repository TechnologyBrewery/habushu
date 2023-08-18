package org.technologybrewery.habushu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

/**
 * Delegates to Poetry during the {@link LifecyclePhase#PACKAGE} build phase to
 * build all deployment related artifacts for this project, including:
 * <ul>
 * <li>sdist and wheel archives</li>
 * <li>pip-compliant {@code requirements.txt} dependency descriptor based on the
 * current Poetry lock file, if configured via the
 * {@link #exportRequirementsFile} flag</li>
 * </ul>
 */
@Mojo(name = "build-deployment-artifacts", defaultPhase = LifecyclePhase.PACKAGE)
public class BuildDeploymentArtifactsMojo extends AbstractHabushuMojo {

    /**
     * By default, export requirements.txt file.
     */
    @Parameter(property = "habushu.exportRequirementsFile", required = false, defaultValue = "true")
    protected boolean exportRequirementsFile;

    /**
     * By default, do not include the --without-urls flag when exporting.
     */
    @Parameter(property = "habushu.exportRequirementsWithUrls", required = false, defaultValue = "true")
    protected boolean exportRequirementsWithUrls;

    /**
     * By default, do not include the --without-hashes flag when exporting.
     */
    @Parameter(property = "habushu.exportRequirementsWithHashes", required = false, defaultValue = "true")
    protected boolean exportRequirementsWithHashes;

    /**
     * By default, export to the dist folder to be included with the build archive.
     */
    @Parameter(property = "habushu.exportRequirementsFolder", required = false, defaultValue = "${project.basedir}/dist")
    protected String exportRequirementsFolder;

    /**
     * Location of the artifact that will be published for this module.
     */
    @Parameter(property = "habushu.mavenArtifactFile", required = true, defaultValue = "${project.basedir}/target/habushu.placeholder.txt")
    protected File mavenArtifactFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();

        String buildCommand, buildLogMessage;
        if (this.rewriteLocalPathDepsInArchives) {
            buildCommand = "build-rewrite-path-deps";
            buildLogMessage = "Building source and wheel archives with poetry-monorepo-dependency-plugin...";
        } else {
            buildCommand = "build";
            buildLogMessage = "Building source and wheel archives...";
        }

        getLog().info(buildLogMessage);
        poetryHelper.executeAndLogOutput(Arrays.asList(buildCommand));

        if (exportRequirementsFile) {
            getLog().info("Exporting requirements.txt file...");

            String outputFile = exportRequirementsFolder + "/requirements.txt";
            File directory = new File(exportRequirementsFolder);
            if (!directory.exists()) {
                directory.mkdir();
            }

            List<String> command = new ArrayList<>();
            command.add("export");
            command.add("--output");
            command.add(outputFile);

            if (!exportRequirementsWithHashes) {
                command.add("--without-hashes");
            }

            if (!exportRequirementsWithUrls) {
                command.add("--without-urls");
            }
            poetryHelper.executeAndLogOutput(command);

            setUpPlaceholderFileAsMavenArtifact();
        }
    }

    protected void setUpPlaceholderFileAsMavenArtifact() {
        mavenArtifactFile.getParentFile().mkdirs();
        try (PrintWriter writer = new PrintWriter(mavenArtifactFile)) {
            writer.println("This is NOT the file you are looking for!");
            writer.println();
            writer.println("To take advantage of the Maven Reactor, we want to publish pom files for this artifact.");
            writer.println("But Maven isn't the right solution for managing Python dependencies.");
            writer.println();
            writer.println(String.format("Please check your appropriate Python repository for the %s files instead!",
                    project.getArtifactId()));

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not create placeholder artifact file!", e);
        }

        project.getArtifact().setFile(mavenArtifactFile);
    }

}
