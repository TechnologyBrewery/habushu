package org.bitbucket.cpointe.habushu.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bitbucket.cpointe.habushu.HabushuException;

public class VirtualEnvFileHelper {
    private File virtualEnvFile;

    public VirtualEnvFileHelper(File virtualEnvFile) {
        this.virtualEnvFile = virtualEnvFile;
    }

	/**
	 * Obtains the list of virtual environment dependencies from the venv dependency file.
	 * @param venvDependencyFile the file listing out dependencies for the virtual environment
	 * @return the list of dependent packages needed for the virtual environment
	 */
	public List<String> readDependencyListFromFile() {
		List<String> dependencies = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(virtualEnvFile.getAbsolutePath()))) {
			String dependencyLine = reader.readLine();

			while (dependencyLine != null) {
				if (StringUtils.isNotBlank(dependencyLine) && !dependencyLine.contains("#")) {
					dependencies.add(dependencyLine);
				}

				dependencyLine = reader.readLine();
			}

		} catch (IOException e) {
			throw new HabushuException("Error reading venv configuration file: {}", e);
		}

		return dependencies;
	}
}
