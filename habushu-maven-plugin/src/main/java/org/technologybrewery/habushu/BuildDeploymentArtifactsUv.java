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

        String buildCommand;
        String buildLogMessage;
        //TODO: rewriteLocalPathDepsInArchives Implementation shall be available once uv-monorepo-dependency-plugin is implemented.
        buildCommand = "build";
        buildLogMessage = "Building source and wheel archives...";

        log.info(buildLogMessage);
        uvCommandHelper.executeAndLogOutput(Arrays.asList(buildCommand));


        if (buildDeploymentArtifactsMojo.exportRequirementsFile()) {
            log.info("Exporting requirements.txt file...");

            File directory = new File(buildDeploymentArtifactsMojo.getExportRequirementsFolder());
            if (!directory.exists()) {
                directory.mkdir();
            }

            List<String> command = new ArrayList<>();
            //TODO: exportRequirementsWithoutPathDependencies shall available done once uv-monorepo-dependency-plugin is implemented.
            command.add("export");
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