package org.technologybrewery.habushu.migration.poetryv2migrations;

import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.AbstractMigration;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.util.TomlReplacementTuple;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides common logic to migrate TOML file entries for Poetry versions 2.0.0 or later.
 * Updates configurations to better comply with PEP 621 standards (https://peps.python.org/pep-0621/).
 */
public abstract class AbstractPoetryMigration extends AbstractMigration {
    private static final Logger logger = LoggerFactory.getLogger(AbstractPoetryMigration.class);
    protected File workingDirectory;
    protected boolean isPoetryVersionAtLeast2 = checkPoetryVersionAtLeast2();

    @Override
    public void setMavenProject(MavenProject project) {
        super.setMavenProject(project);
        this.workingDirectory = project.getBasedir();
    }

    protected boolean checkPoetryVersionAtLeast2(){
        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(workingDirectory);
        return poetryHelper.isPoetryVersionAtLeastMinimumVersion();
    }

    public void setWorkingDirectory(File workingDirectory){
        this.workingDirectory = workingDirectory;
    }

    public void setIsPoetryVersionAtLeast2(boolean isPoetryVersionAtLeast2){
        this.isPoetryVersionAtLeast2 = isPoetryVersionAtLeast2;
    }

    /**
     * Converts a List of Strings containing authors/maintainer data into a toml-formatted string.
     */
    protected String convertListOfStringsToString(List<String> listOfStrings) {
        StringBuilder sb = new StringBuilder();
        int valuesRemaining = listOfStrings.size();
        sb.append("[");

        for (String listValue : listOfStrings) {
            int start = listValue.indexOf("<");
            int end = listValue.indexOf(">");

            if (start != -1 && end != -1 && end > start) {
                sb.append("{");
                sb.append(buildNameString(listValue.substring(0, start).strip()));
                sb.append(", ");
                sb.append(buildEmailString(listValue.substring(start + 1, end).strip()));
                sb.append("}");
            } else  {
                logger.warn("Field not in expected \"name <email>\" format!");
                if (!listValue.contains("@")){
                    logger.warn("Attempting to parse {} as name string", listValue);
                    sb.append("{").append(buildNameString(listValue)).append("}");
                } else {
                    logger.warn("Attempting to parse {} as e-mail string", listValue);
                    sb.append("{").append(buildEmailString(listValue)).append("}");
                }
            }
            valuesRemaining--;
            if (valuesRemaining > 0) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }


    protected String buildNameString(String name){
        return "name = " + TomlUtils.DOUBLE_QUOTE + name + TomlUtils.DOUBLE_QUOTE;
    }

    protected String buildEmailString(String email){
        return "email = " + TomlUtils.DOUBLE_QUOTE + email + TomlUtils.DOUBLE_QUOTE;
    }


    /**
     * Appends dependency definitions to the given file content if their keys are not already present in [project] group
     */
    protected StringBuilder injectDependencies(StringBuilder fileContent, List<String> existingProjectKeys, Map<String, TomlReplacementTuple> replacements) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, TomlReplacementTuple> entry : replacements.entrySet()) {
            // Only inject if the key is not already in the [project] section.
            if (!existingProjectKeys.contains(entry.getKey())) {
                sb.append(entry.getKey())
                        .append(" = ")
                        .append(TomlUtils.escapeTomlRightHandSide(entry.getValue().getOriginalOperatorAndVersion()))
                        .append("\n");
            } else {
                logger.warn("Skipping migration for duplicate entry: {}", entry.getKey());
            }
        }
        return fileContent.append(sb);
    }

    /**
     * Wraps each string in given list with double quotes and then join them with a comma and a space
     */
    protected String joinQuoted(List<String> list) {
        return list.stream()
                .map(item -> TomlUtils.DOUBLE_QUOTE + item + TomlUtils.DOUBLE_QUOTE)
                .collect(Collectors.joining(", "));
    }

}