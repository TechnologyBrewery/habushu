package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;

import java.io.File;

/**
 * Delegates to Package Manager during the {@link LifecyclePhase#PACKAGE} build phase to
 * build all deployment related artifacts for this project, including:
 * <ul>
 * <li>sdist and wheel archives</li>
 * <li>pip-compliant {@code requirements.txt} dependency descriptor based on the
 * current Poetry lock file, if configured via the
 * {@link #exportRequirementsFile} flag</li>
 * </ul>
 */
@Mojo(name = "build-deployment-artifacts", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class BuildDeploymentArtifactsMojo extends AbstractHabushuMojo {

    /**
     * By default, export requirements.txt file.
     */
    @Parameter(property = "habushu.exportRequirementsFile", required = false, defaultValue = "true")
    protected boolean exportRequirementsFile;

    /**
     * By default, do not cache wheel (*.whl) file(s).
     */
    @Parameter(property = "habushu.cacheWheels", required = false, defaultValue = "false")
    protected boolean cacheWheels;

    /**
     * By default, do not include the --without-urls flag when exporting.
     */
    @Parameter(property = "habushu.exportRequirementsWithUrls", required = false, defaultValue = "true")
    protected boolean exportRequirementsWithUrls;

    /**
     * By default, do not include the --without-hashes flag when exporting.
     */
    @Parameter(property = "habushu.exportRequirementsWithHashes", required = false, defaultValue = "true")
    protected boolean exportRequirementsWithHashes;

    @Parameter(property = "habushu.exportRequirementsWithoutPathDependencies", required = false, defaultValue = "true")
    protected boolean exportRequirementsWithoutPathDependencies;

    /**
     * By default, export to the dist folder to be included with the build archive.
     */
    @Parameter(property = "habushu.exportRequirementsFolder", required = false, defaultValue = "${project.basedir}/dist")
    protected String exportRequirementsFolder;

    /**
     * Location of the artifact that will be published for this module.
     */
    @Parameter(property = "habushu.mavenArtifactFile", required = true, defaultValue = "${project.basedir}/target/habushu.placeholder.txt")
    protected File mavenArtifactFile;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        if (HabushuUtil.checkPythonPackageManager(getPyProjectTomlFile()) == PackageManager.POETRY){
            BuildDeploymentArtifactsPoetry buildDeploymentArtifactsPoetry = new BuildDeploymentArtifactsPoetry(getPythonProjectBaseDir(), getLog(), this);
            buildDeploymentArtifactsPoetry.doExecute();
        } else {
            BuildDeploymentArtifactsUv buildDeploymentArtifactsUv = new BuildDeploymentArtifactsUv(getPythonProjectBaseDir(), getLog(), this);
            buildDeploymentArtifactsUv.doExecute();
        }

    }

    public boolean exportRequirementsFile() {
        return exportRequirementsFile;
    }

    public boolean isCacheWheels() {
        return cacheWheels;
    }

    public boolean exportRequirementsWithUrls() {
        return exportRequirementsWithUrls;
    }

    public boolean exportRequirementsWithHashes() {
        return exportRequirementsWithHashes;
    }

    public boolean exportRequirementsWithoutPathDependencies() {
        return exportRequirementsWithoutPathDependencies;
    }

    public String getExportRequirementsFolder() {
        return exportRequirementsFolder;
    }

    public File getMavenArtifactFile() {
        return mavenArtifactFile;
    }
}