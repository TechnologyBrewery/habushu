package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.clean.CleanMojo;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;

/**
 * Overrides the default {@link CleanMojo} behavior to additionally delete the
 * virtual environment that is created/managed by Poetry if the
 * {@link #deleteVirtualEnv} option is enabled.
 */
@Mojo(name = "clean-habushu", defaultPhase = LifecyclePhase.CLEAN, threadSafe = true)
public class CleanHabushuMojo extends CleanMojo {

    /**
     * Base directory in which Python projects will be located - should always be
     * the basedir of the encapsulating Maven project.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    protected File workingDirectory;

    /**
     * Directory in which Poetry/uv places generated source and wheel archive
     * distributions.
     */
    @Parameter(defaultValue = "${project.basedir}/dist", readonly = true, required = true)
    protected File distDirectory;

    /**
     * Directory in which Maven places build-time artifacts - should NOT include dist items.
     */
    @Parameter(defaultValue = "${project.basedir}/target", readonly = true, required = true)
    protected File targetDirectory;

    /**
     * The packaging type of the current Maven project. If it is not "habushu", then this mojo will be skipped.
     */
    @Parameter(defaultValue = "${project.packaging}", readonly = true, required = true)
    protected String packaging;

    /**
     * Enables the explicit deletion of the virtual environment that is
     * created/managed by Poetry/uv.
     */
    @Parameter(property = "habushu.deleteVirtualEnv", required = true, defaultValue = "false")
    protected boolean deleteVirtualEnv;

    /**
     * Indicates whether Habushu should leverage the
     * {@code poetry-monorepo-dependency-plugin} or the
     * {@code uv-monorepo-dependency-plugin} (<- todo) to rewrite any local path
     * dependencies (to other Poetry/uv projects) as versioned packaged dependencies in
     * generated wheel/sdist archives. If {@code true}, Habushu will replace
     * invocations of Poetry/uv's {@code build} and {@code publish} commands in the
     * {@link BuildDeploymentArtifactsMojo} and {@link PublishToPyPiRepoMojo} with
     * the extensions of those commands exposed by the
     * {@code poetry-monorepo-dependency-plugin}/{@code uv-monorepo-dependency-plugin}, which are
     * {@code build-rewrite-path-deps} and {@code publish-rewrite-path-deps}
     * respectively.
     * <p>
     * Typically, this flag will only be {@code true} when deploying/releasing
     * Habushu modules within a CI environment that are part of a monorepo project
     * structure in which multiple Poetry/uv projects depend on one another.
     */
    @Parameter(defaultValue = "false", property = "habushu.rewriteLocalPathDepsInArchives")
    protected boolean rewriteLocalPathDepsInArchives;

    /**
     * Whether to configure Poetry's {@code virtualenvs.in-project} value for this project.
     * If configured, virtual environments will be migrated to this approach during the clean phase of the build.
     * <p>
     * While generally easier to find and use for tasks like debugging, having your virtual environment co-located in
     * your project may be less useful for executions like CI builds where you may want to centrally caches virtual
     * environments from a central location.
     */
    @Parameter(defaultValue = "true", property = "habushu.useInProjectVirtualEnvironment")
    protected boolean useInProjectVirtualEnvironment;


    @Override
    public void execute() throws MojoExecutionException {
        if (HabushuUtil.HABUSHU.equals(packaging)) {
            if (HabushuUtil.checkPythonPackageManager(new File(workingDirectory, TomlUtils.PYPROJECT_TOML)) == PackageManager.POETRY){
                CleanHabushuPoetry cleanHabushuPoetry = new CleanHabushuPoetry(workingDirectory, getLog(), this);
                cleanHabushuPoetry.doExecute();
            } else {
                CleanHabushuUv cleanHabushuUv = new CleanHabushuUv(workingDirectory, getLog(), this);
                cleanHabushuUv.doExecute();
            }

        } else {
            getLog().info("Skipping execution - packaging type is not 'habushu'");
        }
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public File getDistDirectory() {
        return distDirectory;
    }

    public File getTargetDirectory() {
        return targetDirectory;
    }

    public String getPackaging() {
        return packaging;
    }

    public boolean deleteVirtualEnv() {
        return deleteVirtualEnv;
    }

    public boolean rewriteLocalPathDepsInArchives() {
        return rewriteLocalPathDepsInArchives;
    }

    public boolean useInProjectVirtualEnvironment() {
        return useInProjectVirtualEnvironment;
    }

    /**
     * This is to call CleanMoJo execute() so that cleans files in the fileset that is set in the CleanHabushuPoetry and/or CleanHabushuUV
     * @throws MojoExecutionException
     */
    public void cleanExecute() throws MojoExecutionException {
        super.execute();
    }
}
