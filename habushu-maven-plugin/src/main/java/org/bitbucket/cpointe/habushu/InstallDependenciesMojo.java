package org.bitbucket.cpointe.habushu;

import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;

/**
 * Installs dependencies defined in the project's pyproject.toml configuration.
 * If desired, developers may enable the {@link #updateSnapshots} flag, which
 * will update all Habushu SNAPSHOT dependencies that are referenced in the
 * pyproject.toml configuration.
 */
@Mojo(name = "install-dependencies", defaultPhase = LifecyclePhase.COMPILE)
public class InstallDependenciesMojo extends AbstractHabushuMojo {

    @Parameter(property = "habushu.update.snapshots", defaultValue = "false")
    protected boolean updateSnapshots;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
	PoetryCommandHelper poetryHelper = createPoetryCommandHelper();
	getLog().info("Locking dependencies specified in pyproject.toml...");
	poetryHelper.executeAndLogOutput(Arrays.asList("lock"));

	if (this.updateSnapshots) {
	    // NB future iterations will constrain this flag to just Habushu snapshot
	    // dependencies
	    getLog().info(
		    "Updating to latest version of dependencies based on pyproject.toml defined version constraints...");
	    poetryHelper.executeAndLogOutput(Arrays.asList("update"));
	}

	getLog().info("Installing dependencies...");
	poetryHelper.executeAndLogOutput(Arrays.asList("install"));
    }

}
