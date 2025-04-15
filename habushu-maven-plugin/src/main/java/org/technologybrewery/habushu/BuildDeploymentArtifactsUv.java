package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.technologybrewery.habushu.exec.UvCommandHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Delegates to UV during the {@link LifecyclePhase#PACKAGE} build phase to
 * build all deployment related artifacts for this project
 */
public class BuildDeploymentArtifactsUv extends AbstractBuildDeploymentArtifacts {

    /**
     * New instance - these values are typically passed in from Maven-enabled parameters in the calling Mojo.
     *
     * @param baseDir base directory from which to operate for this module
     * @param log     the logger to use for output
     * @param mojo    Configurations for BuildDeploymentArtifactsMojo
     */
    protected BuildDeploymentArtifactsUv(File baseDir, Log log, BuildDeploymentArtifactsMojo mojo) {
        super(baseDir, log, mojo);
    }

    @Override
    public void doExecute() {

        UvCommandHelper uvCommandHelper =  new UvCommandHelper(baseDir);

        if (buildDeploymentArtifactsMojo.rewriteLocalPathDepsInArchives()) {
            List<String> buildCommand = new ArrayList<>();
            buildCommand.add("uv-monorepo-dependency-tool");
            buildCommand.add("build-rewrite-path-deps");
            log.info("Building source and wheel archives with uv-monorepo-dependency-tool...");
            uvCommandHelper.executeAndLogOutput(uvCommandHelper.createToolRunCommand(buildCommand));
        } else {
            log.info("Building source and wheel archives...");
            uvCommandHelper.executeAndLogOutput(Arrays.asList("build"));
        }

        if (buildDeploymentArtifactsMojo.exportRequirementsFile()) {
            log.info("Exporting requirements.txt file...");

            File directory = new File(buildDeploymentArtifactsMojo.getExportRequirementsFolder());
            if (!directory.exists()) {
                directory.mkdir();
            }

            List<String> command = new ArrayList<>();
            command.add("export");
            // By default, uv includes the current project as an editable dependency in the exported requirements file with all of its dependencies.
            // Disabling this feature as we don't need this capability in a containerization setting
            command.add("--no-emit-project");
            // By default, uv includes the development dependency group in the exported requirements file.
            // Disabling this feature as we don't need this capability in a containerization setting
            command.add("--no-dev");

            command.add("--output-file");
            String outputFile = buildDeploymentArtifactsMojo.getExportRequirementsFolder() + "/requirements.txt";
            command.add(outputFile);

            if (!buildDeploymentArtifactsMojo.exportRequirementsWithHashes()) {
                command.add("--no-hashes");
            }

            uvCommandHelper.execute(command);

            setUpPlaceholderFileAsMavenArtifact();
        }

    }
}