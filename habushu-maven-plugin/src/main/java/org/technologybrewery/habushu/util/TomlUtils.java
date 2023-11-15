package org.technologybrewery.habushu.util;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

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

}
