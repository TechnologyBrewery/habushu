package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.resources.CopyResourcesMojo;

/**
 * Overrides the default Maven resources plugin to copy artifacts to a staging directory for archiving in a future
 * lifecycle step. Also pulls down the python source directory along with project level artifacts. In a traditional Java
 * build within Maven, source is moved during compilation. But with an interpreted language, we need to pull it down as
 * a copy since there is no compilation.
 */
@Mojo(name = "resources", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true, threadSafe = true)
public class HabushuResourcesMojo extends CopyResourcesMojo {

    /**
     * The list of resources we want to transfer.
     */
    @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
    private List<Resource> resources;

    /**
     * Folder in which python source files are located.
     */
    @Parameter(property = "pythonSourceDirectory", required = true, defaultValue = "${project.basedir}/src/main/python")
    protected File pythonSourceDirectory;

    /**
     * Folder in which standard maven resources files are located.
     */
    @Parameter(property = "resourcesDirectory", required = true, defaultValue = "${project.basedir}/src/main/resources")
    protected File resourcesDirectory;

    /**
     * The conda configuration file (e.g., yaml file) for this module. Each module can have EXACTLY ONE conda
     * configuration file.
     */
    @Parameter(property = "condaConfigurationFile", required = true, defaultValue = "${project.basedir}/"
            + AbstractHabushuMojo.DEFAULT_CONDA_CONFIGURATION_FILE_NAME)
    protected File condaConfigurationFile;

    /**
     * The output directory into which to copy the resources.
     */
    @Parameter(defaultValue = "${project.build.directory}/" + AbstractHabushuMojo.DEFAULT_STAGING_FOLDER, required = true)
    private File outputDirectory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException {
        List<Resource> pluginResources = new ArrayList<>();

        Resource source = new Resource();
        source.setDirectory(pythonSourceDirectory.getAbsolutePath());
        pluginResources.add(source);

        Resource resourceDirectory = new Resource();
        resourceDirectory.setDirectory(resourcesDirectory.getAbsolutePath());
        pluginResources.add(resourceDirectory);

        Resource baseFolderResources = new Resource();
        baseFolderResources.setDirectory(".");
        baseFolderResources.addInclude("pom.xml");
        baseFolderResources.addInclude(condaConfigurationFile.getName());
        pluginResources.add(baseFolderResources);

        setResources(pluginResources);

        super.execute();
    }

}