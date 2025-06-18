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
import java.util.List;
import java.util.Optional;

/**
 * Provides functionality to remove empty headers from pyproject.toml file
 * Runs when on Poetry v2.0.0 or later.
 */
public class PoetryRemoveEmptyTomlMigration extends AbstractPoetryMigration{
    private static final Logger logger = LoggerFactory.getLogger(PoetryRemoveEmptyTomlMigration.class);
    private boolean isToolPoetryEmpty;
    private boolean isToolPoetryDepsEmpty;

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        if (super.shouldExecuteOnFile(file)) {
            try(FileConfig tomlFileConfig = FileConfig.of(file)){
                tomlFileConfig.load();
                List<String> allTomlSectionHeaders = TomlUtils.extractTomlSectionHeaders(file);

                Optional<Config> toolPoetryGroupOpt = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY);
                boolean hasToolPoetryHeader = allTomlSectionHeaders.contains(TomlUtils.TOOL_POETRY);

                // only check for emptiness if the header actually exists
                if (hasToolPoetryHeader && toolPoetryGroupOpt.isPresent()){
                    isToolPoetryEmpty = TomlUtils.isSectionEmpty(toolPoetryGroupOpt.get(), TomlUtils.TOOL_POETRY, allTomlSectionHeaders);
                }

                Optional<Config> toolPoetryDepsGroupOpt = tomlFileConfig.getOptional(TomlUtils.TOOL_POETRY_DEPENDENCIES);
                boolean hasToolPoetryDepsHeader = allTomlSectionHeaders.contains(TomlUtils.TOOL_POETRY_DEPENDENCIES);

                if (hasToolPoetryDepsHeader && toolPoetryDepsGroupOpt.isPresent()) {
                    isToolPoetryDepsEmpty = TomlUtils.isSectionEmpty(toolPoetryDepsGroupOpt.get(), TomlUtils.TOOL_POETRY_DEPENDENCIES, allTomlSectionHeaders);
                }
            }
        }
        return isToolPoetryEmpty || isToolPoetryDepsEmpty;
    }



    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        boolean inToolPoetrySection = false;
        boolean inToolPoetryDepsSection = false;
        StringBuilder fileContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            String line = reader.readLine();
            while (line != null) {
                boolean addLine = true;
                String trimmedLine = line.strip();

                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")){
                    if (trimmedLine.equals("[" + TomlUtils.TOOL_POETRY +"]")) {
                        inToolPoetrySection = true;
                        inToolPoetryDepsSection = false;
                        if(isToolPoetryEmpty){
                            logger.info("Removing empty [tool.poetry] header...");
                            addLine = false;
                        }
                    } else if (trimmedLine.equals("[" + TomlUtils.TOOL_POETRY_DEPENDENCIES +"]")){
                        inToolPoetrySection = false;
                        inToolPoetryDepsSection = true;
                        if (isToolPoetryDepsEmpty){
                            logger.info("Removing empty [tool.poetry.dependencies] header...");
                            addLine = false;
                        }
                    } else {
                        inToolPoetrySection = false;
                        inToolPoetryDepsSection = false;
                    }
                } else if (trimmedLine.isEmpty()){
                    // remove any blank lines following empty section headers
                    if (inToolPoetrySection && isToolPoetryEmpty){
                        addLine = false;
                        inToolPoetrySection = false;
                    } else if (inToolPoetryDepsSection && isToolPoetryDepsEmpty){
                        addLine = false;
                        inToolPoetryDepsSection = false;
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
