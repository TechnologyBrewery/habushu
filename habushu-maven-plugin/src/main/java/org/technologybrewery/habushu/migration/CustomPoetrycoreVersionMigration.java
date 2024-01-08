package org.technologybrewery.habushu.migration;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    private boolean isPoetryCoreVersionUpdateRequired;
    private static final String POETRY_CORE_REQUIRED_VERSION = PoetryUtil.POETRY_CORE_VERSION_REQUIREMENT.substring(1);

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        try (FileConfig tomlFileConfig = FileConfig.of(file)) {
            tomlFileConfig.load();
            Optional<Config> toolBuildSystem = tomlFileConfig.getOptional(TomlUtils.BUILD_SYSTEM);
            if (toolBuildSystem.isPresent()) {
                Config buildSystem = toolBuildSystem.get();
                Map<String, Object> dependencyMap = buildSystem.valueMap();
                for (Map.Entry<String, Object> dependency : dependencyMap.entrySet()) {
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
                if (line.contains(StringUtils.SPACE) && line.contains(TomlUtils.EQUALS)) {
                    String key = line.substring(0, line.indexOf(StringUtils.SPACE));
                    if (key == null) {
                        key = line.substring(0, line.indexOf(TomlUtils.EQUALS));
                    } else {
                        key = key.strip();
                        TomlReplacementTuple matchedTuple = replacements.get(key);
                        if ((matchedTuple != null) && (line.contains(TomlUtils.POETRY_CORE)) && (line.contains(TomlUtils.EQUALS))) {
                            // update the poetry-core version if required.
                            StringBuilder stringBuilder = new StringBuilder(line);

                            if(isPoetryCoreVersionUpdateRequired){
                                int versionIndex = line.lastIndexOf(TomlUtils.EQUALS) + 1;
                                int endVersionIndex = line.lastIndexOf(TomlUtils.DOT) + 2;
                                //This case will only occur when:
                                // requires = ["poetry-core>=1"] where there is no dot after 1
                                if(line.lastIndexOf(TomlUtils.DOT) == -1){
                                    endVersionIndex = versionIndex + 1;
                                }
                                line = stringBuilder.replace(versionIndex, endVersionIndex, POETRY_CORE_REQUIRED_VERSION).toString();
                                logger.info("Updating poetry-core version to {}.", POETRY_CORE_REQUIRED_VERSION);
                            }
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
        String packageName = dependency.getKey();
        Semver poetryCoreSemVerWithPatch;

        if (packageName.equals(TomlUtils.REQUIRES)) {
            String dependencyVal = dependency.getValue().toString();
            if (dependencyVal.contains(TomlUtils.POETRY_CORE) && dependencyVal.contains(TomlUtils.EQUALS)) {
                String poetrycoreVerFromToml = dependencyVal.substring(TomlUtils.getIndexOfFirstDigit(dependencyVal), TomlUtils.getIndexOfLastDigit(dependencyVal));
                String poetrycoreVer = formatSemVerString(poetrycoreVerFromToml);
                Semver poetryCoreSemVer = new Semver(poetrycoreVerFromToml, SemverType.NPM);
                Semver poetryCoreSemVerReqVersion =  new Semver(POETRY_CORE_REQUIRED_VERSION, SemverType.NPM);

                if(poetryCoreSemVer.isLowerThan(poetryCoreSemVerReqVersion)){
                    isPoetryCoreVersionUpdateRequired = true;
                    TomlReplacementTuple replacementTuple = new TomlReplacementTuple(packageName, poetrycoreVer, getUpdatedOperatorAndVersion());
                    replacements.put(packageName, replacementTuple);
                    logger.info("Found build-system's poetry-core version : {} less than the required version for Habushu. It will be updated to the required version of {}.", poetrycoreVer, POETRY_CORE_REQUIRED_VERSION);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Converts a major.minor version to major.minor.patch version
     * @param poetryCoreVer
     * @return String formatted in semantic version format
     */
    private String formatSemVerString(String poetryCoreVer){

        String formattedString = "0.0.0";

        //Adding a zero if patch version is missing.
        // Will be corrected to the right patch version in performMigration method
        if (Pattern.matches("\\d", poetryCoreVer)) {
            formattedString = poetryCoreVer + ".0.0";
        } else if (Pattern.matches("\\d\\.\\d", poetryCoreVer)) {
            formattedString = poetryCoreVer + ".0";
        } else if (Pattern.matches("\\d\\.\\d\\.\\d", poetryCoreVer)) {
            formattedString = poetryCoreVer;
        }

        return formattedString;
    }

    /**
     * This method will the PoetryUtil POETRY_CORE_VERSION_REQUIREMENT to
     * get the value and create an updatedOperatorAndVersion. Until this change
     * it was being hard coded. Refactoring so only one change is needed in PoetryUtil
     * when a new version is used
     * @return a string of updatedOperatorAndVersion
     */
    private String getUpdatedOperatorAndVersion(){
        return "poetry-core>="+POETRY_CORE_REQUIRED_VERSION;
    }

}
