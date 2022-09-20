package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;

/**
 * Delegates to Poetry to builds the source and wheel archives of this project as a part of the
 * {@link LifecyclePhase#PACKAGE} build phase.
 */
@Mojo(name = "build-archive", defaultPhase = LifecyclePhase.PACKAGE)
public class BuildArchiveMojo extends AbstractHabushuMojo {

    /**
     * By default, export requirements.txt file
     */
    @Parameter(property = "habushu.exportRequirementsFile", required = false, defaultValue = "true")
    protected boolean exportRequirementsFile;

    /**
     * By default, do not include the --without-urls flag when exporting
     */
    @Parameter(property = "habushu.exportRequirementsWithUrls", required = false, defaultValue = "true")
    protected boolean exportRequirementsWithUrls;

    /**
     * By default, do not include the --without-hashes flag when exporting
     */
    @Parameter(property = "habushu.exportRequirementsWithHashes", required = false, defaultValue = "true")
    protected boolean exportRequirementsWithHashes;

    /**
     * By default, export to the dist folder to be included with the build archive
     */
    @Parameter(property = "habushu.exportRequirementsFolder", required = false, defaultValue = "${project.basedir}/dist")
    protected String exportRequirementsFolder;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
        getLog().info("Building source and wheel archives...");
        poetryHelper.executeAndLogOutput(Arrays.asList("build"));

        if (exportRequirementsFile) {
            getLog().info("Exporting requirements.txt file...");
            
            String outputFile = exportRequirementsFolder + "/requirements.txt";
            File directory = new File(exportRequirementsFolder);
            if (!directory.exists()){
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
        }
    }

}
