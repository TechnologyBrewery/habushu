package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.lang.reflect.Field;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.clean.CleanMojo;
import org.apache.maven.plugins.clean.Fileset;
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

				Fileset buildDirectory = new Fileset();

				String[] excludes = new String[2];
				excludes[0] = "build-accelerator/**";
				excludes[1] = "virtualenvs/**";

				try {
					FieldUtils.writeField(buildDirectory, "directory", workingDirectory, true);
					FieldUtils.writeField(buildDirectory, "excludes", excludes, true);
				} catch (IllegalAccessException e) {
					throw new HabushuException("Could not write to private field in Fileset class.", e);
				}

				Fileset[] filesets = new Fileset[1];
				filesets[0] = buildDirectory;

				setPrivateParentField("excludeDefaultDirectories", true);
				setPrivateParentField("filesets", filesets);
			} else {
				logger.debug(
						"Habushu force clean selected.  Clearing away /build-accelerator/ and /virtualenvs/ folders.");
			}

			super.execute();
		}
	}

	/**
	 * Sets a given field in the CleanMojo with a provided value.
	 * 
	 * @param fieldName the name of the field in CleanMojo
	 * @param value     the field's intended value
	 */
	private void setPrivateParentField(String fieldName, Object value) {
		Field fieldInParentClass;
		try {
			fieldInParentClass = this.getClass().getSuperclass().getDeclaredField(fieldName);
			fieldInParentClass.setAccessible(true);
			fieldInParentClass.set(this, value);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new HabushuException("Could not write to field in CleanMojo class.", e);
		}

	}
}
