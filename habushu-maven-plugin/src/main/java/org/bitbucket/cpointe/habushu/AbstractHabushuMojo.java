package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.bitbucket.cpointe.habushu.exec.PoetryCommandHelper;
import org.bitbucket.cpointe.habushu.exec.PyenvCommandHelper;

/**
 * Contains logic common across the various Habushu mojos.
 */
public abstract class AbstractHabushuMojo extends AbstractMojo {

    /**
     * The current Maven user's settings, pulled dynamically from their settings.xml
     * file.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    /**
     * Base directory in which Poetry projects will be located - should always be
     * the basedir of the encapsulating Maven project.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    protected File workingDirectory;

    /**
     * Folder in which Python source files are located - should align with Poetry's
     * project structure conventions.
     */
    @Parameter(property = "sourceDirectory", required = true, defaultValue = "${project.basedir}/src")
    protected File sourceDirectory;

    /**
     * Folder in which Python test files are located - should align with Poetry's
     * project structure conventions.
     */
    @Parameter(property = "testDirectory", required = true, defaultValue = "${project.basedir}/tests")
    protected File testDirectory;

    /**
     * Gets the canonical path for a file without having to deal w/ checked
     * exceptions.
     * 
     * @param file file for which to get the canonical format
     * @return canonical format
     */
    protected String getCanonicalPathForFile(File file) {
	try {
	    return file.getCanonicalPath();

	} catch (IOException ioe) {
	    throw new HabushuException("Could not access file: " + file.getName(), ioe);
	}
    }

    /**
     * Creates a {@link PyenvCommandHelper} that may be used to invoke Pyenv
     * commands from the project's working directory.
     * 
     * @return
     */
    protected PyenvCommandHelper createPyenvCommandHelper() {
	return new PyenvCommandHelper(this.workingDirectory);
    }

    /**
     * Creates a {@link PoetryCommandHelper} that may be used to invoke Poetry
     * commands from the project's working directory.
     * 
     * @return
     */
    protected PoetryCommandHelper createPoetryCommandHelper() {
	return new PoetryCommandHelper(this.workingDirectory);
    }
}
