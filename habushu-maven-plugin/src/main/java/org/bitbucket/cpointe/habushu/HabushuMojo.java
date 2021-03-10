package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

/**
 * A plugin to help include Venv-based projects in Maven builds. This helps keep
 * a single build command that can build the entire system with common lifecycle
 * needs like testings and packaging artifacts that are commonly skipped in
 * Python- and R-projects.
 */
@Mojo(name = "configure-environment", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class HabushuMojo extends AbstractHabushuMojo {

	private static final Logger logger = LoggerFactory.getLogger(HabushuMojo.class);

	/**
	 * Folder where the previous file hash of the venv dependency file is stored.
	 */
	@Parameter(property = "previousDependencyHashDirectory", required = true, defaultValue = "${project.build.directory}/build-accelerator/")
	protected File previousDependencyHashDirectory;

	/**
	 * The name of the .txt file storing the previous hash of the venv dependency
	 * file.
	 */
	@Parameter(property = "previousVenvDependencyFileHash", required = true, defaultValue = "previousDependencyFileHash.txt")
	protected String previousVenvDependencyFileHash;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();

		createPreviousDependencyFileHashDirectoryIfNeeded();

		boolean updateRequired = compareCurrentAndPreviousDependencyFileHashes();
		if (updateRequired) {
			logger.debug("Change detected in venv dependency file. Updating configuration.");
			installUnpackedPythonDependencies();
			installVenvDependencies();
			overwritePreviousDependencyHash();
		} else {
			logger.debug("No change detected in venv dependency file.");
		}
	}

	/**
	 * Compares the current and previous hashes of the venv dependency file.
	 * 
	 * @return true if the hashes/files are different, false if they're equal
	 */
	private boolean compareCurrentAndPreviousDependencyFileHashes() {
		String currentDependencyFileHash = null;
		String previousDependencyFileHash = null;
		String previousDependencyFilePath = previousDependencyHashDirectory.getAbsolutePath()
				+ previousVenvDependencyFileHash;

		File previousDependencyHashFile = new File(previousDependencyFilePath);
		if (previousDependencyHashFile.exists()) {
			try {
				HashFunction hashAlgorithm = Hashing.sha256();
				
				currentDependencyFileHash = Files.asByteSource(previousDependencyHashFile).hash(hashAlgorithm)
						.toString();
				previousDependencyFileHash = Files.asByteSource(venvDependencyFile).hash(hashAlgorithm).toString();
			} catch (IOException e) {
				throw new HabushuException("Error when attempting to create file hashes for comparison!", e);
			}
		} else {
			logger.debug("No previous venv dependency file hash found. Update required.");
			return true;
		}

		return !StringUtils.equals(currentDependencyFileHash, previousDependencyFileHash);
	}

	/**
	 * Overwrite the previous hash of the venv dependency file, updating it to the
	 * current one to reflect any changes.
	 */
	private void overwritePreviousDependencyHash() {
		logger.debug("Overwriting previous file hash with current one.");

		File previousDependencyHash = new File(previousVenvDependencyFileHash);
		File currentDependencyHash = new File(venvDependencyFile.getAbsolutePath());

		try {
			FileUtils.copyFile(currentDependencyHash, previousDependencyHash);
		} catch (IOException e) {
			throw new HabushuException("Error when trying to overwrite previous dependency file hash with current!", e);
		}
	}

	private void installUnpackedPythonDependencies() {
		List<File> dependencies = getDependencies();
		for (File dependency : dependencies) {
			File setupPyFile = new File(dependency, "setup.py");
			logger.debug("Unpacking dependency: {}", dependency.getName());
			if (setupPyFile.exists()) {
				VenvExecutor executor = createExecutorWithDirectory(dependency, PYTHON_COMMAND + " setup.py install");
				executor.executeAndGetResult(logger);
			}
		}
	}

	private List<File> getDependencies() {
		List<File> dependencies = new ArrayList<>();

		File dependencyDirectory = new File(workingDirectory, "dependency");
		if (dependencyDirectory.exists()) {
			dependencies = Arrays.asList(dependencyDirectory.listFiles());
		}

		return dependencies;
	}

	private void installVenvDependencies() {
		String pathToPip = pathToVirtualEnvironment + "/bin/pip";
		VirtualEnvFileHelper venvFileHelper = new VirtualEnvFileHelper(venvDependencyFile);
		List<String> dependencies = venvFileHelper.readDependencyListFromFile();

		for (String dependency : dependencies) {
			logger.debug("Installing dependency listed in dependency file: {}", dependency);

			VenvExecutor executor = createExecutorWithDirectory(venvDirectory, pathToPip + " install " + dependency);
			executor.executeAndRedirectOutput(logger);
		}
	}

	/**
	 * Create the directory that will contain the previous hash of the venv
	 * dependency file.
	 */
	private void createPreviousDependencyFileHashDirectoryIfNeeded() {
		if (!previousDependencyHashDirectory.exists()) {
			logger.debug("Previous dependency file hash directory did not exist - creating {}",
					getCanonicalPathForFile(previousDependencyHashDirectory));
			previousDependencyHashDirectory.mkdirs();
		} else {
			logger.debug("Previous dependency file hash directory already exists.");
		}
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}
}
