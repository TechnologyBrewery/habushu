package org.bitbucket.cpointe.habushu;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.clean.CleanMojo;
import org.bitbucket.cpointe.habushu.util.HabushuUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An overridden Maven Clean plugin designed to conditionally exclude certain
 * folders. Specifically, this is intended to skip removing the
 * target/build-accelerator folder, except when the user enters the -force-clean
 * option that will intentionally remove it.
 */
@Mojo(name = "habushu-clean", defaultPhase = LifecyclePhase.CLEAN, threadSafe = true)
public class HabushuCleanMojo extends CleanMojo {

	private static final Logger logger = LoggerFactory.getLogger(HabushuCleanMojo.class);

	/**
	 * The base directory for running all Venv commands. Usually the target
	 * directory.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "workingDirectory", required = false)
	protected File workingDirectory;

	/**
	 * Whether the build should clear all directories in the target folder,
	 * including the one containing the file hash of the previous venv dependency
	 * file.
	 */
	@Parameter(property = "habushu.force.clean", required = true, defaultValue = "false")
	protected boolean habushuForceClean;

	@Override
	public void execute() throws MojoExecutionException {
		if (workingDirectory.exists()) {
			if (!habushuForceClean) {
				logger.debug(
						"Habushu force clean not selected.  Sparing the /build-accelerator/ and /virtualenvs/ folders.");
			} else {
				logger.debug(
						"Habushu force clean selected.  Clearing away /build-accelerator/ and /virtualenvs/ folders.");

				try {
					HabushuUtil.changePrivateCleanMojoField(this.getClass().getSuperclass(), "excludeDefaultDirectories", false);
				} catch (SecurityException e) {
					throw new HabushuException("Could not access field in CleanMojo", e);
				}

				super.execute();
			}
		}
	}
}
