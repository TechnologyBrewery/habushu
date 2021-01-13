package org.bitbucket.cpointe.habushu;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class CondaFileHelper {
    private File condaConfigurationFile;

    public CondaFileHelper(File condaConfigurationFile) {
        this.condaConfigurationFile = condaConfigurationFile;
    }

    /**
     * Gets the name of the virtual environment specified in the conda yaml file
     * @return name of the virtual environment
     */
    public String getCondaEnvironmentName() {
        String environmentName;
        Map<String, Object> condaEnvironment;

        condaEnvironment = getCondaEnvironment();

        if (!condaConfigurationFile.exists()) {
            throw new HabushuException("Specified configuration file '" + condaConfigurationFile + "' does not exist!");
        }

        environmentName = (String) condaEnvironment.get("name");

        return environmentName;
    }

    /**
     * Gets the map equivalent of the conda yaml file
     * @return map of the conda yaml file
     */
    public Map<String, Object> getCondaEnvironment() {
        Map<String, Object> condaEnvironment;
        Yaml condaYaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(condaConfigurationFile)) {
            condaEnvironment = condaYaml.load(inputStream);
        } catch (IOException e) {
            throw new HabushuException("Problem reading conda yaml file!", e);
        }
        return condaEnvironment;
    }

}
