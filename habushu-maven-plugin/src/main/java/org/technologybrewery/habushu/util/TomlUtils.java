package org.technologybrewery.habushu.util;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common utility methods for handling TOML files.
 */
public final class TomlUtils {

    public static final String EQUALS = "=";
    public static final String DOUBLE_QUOTE = "\"";
    public static final String TOOL_POETRY_DEPENDENCIES = "tool.poetry.dependencies";
    public static final String TOOL_POETRY_GROUP_MONOREPO_DEPENDENCIES = "tool.poetry.group.monorepo.dependencies";
    public static final String VERSION = "version";
    public static final String PATH = "path";
    public static final String DEVELOP = "develop";
    public static final String EXTRAS = "extras";

    public static final String BUILD_SYSTEM = "build-system";
    public static final String REQUIRES = "requires";
    public static final String POETRY_CORE ="poetry-core";
    public static final String DOT = ".";


    protected TomlUtils() {
        // prevent instantiation of all static class
    }

    public static boolean representsLocalDevelopmentVersion(Object rawData) {
        boolean localDevelopmentVersion = false;

        if (rawData instanceof CommentedConfig) {
            CommentedConfig config = (CommentedConfig) rawData;
            if (!config.contains(VERSION)) {
                localDevelopmentVersion = true;
            }

        }

        return localDevelopmentVersion;
    }

    /**
     * Handles escaping with double quotes only if the value is not an inline table.
     *
     * @param valueToEscape value to potentially escape
     * @return value ready to write to toml file
     */
    public static String escapeTomlRightHandSide(String valueToEscape) {
        return (!valueToEscape.contains("{")) ? DOUBLE_QUOTE + valueToEscape + DOUBLE_QUOTE : valueToEscape;
    }

    public static void writeTomlFile(File pyProjectTomlFile, String fileContent) throws IOException {
        if (fileContent != null) {
            try (Writer writer = new FileWriter(pyProjectTomlFile)) {
                writer.write(fileContent);
            }
        }
    }

    public static String convertCommentedConfigToToml(CommentedConfig config) {
        int valuesRemaining = config.size();

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        if (config.get(PATH) != null) {
            sb.append(PATH).append(" = \"").append(config.get(PATH).toString()).append("\"");
            valuesRemaining--;
            addCommaBetweenValues(valuesRemaining, sb);
        }

        if (config.get(DEVELOP) != null) {
            sb.append(DEVELOP).append(" = ").append(config.get(DEVELOP).toString());
            valuesRemaining--;
            addCommaBetweenValues(valuesRemaining, sb);
        }

        if (config.get(VERSION) != null) {
            sb.append(VERSION).append(" = \"").append(config.get(VERSION).toString()).append("\"");
            List<String> extras = config.get(EXTRAS);
            if (CollectionUtils.isNotEmpty(extras)) {
                sb.append(", ").append(EXTRAS).append(" = [");
                // NB: if we expect more complex values, such as multiple extras, more work would need to be done for
                // both consistent formatting and comparison of these values.  However, at the time of initially writing
                // this method, there isn't a clear demand signal, so we are going to KISS for now:

                for (int i = 0; i < extras.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append("\"").append(extras.get(i)).append("\"");
                }
                sb.append("]");
            }
        }

        sb.append("}");

        return sb.toString();
    }

    private static void addCommaBetweenValues(int valuesRemaining, StringBuilder sb) {
        if (valuesRemaining > 0) {
            sb.append(", ");
        }
    }

    /**
     * Finds and returns the index of the first digit in a given string.
     *
     * @param input string
     * @return index of the first digit. If not found then it will return -1.
     */
    public static int getIndexOfFirstDigit(String input) {
        Pattern pattern = Pattern.compile("\\d");
        Matcher matcher = pattern.matcher(input);
        if(matcher.find()){
            return matcher.start();
        }
        return -1;
    }

    /**
     * Finds and returns the index of the last digit in a given string.
     *
     * @param input string
     * @return index of the last digit. If not found then it will return -1.
     */
    public static int getIndexOfLastDigit(String input) {
        Pattern pattern = Pattern.compile("\\d");
        Matcher matcher = pattern.matcher(input);
        int lastDigitIndex = -1;
        while(matcher.find()){
            lastDigitIndex =  matcher.end();
        }
        return lastDigitIndex;
    }

}
