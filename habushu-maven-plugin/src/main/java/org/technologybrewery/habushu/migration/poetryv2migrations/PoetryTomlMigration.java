package org.technologybrewery.habushu.migration.poetryv2migrations;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.BatonException;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

/**
 * Provides functionality to remove deprecated configurations from poetry.toml file.
 * Runs when on Poetry v2.0.0 or later.
 */
public class PoetryTomlMigration extends AbstractPoetryMigration{
    private static final Logger logger = LoggerFactory.getLogger(PoetryTomlMigration.class);
    private static final String PREFER_ACTIVE_PYTHON = "prefer-active-python";
    private static final String SYSTEM_GIT_CLIENT = "system-git-client";
    private boolean hasDeprecatedVirtualEnvs = false;
    private boolean hasDeprecatedExperimental = false;
    private String systemGitClientRhs;
    private int numExperimental;

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        if (isPoetryVersionAtLeast2){
            try(FileConfig tomlFileConfig = FileConfig.of(file)){
                tomlFileConfig.load();
                Optional<Config> virtualEnvsGroupOpt = tomlFileConfig.getOptional(TomlUtils.VIRTUAL_ENVS);

                if (virtualEnvsGroupOpt.isPresent()){
                    Config virtualEnvsGroup = virtualEnvsGroupOpt.get();
                    hasDeprecatedVirtualEnvs = virtualEnvsGroup.contains(PREFER_ACTIVE_PYTHON);
                    if (hasDeprecatedVirtualEnvs){
                        logger.info("Removing deprecated configuration \"virtualenvs.prefer-active-python\"...");
                    }
                }

                Optional<Config> experimentalGroupOpt = tomlFileConfig.getOptional(TomlUtils.EXPERIMENTAL);
                if (experimentalGroupOpt.isPresent()){
                    Config experimentalGroup = experimentalGroupOpt.get();

                    if (experimentalGroup.contains(SYSTEM_GIT_CLIENT)){
                        hasDeprecatedExperimental = true;
                        systemGitClientRhs = experimentalGroup.get(SYSTEM_GIT_CLIENT).toString();
                        numExperimental = experimentalGroup.entrySet().size();
                        logger.info("Removing deprecated configuration \"experimental.system-git-client\"...");
                    }
                }
            }
        }
        return  hasDeprecatedVirtualEnvs || hasDeprecatedExperimental;
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        boolean inVirtualEnvsSection = false;
        boolean inExperimentalSection = false;
        boolean systemGitClientInjected = false;

        StringBuilder fileContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            String line = reader.readLine();

            while (line != null) {
                boolean addLine = true;
                String trimmedLine = line.strip();

                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")){
                    if (trimmedLine.equals("[" + TomlUtils.VIRTUAL_ENVS + "]")) {
                        inVirtualEnvsSection = true;
                        inExperimentalSection = false;
                    } else if (trimmedLine.equals(("[" + TomlUtils.EXPERIMENTAL + "]"))) {
                        inVirtualEnvsSection = false;
                        inExperimentalSection = true;
                        
                        if (hasDeprecatedExperimental && numExperimental==1){
                            // if we have the deprecated experimental.system-git-client AND it's the only entry in the section, then skip adding the header because it will be empty after the migration anyway
                            addLine = false;
                        }
                    } else {
                        inVirtualEnvsSection = false;
                        inExperimentalSection = false;
                    }
                }

                // if hasDeprecatedExperimental = true, then inject at top since system-git-client has no parent group entry
                if (hasDeprecatedExperimental && !systemGitClientInjected){
                    fileContent.append("system-git-client = ").append(systemGitClientRhs).append("\n\n");
                    systemGitClientInjected = true;
                }

                if (trimmedLine.contains(TomlUtils.EQUALS)){
                    if (inVirtualEnvsSection && trimmedLine.startsWith(PREFER_ACTIVE_PYTHON)){
                        addLine = false;
                    }
                    if (inExperimentalSection && trimmedLine.startsWith(SYSTEM_GIT_CLIENT)){
                        addLine = false;
                    }
                }

                if (addLine) {
                    fileContent.append(line).append("\n");
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new BatonException("Problem reading pyproject.toml for migration!", e);
        }

        try {
            TomlUtils.writeTomlFile(pyProjectTomlFile, fileContent.toString());
        } catch (IOException e) {
            throw new BatonException("Problem writing updated pyproject.toml!", e);
        }

        return true;
    }
}
