package org.bitbucket.cpointe.habushu;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.fromDependencies.UnpackDependenciesMojo;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.dependency.utils.markers.DefaultFileMarkerHandler;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;

/**
 * A Mojo class to handle retrieval and unpacking of python dependencies for later install into a Conda virtual
 * environment.
 */
@Mojo(name = "unpack-python-dependencies", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class HabushuUnpackMojo extends UnpackDependenciesMojo {

    private static final Logger logger = LoggerFactory.getLogger(HabushuUnpackMojo.class);

    /**
     * The Maven Project
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Override
    protected void doExecute() throws MojoExecutionException {
        Set<Artifact> artifacts =  project.getDependencyArtifacts();

        for(Artifact artifact : artifacts) {
            File file = artifact.getFile();
            if(FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("zip")) {
                File destDir = DependencyUtil.getFormattedOutputDirectory( useSubDirectoryPerScope, useSubDirectoryPerType,
                        useSubDirectoryPerArtifact, useRepositoryLayout,
                        stripVersion, outputDirectory, artifact );
                unpack( artifact, destDir, getIncludes(), getExcludes(), getEncoding(), getFileMappers() );
                DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler( artifact, this.markersDirectory );
                handler.setMarker();
            }
        }
    }
}
