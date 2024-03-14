package org.technologybrewery.habushu;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.conversion.Path;
import com.electronwill.nightconfig.core.file.FileConfig;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * By default, do not cache wheel (*.whl) file(s).
     */
    @Parameter(property = "habushu.cacheWheels", required = false, defaultValue = "false")
    protected boolean cacheWheels;

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

    @Parameter(property = "habushu.exportRequirementsWithoutPathedDependencies", required = false, defaultValue = "true")
    protected boolean exportRequirementsWithoutPathedDependencies;

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
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();

        String buildCommand;
        String buildLogMessage;
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

            File directory = new File(exportRequirementsFolder);
            if (!directory.exists()) {
                directory.mkdir();
            }

            List<String> command = new ArrayList<>();
            command.add( exportRequirementsWithoutPathedDependencies ? "export-without-path-deps" : "export");
            command.add("--output");
            String outputFile = exportRequirementsFolder + "/requirements.txt";
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
            throw new HabushuException("Could not create placeholder artifact file!", e);
        }

        project.getArtifact().setFile(mavenArtifactFile);
    }  
}