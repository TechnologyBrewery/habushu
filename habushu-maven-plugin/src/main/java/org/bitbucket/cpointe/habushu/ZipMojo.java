package org.bitbucket.cpointe.habushu;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.assembly.mojos.AbstractAssemblyMojo;
import org.apache.maven.project.MavenProject;

/**
 * Archives content in the staging directory into a zip file that is treated as the archive file for subsequent
 * lifecycle steps (install, deploy).
 */
@Mojo(name = "zip", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class ZipMojo extends AbstractAssemblyMojo {

    /**
     * Reference to the Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    
    /**
     * Override to make the naming of zips like a normal artifact in Maven.
     */
    @Parameter(property = "assembly.appendAssemblyId", defaultValue = "false")
    boolean appendAssemblyId;

    /**
     * {@inheritDoc}
     */
    @Override
    public MavenProject getProject() {
        return project;
    }

    /**
     * In not overriden in the pom, pull a default assembly file from this plugin that will inform the
     * maven-assembly-plugin how to create the desired habushu zip file.
     * 
     * {@inheritDoc}
     */
    @Override
    public String[] getDescriptorReferences() {
        String[] descriptorRefs = super.getDescriptorReferences();
        if (descriptorRefs == null || descriptorRefs.length == 0) {
            String[] defaultDescriptor = { "habushu-assembly" };
            descriptorRefs = defaultDescriptor;
        }
        return descriptorRefs;
    }
}
