package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

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
    	Scanner reader = null;
    	
    	try {
            reader = new Scanner(virtualEnvFile);
            while (reader.hasNextLine()) {
              String dependencyLine = reader.nextLine();
              
              if (StringUtils.isNotBlank(dependencyLine) && !dependencyLine.contains("#")) {
            	  dependencies.add(dependencyLine);
              }
            }
            
            reader.close();
          } catch (FileNotFoundException e) {
              throw new HabushuException("Error reading venv configuration file: {}", e);
          } finally {
        	  if (reader != null) {
        		  reader.close();
        	  }
          }
    	
    	return dependencies;
    }
}
