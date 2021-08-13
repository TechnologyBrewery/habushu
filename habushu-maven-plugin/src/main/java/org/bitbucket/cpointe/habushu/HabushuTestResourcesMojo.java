package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.lang.Deprecated;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    @Parameter(property = "pythonSourceDirectory", required = true, defaultValue = "${project.basedir}/src/main/python")
    protected File pythonSourceDirectory;
    
    /**
     * Folder in which python test files are located.
     */
    @Parameter(property = "pythonTestDirectory", required = true, defaultValue = "${project.basedir}/src/test/python")
    protected File pythonTestDirectory;

    /**
     * Folder in which python test resources files are located.
     */
    @Parameter(property = "pythonTestResourceDirectory", required = true, defaultValue = "${project.basedir}/src/test/resources")
    protected File pythonTestResourceDirectory;

    /**
     * Required by the copy mojo. List of resources to copy.
     * @deprecated
     * This parameter is not used by Habushu acceptable to compute time between versions.
     * <p> Use {@link HabushuTestResourcesMojo#pythonTestResourceDirectory} instead. </p>
     */
    @Deprecated
    @Parameter(defaultValue = "${project.testResources}", required = true, readonly = true)
    private List<Resource> resources;

    /**
     * The output directory into which to copy the resources.
     */
    @Parameter(defaultValue = "${project.build.directory}/" + AbstractHabushuMojo.DEFAULT_TEST_STAGING_FOLDER, required = true)
    private File outputDirectory;

    /**
     * {@inheritDoc} Copies all src and test files into the default test staging
     * directory.
     */
    @Override
    public void execute() throws MojoExecutionException {
        List<Resource> pluginResources = new ArrayList<>();

        Resource source = new Resource();
        source.setDirectory(pythonSourceDirectory.getAbsolutePath());
        pluginResources.add(source);
        
        Path pythonPath = Paths.get(pythonTestDirectory.getAbsolutePath());
        if (Files.exists(pythonPath)) {
            Resource testSource = new Resource();
            testSource.setDirectory(pythonTestDirectory.getAbsolutePath());
            pluginResources.add(testSource);
        }

        Path resourcePath = Paths.get(pythonTestResourceDirectory.getAbsolutePath());
        if (Files.exists(resourcePath)) {
            Resource resourceSource = new Resource();
            resourceSource.setDirectory(pythonTestResourceDirectory.getAbsolutePath());
            pluginResources.add(resourceSource);
        }

        setResources(pluginResources);

        super.execute();
        
    }

}
