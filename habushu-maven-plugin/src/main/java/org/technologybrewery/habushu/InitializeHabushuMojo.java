package org.technologybrewery.habushu;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.technologybrewery.habushu.util.HabushuUtil;
import org.technologybrewery.habushu.util.PackageManager;

/**
 * Ensures that the current project is a valid Poetry or uv project and initializes
 * Habushu versioning conventions, specifically aligning the version specified
 * in the {@code pom.xml} with the version in the project's
 * {@code pyproject.toml}.
 */
@Mojo(name = "initialize-habushu", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class InitializeHabushuMojo extends AbstractHabushuMojo {

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        String pomVersion = project.getVersion();
        String expectedPythonPackageVersion = getPythonPackageVersion(pomVersion, false, null);
        if (HabushuUtil.checkPythonPackageManager(getPyProjectTomlFile()) == PackageManager.POETRY){
            InitializeHabushuPoetry initializeHabushuPoetry = new InitializeHabushuPoetry(getPythonProjectBaseDir(), getLog(), overridePackageVersion, expectedPythonPackageVersion );
            initializeHabushuPoetry.doExecute();
        } else {
            InitializeHabushuUv initializeHabushuUv =  new InitializeHabushuUv(getPythonProjectBaseDir(), getLog(), overridePackageVersion, expectedPythonPackageVersion);
            initializeHabushuUv.doExecute();
        }
    }

}
