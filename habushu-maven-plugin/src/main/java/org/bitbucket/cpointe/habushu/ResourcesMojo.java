package org.bitbucket.cpointe.habushu;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Overrides the default Maven resources plugin to copy artifacts to a staging directory for archiving in a future
 * lifecycle step/
 */
@Mojo(name = "resources", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true, threadSafe = true)
public class ResourcesMojo extends org.apache.maven.plugins.resources.ResourcesMojo {

    /**
     * The output directory into which to copy the resources.
     */
    @Parameter(defaultValue = "target/" + AbstractHabushuMojo.DEFAULT_STAGING_FOLDER, required = true)
    private File outputDirectory;

}
