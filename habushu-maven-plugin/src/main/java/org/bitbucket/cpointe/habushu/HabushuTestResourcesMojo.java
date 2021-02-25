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
 * Overrides the default Maven test resources plugin to copy source and test
 * artifacts to a default directory for running behave tests later in the
 * lifecycle. Also pulls down the python source directory along with project
 * level artifacts. In a traditional Java build within Maven, source is moved
 * during compilation. But with an interpreted language, we need to pull it down
 * as a copy since there is no compilation. We need the test and source code
 * together with a common parent directory to resolve direct references from the
 * test code to the source code.
 */
@Mojo(name = "test-resources", defaultPhase = LifecyclePhase.PROCESS_TEST_RESOURCES, requiresProject = true, threadSafe = true)
public class HabushuTestResourcesMojo extends CopyResourcesMojo {

    /**
     * Folder in which python source files are located.
     */
    @Parameter(property = "baseDirectory", required = false, defaultValue = "${project.basedir}/src")
    protected File baseDirectory;
        
    /**
     * The list of resources we want to transfer.
     */
    @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
    private List<Resource> resources;

    /**
     * The output directory into which to copy the resources.
     */
    @Parameter(defaultValue = "${project.build.directory}/" + AbstractHabushuMojo.DEFAULT_TEST_STAGING_FOLDER + "/src", required = true)
    private File outputDirectory;

    /**
     * {@inheritDoc} Copies all src and test files into the default test staging
     * directory.
     */
    @Override
    public void execute() throws MojoExecutionException {
        List<Resource> pluginResources = new ArrayList<>();

        Resource source = new Resource();
        source.setDirectory(baseDirectory.getAbsolutePath());
        pluginResources.add(source);

        setResources(pluginResources);

        super.execute();
        
    }

}
