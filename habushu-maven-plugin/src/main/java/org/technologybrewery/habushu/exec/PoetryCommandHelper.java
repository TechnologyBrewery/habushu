package org.technologybrewery.habushu.exec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.shell.exec.CommandHelper;
import org.technologybrewery.shell.exec.ProcessExecutor;

/**
 * Facilitates the execution of Poetry commands.
 */
public class PoetryCommandHelper extends CommandHelper {

    private static final String POETRY_COMMAND = "poetry";
    private static final Logger logger = LoggerFactory.getLogger(PoetryCommandHelper.class);

    private static final String extractVersionRegex = "[^0-9\\.]";

    public PoetryCommandHelper(File workingDirectory) {
	super(workingDirectory, POETRY_COMMAND);
    }

    /**
     * Returns a {@link Boolean} and {@link String} {@link Pair} indicating whether
     * Poetry is installed and if so, the version of Poetry that is installed. If
     * Poetry is not installed, the returned {@link String} part of the {@link Pair}
     * will be {@code null}.
     * 
     * @return
     */
    public Pair<Boolean, String> getIsPoetryInstalledAndVersion() {
	try {
	    String versionResult = execute(Arrays.asList("--version")).getStdout();

	    // Extracts version number from output, whether it's "Poetry version 1.1.15" or
	    // "Poetry (version 1.2.1)"
	    String version = versionResult.replaceAll(extractVersionRegex, "");
	    return new ImmutablePair<>(true, version);
	} catch (Throwable e) {
	    return new ImmutablePair<>(false, null);
	}
    }

    /**
     * Returns whether the specified dependency package is installed within this
     * Poetry project's virtual environment (and pyproject.toml).
     * 
     * @param packageName
     * @return
     */
    public boolean isDependencyInstalled(String packageName) {
	try {
	    execute(Arrays.asList("show", packageName));
	} catch (Throwable e) {
	    return false;
	}
	return true;

    }

    /**
     * Installs the specified package as a development dependency to this Poetry
     * project's virtual environment and pyproject.toml specification.
     * 
     * @param packageName
     */
    public void installDevelopmentDependency(String packageName) {
	execute(Arrays.asList("add", packageName, "--group", "dev"));
    }


    /**
     * Executes a Poetry command with the given arguments and logs a warning message
     * if the command has not yet completed after the specified timeout period. This
     * may be useful for providing input to developers when certain Poetry commands
     * are running for longer than expected and may need to be manually halted due
     * to cache-related issues.<br>
     * <b>NOTE:</b>The executed Poetry command will *not* be halted nor terminated
     * when the timeout expires. After the timeout expires, this method will
     * continue to wait until underlying Poetry command completes.
     * 
     * @param arguments
     * @param timeout
     * @param timeUnit
     * @return
     */
    public Integer executePoetryCommandAndLogAfterTimeout(List<String> arguments, int timeout, TimeUnit timeUnit) {
	ExecutorService executor = Executors.newSingleThreadExecutor();
	Future<Integer> future = executor.submit(() -> this.executeAndLogOutput(arguments));
	try {
	    return future.get(timeout, timeUnit);
	} catch (TimeoutException e) {
	    logger.warn("poetry " + String.join(" ", arguments)
		    + " has been running for quite some time, you may want to quit the mvn process (Ctrl+c) and run \"poetry cache clear . --all\" and restart your build.");
	    try {
		return future.get();
	    } catch (InterruptedException | ExecutionException e1) {
		throw new RuntimeException("Error occurred while waiting for Poetry command to complete", e1);
	    }
	} catch (Exception e) {
	    throw new RuntimeException(String.format("Error occurred while performing Poetry command: poetry %s",
		    StringUtils.join(arguments, " ")), e);
	} finally {
	    executor.shutdown();
	}
    }

    /**
     * Installs a Poetry plugin with the given name.
     * 
     * @param name
     * @return
	 */
    public int installPoetryPlugin(String name) {
	List<String> args = new ArrayList<String>();
	args.add("self");
	args.add("add");
	args.add(name);
	return this.executeAndLogOutput(args);
    }
}
