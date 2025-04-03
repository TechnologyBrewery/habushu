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
    public static final String TOOL_POETRY = "tool.poetry";
    public static final String TOOL_UV_PROJECT = "project";
    public static final String TOOL_UV_DEPENDENCY_GROUPS = "dependency-groups";
    public static final String TOOL_POETRY_DEPENDENCIES = "tool.poetry.dependencies";
    public static final String TOOL_POETRY_DEV_DEPENDENCIES = "tool.poetry.group.dev.dependencies";
    public static final String TOOL_POETRY_GROUP_MONOREPO_DEPENDENCIES = "tool.poetry.group.monorepo.dependencies";
    public static final String PROJECT = "project";
    public static final String VERSION = "version";
    public static final String OPERATOR = "operator";
    public static final String PATH = "path";
    public static final String DEVELOP = "develop";
    public static final String EXTRAS = "extras";
    public static final String REQUIRES_PYTHON = "requires-python";
    public static final String DYNAMIC = "dynamic";
    public static final String PYTHON = "python";
    public static final String README = "readme";
    public static final String DEPENDENCIES = "dependencies";
    public static final String VIRTUAL_ENVS = "virtualenvs";
    public static final String EXPERIMENTAL = "experimental";

    public static final String BUILD_SYSTEM = "build-system";
    public static final String REQUIRES = "requires";
    public static final String POETRY_CORE ="poetry-core";
    public static final String DOT = ".";
    public static final String DOT_REGEX = "\\.";
    public static final String PYPROJECT_TOML = "pyproject.toml";
    public static final String CARROT = "^";
    public static final String CARROT_REGEX = "\\^";
    public static final String GREATER_THAN = ">";
    public static final String GREATER_THAN_OR_EQUAL_TO = ">=";
    public static final String COMMA = ",";
    public static final String LESS_THAN = "<";
    public static final String DEFAULT_LOWER_BOUND = "0.0.0";
    public static final List<String> COMPARATORS = List.of(">","<",">=","<=","==");

    /**
     * Given a package in format "[<packagename><lowerboundVersion>,<upperboundVersion>]"
     *      e.g. "[poetry-core>=1.0.0,<2.0.0]"
     * Then Regex captures <packagename> as capture group "package"
     * And <lowerboundVersion>,<upperboundVersion> as capture group "version"
     * Only one of <lowerboundVersion> and <upperboundVersion> may be provided by omitting the comma
     */
    private static final String PACKAGE_AND_VERSION_REGEX = "^\\[(?<package>[^\\s>=<,]+)(?<version>(?:[>=<][^,]+)(?:,[>=<][^,]+)*)?\\]$";

    /**
     * Given a semantic version <operator><version>
     *     e.g. package-a>=2.0.0
     * The regex captures <operator> (package-a) and <version> (>=2.0.0) as separate groups named "operator" and "version"
     */
    private static final String OPERATOR_AND_VERSION_REGEX = "^(?<operator>[><=]+)(?<version>\\d+(?:\\.\\d+)?(?:\\.\\d+)?)$";


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
     * Given a string "<packagename><comparators and versions>"
     * e.g. package-a>=2.0.0,<3.0.0
     * The function will separate "<packagename>" and "<comparators and versions>"
     * e.g. "package-a" and ">=2.0.0,<3.0.0"
     * @param versionRequirementsWithName
     * @return version requirements without the package name
     */
    public static String getVersionRequirementsWithoutPackageName(String versionRequirementsWithName) {
        Pattern pattern = Pattern.compile(PACKAGE_AND_VERSION_REGEX);
        Matcher matcher = pattern.matcher(versionRequirementsWithName);

        if (matcher.matches()) {
            return matcher.group(VERSION);
        } else {
            return null;
        }
    }

    /**
     * Increments the number of a semantic version by 1
     * e.g. 1.0.0 -> 1.0.1
     * minor and patch versions >=9 will assume the major and minor versions will be bumped up, respectively.
     * e.g. 1.9.9 -> 2.0.0, 1.0.9 -> 1.1.0, 1.10.9 -> 2.0.0
     * @param version
     * @return the next higher semantic version in sequence
     */
    public static String incrementSemVersion(String version) {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        // Increment the version appropriately
        if (patch < 9) {
            patch++;
        } else {
            patch = 0;
            if (minor < 9) {
                minor++;
            } else {
                minor = 0;
                major++;
            }
        }

        return major + "." + minor + "." + patch;
    }

    /**
     *
     * Decrements the number of a semantic version by 1
     * e.g. 1.0.1 -> 1.0.0
     * minor and patch versions of 0 will assume the major and minor versions will be bumped down, respectively.
     * e.g. 2.0.0->1.9.9, 1.1.0 -> 1.0.9, 2.0.0 -> 1.9.9
     * @param version
     * @return the next lower semantic version in sequence
     */
    public static String decrementSemVersion(String version) {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        // Decrement the version appropriately
        if (patch > 0) {
            patch--;
        } else {
            patch = 9;
            if (minor > 0) {
                minor--;
            } else {
                minor = 9;
                if (major > 0) {
                    major--;
                } else {
                    // Version is at 0.0.0, cannot decrement further
                    return DEFAULT_LOWER_BOUND;
                }
            }
        }

        return major + "." + minor + "." + patch;
    }

    /**
     * For a given String <operator><version>, returns the <operator> and <version> as separate parts
     * e.g. ">=2.0.0" -> ">=" and "2.0.0"
     * @param constraint the version constraint provided
     * @return the given
     * @see org.technologybrewery.habushu.util.TomlUtils.VersionParts
     */
    public static VersionParts parseVersionConstraint(String constraint) {
        // Regular expression to match the operator and version
        Pattern pattern = Pattern.compile(OPERATOR_AND_VERSION_REGEX);
        Matcher matcher = pattern.matcher(constraint.trim());

        if (matcher.matches()) {
            String operator = matcher.group(TomlUtils.OPERATOR);
            String version = matcher.group(TomlUtils.VERSION);
            return new VersionParts(operator, version);
        }
        return null;
    }

    /**
     * <pre>Helper class to hold the operator and version of a given semantic version constraint</pre>
     * Contains getters for:
     * @code String operator
     * @code String version
     */
    public static class VersionParts {
        private final String operator;
        private final String version;

        public VersionParts(String operator, String version) {
            this.operator = operator;
            this.version = version;
        }

        public String getOperator() {
            return operator;
        }

        public String getVersion() {
            return version;
        }
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

    /**
     * Converts the semver syntax "^" to use greater than or equal to equivalent syntax
     * e.g. ^1.0.0 -> >=1.0.0,<2.0.0
     * @param semver the initial semantic version using a "^"
     * @return the adjusted semantic version using ">=" and "<"
     */
    public static String refactorCarrotIntoGreaterThanLessThan(String semver) {
        String[] carrotSplit = semver.split(CARROT_REGEX);
        String semverNoComparator = carrotSplit[1];

        Integer nextMajorSemver;

        if (semverNoComparator.contains(DOT)) { // e.g. ^3.11 turns into >=3.11,<4
            String[] semverSplit = semverNoComparator.split(DOT_REGEX);
            nextMajorSemver = Integer.parseInt(semverSplit[0]) + 1;
        } else { // e.g. ^3 turns into >=3,<4
            nextMajorSemver = Integer.parseInt(semverNoComparator) + 1;
        }

        StringBuilder newSemver = new StringBuilder()
                .append(GREATER_THAN_OR_EQUAL_TO)
                .append(semverNoComparator)
                .append(COMMA)
                .append(LESS_THAN)
                .append(nextMajorSemver);

        return newSemver.toString();
    }

    /**
     * Converts a major.minor version or major version to major.minor.patch version
     * @param poetryCoreVer
     * @return String formatted in semantic version format
     */
    public static String formatSemVerString(String poetryCoreVer){

        String formattedString = DEFAULT_LOWER_BOUND;

        //Adding a zero if patch version is missing.
        // Will be corrected to the right patch version in performMigration method
        if (Pattern.matches("\\d\\.\\d\\.\\d", poetryCoreVer)) {
            formattedString = poetryCoreVer;
        } else if (Pattern.matches("\\d\\.\\d", poetryCoreVer)) {
            formattedString = poetryCoreVer + ".0";
        } else if (Pattern.matches("\\d", poetryCoreVer)) {
            formattedString = poetryCoreVer + ".0.0";
        }

        return formattedString;
    }

    /**
     * Checks if a string contains comparators used in semantic versioning
     * Comparators are determined by {@code List} constant {@code COMPARATORS}
     * @param string
     * @return a boolean indicating whether the string contains comparators
     */
    public static boolean hasComparators(String string) {
        return COMPARATORS.stream().anyMatch(string::contains);
    }

}
