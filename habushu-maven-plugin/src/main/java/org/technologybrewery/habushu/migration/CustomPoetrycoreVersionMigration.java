package org.technologybrewery.habushu.migration;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.AbstractMigration;
import org.technologybrewery.baton.BatonException;
import org.technologybrewery.habushu.HabushuException;
import org.technologybrewery.habushu.util.TomlReplacementTuple;
import org.technologybrewery.habushu.util.TomlUtils;
import org.technologybrewery.habushu.util.PoetryUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Automatically migrates Poetry-core version in the [build-system] group to the
 * version where the major version and minor version are inline with the
 * Poetry-core version required by Habushu defined by the POETRY_CORE_VERSION_REQUIREMENT in the
 * PoetryUtil. As noted in the project's README.md, this prevents these dependencies from causing issues
 * when Poetry projects are exported in development releases.
 */
public class CustomPoetrycoreVersionMigration extends AbstractMigration {

    public static final Logger logger = LoggerFactory.getLogger(CustomPoetrycoreVersionMigration.class);

    protected Map<String, TomlReplacementTuple> replacements = new HashMap<>();

    private boolean isMajorVersionUpdateRequired;
    private boolean isMinorVersionUpdateRequired;

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        try (FileConfig tomlFileConfig = FileConfig.of(file)) {
            tomlFileConfig.load();
            Optional<Config> toolBuildSystem = tomlFileConfig.getOptional(TomlUtils.BUILD_SYSTEM);
            if (toolBuildSystem.isPresent()) {
                Config buildSystem = toolBuildSystem.get();
                Map<String, Object> dependencyMap = buildSystem.valueMap();
                for (Map.Entry<String, Object> dependency : dependencyMap.entrySet()) {
                    isMajorVersionUpdateRequired = false;
                    isMinorVersionUpdateRequired = false;
                    // check if we need to upgrade the poetry-core version.
                   if(isPoetrycoreUpgradeRequired(dependency)) {
                       return true;
                   }
                }
            }
        }
        return false;
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        String fileContent = StringUtils.EMPTY;
        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            String line = reader.readLine();
            while (line != null) {
                boolean isEmptyLine = line.isBlank();

                if (line.contains(StringUtils.SPACE) && line.contains(TomlUtils.EQUALS)) {
                    String key = line.substring(0, line.indexOf(StringUtils.SPACE));
                    if (key == null) {
                        key = line.substring(0, line.indexOf(TomlUtils.EQUALS));
                    }

                    if (key != null) {
                        key = key.strip();

                        TomlReplacementTuple matchedTuple = replacements.get(key);
                        if ((matchedTuple != null) && (line.contains(TomlUtils.POETRY_CORE)) && (line.contains(TomlUtils.EQUALS))) {
                            // update the poetry-core major version upgrade if required.
                            StringBuilder stringBuilder = new StringBuilder(line);
                            if(isMajorVersionUpdateRequired){
                                int majorIndex = line.lastIndexOf(TomlUtils.EQUALS) + 1;
                                int endMajorIndex = line.indexOf(TomlUtils.DOT);
                                String poetryCoreReqMajorVer = TomlUtils.getMajorReqVersion(PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT);
                                stringBuilder = stringBuilder.replace(majorIndex, endMajorIndex, poetryCoreReqMajorVer);
                                line = stringBuilder.toString();
                                logger.debug("Updating the Poetry-core major version to {}.", poetryCoreReqMajorVer);
                            }

                            // update the poetry-core minor version if required.
                            if (isMinorVersionUpdateRequired) {
                                int minorVersionIndex = line.indexOf(TomlUtils.DOT) + 1;
                                int endMinorVersionIndex = line.lastIndexOf(TomlUtils.DOT);
                                String poetryCoreReqMinorVersion = TomlUtils.getMinorReqVersion(PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT);
                                stringBuilder = stringBuilder.replace(minorVersionIndex, endMinorVersionIndex, poetryCoreReqMinorVersion);
                                line = stringBuilder.toString();
                                logger.debug("Updating the Poetry-core minor version to {}.", poetryCoreReqMinorVersion);
                            }
                            logger.info("Updated the Poetry-core version to {} required by Habushu.", PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT);
                        }
                    }
                }
                fileContent += line + "\n";
                line = reader.readLine();
            }

        } catch (IOException e) {
            throw new HabushuException("Problem reading pyproject.toml while updating the build-sytem's Poetry-core version!", e);
        }

        try {
            TomlUtils.writeTomlFile(pyProjectTomlFile, fileContent);
        } catch (IOException e) {
            throw new BatonException("Problem while writing dependencies to TomlFile while updating the build-system's Poetry-core version!", e);
        }
        return true;
    }

    private boolean isPoetrycoreUpgradeRequired(Map.Entry<String, Object> dependency) {
        replacements.clear();
        String packageName = dependency.getKey();

        if (packageName.equals(TomlUtils.REQUIRES)) {
            String dependencyVal = dependency.getValue().toString();
            if (dependencyVal.contains(TomlUtils.POETRY_CORE) && dependencyVal.contains(TomlUtils.EQUALS)) {
                String poetrycoreVer = dependencyVal.substring(TomlUtils.getIndexOfFirstDigit(dependencyVal), TomlUtils.getIndexOfLastDigit(dependencyVal));
                int endMajorVerIndex = poetrycoreVer.indexOf(TomlUtils.DOT);
                int poetryCoreMajorVer = Integer.parseInt(poetrycoreVer.substring(0, endMajorVerIndex));
                int poetryCoreReqMajorVersion =  Integer.parseInt(TomlUtils.getMajorReqVersion(PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT));

                // if the major version is less than required major version then return true.
                if(poetryCoreMajorVer < poetryCoreReqMajorVersion) {
                    isMajorVersionUpdateRequired = true;
                    isMinorVersionUpdateRequired = true;
                }

                // Now check for the minor version. If the major version is equal to the required
                // major version but if the minor version is less than the required minor version
                // then upgrade for minor version is needed.
                int minorVerIndex = poetrycoreVer.indexOf(TomlUtils.DOT) + 1;
                int endMinorVerIndex = poetrycoreVer.lastIndexOf(TomlUtils.DOT);
                int poetryCoreMinorVer = Integer.parseInt(poetrycoreVer.substring(minorVerIndex, endMinorVerIndex));
                int poetryCoreReqMinorVersion = Integer.parseInt(TomlUtils.getMinorReqVersion(PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT));

                if ((poetryCoreMajorVer == poetryCoreReqMajorVersion && poetryCoreMinorVer < poetryCoreReqMinorVersion)) {
                    isMinorVersionUpdateRequired = true;
                }

                // Return true if major or minor version upgarde is required.
                if(isMajorVersionUpdateRequired || isMinorVersionUpdateRequired) {
                    TomlReplacementTuple replacementTuple = new TomlReplacementTuple(packageName, poetrycoreVer, "poetry-core>=1.6.0");
                    replacements.put(packageName, replacementTuple);
                    logger.debug("Found build-system's Poetry-core version : {} less than the required version for Habushu. It will be updated to the required version of {}.", poetrycoreVer, PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT);
                    return true;
                }
            }
        }
        return false;
    }
}
