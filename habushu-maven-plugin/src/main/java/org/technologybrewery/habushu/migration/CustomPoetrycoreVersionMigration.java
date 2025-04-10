package org.technologybrewery.habushu.migration;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class CustomPoetrycoreVersionMigration extends AbstractHabushuMigration {

    public static final Logger logger = LoggerFactory.getLogger(CustomPoetrycoreVersionMigration.class);
    protected Map<String, TomlReplacementTuple> replacements = new HashMap<>();
    private boolean isPoetryCoreVersionUpdateRequired;

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        boolean shouldExecute = false;
        if (isPoetryProject(file)) {
            try (FileConfig tomlFileConfig = FileConfig.of(file)) {
                tomlFileConfig.load();
                Optional<Config> toolBuildSystem = tomlFileConfig.getOptional(TomlUtils.BUILD_SYSTEM);
                if (toolBuildSystem.isPresent()) {
                    Config buildSystem = toolBuildSystem.get();
                    Map<String, Object> dependencyMap = buildSystem.valueMap();
                    for (Map.Entry<String, Object> dependency : dependencyMap.entrySet()) {
                        // check if we need to upgrade the poetry-core version.
                        if(isPoetryCoreUpgradeRequired(dependency)) {
                            shouldExecute = true;
                        }
                    }
                }
            }
        }
        return shouldExecute;
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        StringBuilder fileContent = new StringBuilder(StringUtils.EMPTY);
        try (BufferedReader reader = new BufferedReader(new FileReader(pyProjectTomlFile))) {
            String line = reader.readLine();
            while (line != null) {
                if (line.contains(StringUtils.SPACE) && line.contains(TomlUtils.EQUALS)) {
                    TomlReplacementTuple matchedTuple = getTomlReplacementTuple(line);
                    if (isAPythonCoreRequirement(matchedTuple, line)) {
                        // update the poetry-core version if required.
                        StringBuilder stringBuilder = new StringBuilder(line);

                        if(isPoetryCoreVersionUpdateRequired){
                            line = substituteWithDefaultPoetryCoreRequirement(matchedTuple, line, stringBuilder);
                            logger.info("Updating poetry-core version to {}.", PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT);
                        }
                    }
                }
                fileContent.append(line).append("\n");
                line = reader.readLine();
            }

        } catch (IOException e) {
            throw new HabushuException("Problem reading pyproject.toml while updating the build-sytem's Poetry-core version!", e);
        }

        try {
            TomlUtils.writeTomlFile(pyProjectTomlFile, fileContent.toString());
        } catch (IOException e) {
            throw new BatonException("Problem while writing dependencies to TomlFile while updating the build-system's Poetry-core version!", e);
        }
        return true;
    }

    private TomlReplacementTuple getTomlReplacementTuple(String line) {
        String key = line.substring(0, line.indexOf(StringUtils.SPACE));
        key = key.strip();
        return replacements.get(key);
    }

    private static String substituteWithDefaultPoetryCoreRequirement(TomlReplacementTuple matchedTuple, String line, StringBuilder stringBuilder) {
        String originalOpAndVer = matchedTuple.getOriginalOperatorAndVersion();

        int versionIndex = line.indexOf(originalOpAndVer);
        int endVersionIndex = versionIndex + originalOpAndVer.length();
        line = stringBuilder.replace(
                versionIndex,
                endVersionIndex,
                matchedTuple.getUpdatedOperatorAndVersion()
        ).toString();
        return line;
    }

    private static boolean isAPythonCoreRequirement(TomlReplacementTuple matchedTuple, String line) {
        return (matchedTuple != null) && (line.contains(TomlUtils.POETRY_CORE)) && TomlUtils.hasComparators(line);
    }

    private boolean isPoetryCoreUpgradeRequired(Map.Entry<String, Object> dependency) {
        String packageName = dependency.getKey();

        if (packageName.equals(TomlUtils.REQUIRES)) {
            String dependencyVal = dependency.getValue().toString();
            if (dependencyVal.contains(TomlUtils.POETRY_CORE) && TomlUtils.hasComparators(dependencyVal)) {
                String poetryCoreVerFromToml = TomlUtils.getVersionRequirementsWithoutPackageName(dependencyVal);
                if (poetryCoreVerFromToml == null) {
                    throw new HabushuException( new StringBuilder()
                            .append("Unable to parse given semantic versioning constraints of ")
                            .append(TomlUtils.REQUIRES).append(" section in ").append(TomlUtils.BUILD_SYSTEM)
                            .append(" of file ").append(TomlUtils.PYPROJECT_TOML).toString()
                    );
                }

                PoetryCoreRequirement providedPoetryCoreRange = PoetryCoreRequirement
                        .buildHabushu(poetryCoreVerFromToml);

                PoetryCoreRequirement requiredPoetryCoreRange = PoetryCoreRequirement
                        .buildHabushu(PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT);

                if(!requiredPoetryCoreRange.isEncompassedBy(providedPoetryCoreRange)){
                    isPoetryCoreVersionUpdateRequired = true;
                    TomlReplacementTuple replacementTuple = new TomlReplacementTuple(
                            packageName,
                            poetryCoreVerFromToml,
                            PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT
                    );

                    replacements.put(packageName, replacementTuple);
                    logger.info(
                            "Found build-system's poetry-core version requirements :" +
                            " {} less than the required version for Habushu." +
                            " It will be updated to include the required version of {}.",
                            providedPoetryCoreRange,
                            PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT
                    );
                    return true;
                }
            }
        }
        return false;
    }
}
