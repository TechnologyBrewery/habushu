package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Delegates to Poetry during the {@link LifecyclePhase#PACKAGE} build phase to
 * build all deployment related artifacts for this project
 */
public class BuildDeploymentArtifactsPoetry extends AbstractBuildDeploymentArtifacts {

    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param baseDir base directory from which to operate for this module
     * @param log     the logger to use for output
     * @param mojo    Configurations for BuildDeploymentArtifactsMojo
     */
    protected BuildDeploymentArtifactsPoetry(File baseDir, Log log, BuildDeploymentArtifactsMojo mojo) {
        super(baseDir, log, mojo);
    }

    @Override
    public void doExecute() {

        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(baseDir);

        String buildCommand;
        String buildLogMessage;
        if (buildDeploymentArtifactsMojo.rewriteLocalPathDepsInArchives()) {
            buildCommand = "build-rewrite-path-deps";
            buildLogMessage = "Building source and wheel archives with poetry-monorepo-dependency-plugin...";
        } else {
            buildCommand = "build";
            buildLogMessage = "Building source and wheel archives...";
        }

        log.info(buildLogMessage);
        poetryHelper.executeAndLogOutput(Arrays.asList(buildCommand));

        if (buildDeploymentArtifactsMojo.exportRequirementsFile()) {
            log.info("Exporting requirements.txt file...");

            File directory = new File(buildDeploymentArtifactsMojo.getExportRequirementsFolder());
            if (!directory.exists()) {
                directory.mkdir();
            }

            List<String> command = new ArrayList<>();
            command.add( buildDeploymentArtifactsMojo.exportRequirementsWithoutPathDependencies() ? "export-without-path-deps" : "export");
            command.add("--output");
            String outputFile = buildDeploymentArtifactsMojo.getExportRequirementsFolder() + "/requirements.txt";
            command.add(outputFile);

            if (!buildDeploymentArtifactsMojo.exportRequirementsWithHashes()) {
                command.add("--without-hashes");
            }

            if (!buildDeploymentArtifactsMojo.exportRequirementsWithUrls()) {
                command.add("--without-urls");
            }

            poetryHelper.executeAndLogOutput(command);

            setUpPlaceholderFileAsMavenArtifact();
        }
    }

}