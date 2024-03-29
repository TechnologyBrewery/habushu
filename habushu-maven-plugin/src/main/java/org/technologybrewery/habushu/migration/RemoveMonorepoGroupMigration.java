package org.technologybrewery.habushu.migration;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.AbstractMigration;
import org.technologybrewery.baton.BatonException;
import org.technologybrewery.habushu.HabushuException;
import org.technologybrewery.habushu.util.TomlReplacementTuple;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Automatically migrates any monorepo dependencies (e.g., foo = {path = "../foo", develop = true}) in the main
 * [tool.poetry.dependencies] group into the [tool.poetry.group.monorepo.dependencies] group instead.  As noted in the
 * project's README.md, this prevents these dependencies from causing issues when Poetry projects are exported in
 * development releases.
 */
public class RemoveMonorepoGroupMigration extends AbstractMigration {

    public static final Logger logger = LoggerFactory.getLogger(RemoveMonorepoGroupMigration.class);

    protected Map<String, TomlReplacementTuple> replacements = new HashMap<>();

    private boolean hasExistingDependenciesGroup = false;

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        replacements.clear();
        boolean shouldExecute = false;
        try (FileConfig tomlFileConfig = FileConfig.of(file)) {
            tomlFileConfig.load();

            Optional<Config> toolPoetryMonorepoDependencies = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY_GROUP_MONOREPO_DEPENDENCIES);
            if (toolPoetryMonorepoDependencies.isPresent()) {
                Config foundDependencies = toolPoetryMonorepoDependencies.get();
                Map<String, Object> dependencyMap = foundDependencies.valueMap();

                for (Map.Entry<String, Object> dependency : dependencyMap.entrySet()) {
                    String packageName = dependency.getKey();
                    Object packageRhs = dependency.getValue();
                    String packageRshAsString = null;
                    if (packageRhs instanceof CommentedConfig) {
                        packageRshAsString = TomlUtils.convertCommentedConfigToToml((CommentedConfig) packageRhs);
                    } else {
                        packageRshAsString = (String) packageRhs;
                    }
                    logger.info("Found local dependency within monorepo group! ({} = {})", packageName, packageRshAsString);
                    TomlReplacementTuple replacementTuple = new TomlReplacementTuple(packageName, packageRshAsString, "");
                    replacements.put(packageName, replacementTuple);
                }
                Optional<Config> toolPoetryDependecies = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY_DEPENDENCIES);
                hasExistingDependenciesGroup = toolPoetryDependecies.isPresent();
                shouldExecute = true;
            }
        }

        return shouldExecute;
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        String fileContent = StringUtils.EMPTY;
        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            String line = reader.readLine();
            boolean injectAfterNextEmptyLine = false;

            while (line != null) {
                boolean addLine = true;
                boolean isEmptyLine = line.isBlank();

                if (line.contains(StringUtils.SPACE) && line.contains(TomlUtils.EQUALS)) {
                    String key = line.substring(0, line.indexOf(StringUtils.SPACE));

                    if (key == null) {
                        key = line.substring(0, line.indexOf(TomlUtils.EQUALS));
                    }

                    if (key != null) {
                        key = key.strip();

                        TomlReplacementTuple matchedTuple = replacements.get(key);
                        if (matchedTuple != null) {
                            // skip this line, we will add it back to [tool.poetry.dependencies] later
                            addLine = false;
                        }
                    }

                } else if (line.contains("[") && line.contains("]")) {
                    String key = line.strip();

                    if (hasExistingDependenciesGroup && (key.equals("[" + TomlUtils.TOOL_POETRY_DEPENDENCIES + "]"))) {
                        // skip this line as we are overriding with the line plus dependencies here:
                        addLine = false;
                        fileContent += line + "\n";
                        fileContent = injectDependencies(fileContent);
                    } else if (!hasExistingDependenciesGroup) {
                        injectAfterNextEmptyLine = true;
                    }

                    if ((key.equals("[" + TomlUtils.TOOL_POETRY_GROUP_MONOREPO_DEPENDENCIES + "]"))){
                        addLine = false;
                    }
                }

                if (isEmptyLine && injectAfterNextEmptyLine) {
                    fileContent += "\n[" + TomlUtils.TOOL_POETRY_DEPENDENCIES + "]" + "\n";
                    fileContent = injectDependencies(fileContent);
                    injectAfterNextEmptyLine = false;
                }

                if (addLine) {
                    fileContent += line + "\n";
                }

                line = reader.readLine();
            }

        } catch (IOException e) {
            throw new HabushuException("Problem reading pyproject.toml to update with managed dependencies!", e);
        }

        try {
            TomlUtils.writeTomlFile(pyProjectTomlFile, fileContent);
        } catch (IOException e) {
            throw new BatonException("Problem moving monorepo dependencies to [tool.poetry.dependencies]!", e);
        }

        return true;

    }

    private String injectDependencies(String fileContent) {
        for (Map.Entry<String, TomlReplacementTuple> entry : replacements.entrySet()) {
            fileContent += entry.getKey() + " = " + TomlUtils.escapeTomlRightHandSide(entry.getValue().getOriginalOperatorAndVersion()) + "\n";
        }
        return fileContent;
    }
}
