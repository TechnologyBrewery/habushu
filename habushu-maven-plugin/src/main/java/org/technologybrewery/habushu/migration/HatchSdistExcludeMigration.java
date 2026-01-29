package org.technologybrewery.habushu.migration;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.BatonException;
import org.technologybrewery.habushu.util.TomlUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration that automatically adds {@code [tool.hatch.build.targets.sdist]} configuration
 * with {@code exclude = ["target/"]} for UV-based Habushu projects. This prevents the
 * Maven target directory from being included in source distributions.
 */
public class HatchSdistExcludeMigration extends AbstractHabushuMigration {

    private static final Logger logger = LoggerFactory.getLogger(HatchSdistExcludeMigration.class);

    private static final String TOOL_HATCH_BUILD_TARGETS_SDIST = "tool.hatch.build.targets.sdist";
    private static final String TOOL_HATCH_BUILD_TARGETS_SDIST_HEADER = "[tool.hatch.build.targets.sdist]";
    private static final String EXCLUDE_KEY = "exclude";
    private static final String TARGET_DIR = "target/";

    private List<String> existingExcludes = null;

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        if (!isUvProject(file)) {
            return false;
        }

        try (FileConfig toml = FileConfig.of(file)) {
            toml.load();

            List<String> excludeList = toml.get(TOOL_HATCH_BUILD_TARGETS_SDIST + "." + EXCLUDE_KEY);
            if (excludeList != null) {
                existingExcludes = new ArrayList<>(excludeList);
                if (excludeList.contains(TARGET_DIR)) {
                    // target/ is already excluded, no migration needed
                    return false;
                }
            } else {
                existingExcludes = null;
            }

            return true;
        } catch (Exception e) {
            throw new BatonException("Error reading pyproject.toml for hatch sdist exclude migration", e);
        }
    }

    @Override
    protected boolean performMigration(File file) {
        try {
            StringBuilder fileContent = new StringBuilder();
            boolean sectionFound = false;
            boolean excludeAdded = false;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmedLine = line.trim();

                    // Check if this is the sdist section header
                    if (trimmedLine.equals(TOOL_HATCH_BUILD_TARGETS_SDIST_HEADER)) {
                        sectionFound = true;
                        fileContent.append(line).append("\n");

                        if (existingExcludes != null) {
                            // Read through the section, skipping the exclude array (single or multi-line)
                            line = reader.readLine();
                            while (line != null && !line.trim().startsWith("[")) {
                                if (line.trim().startsWith("exclude")) {
                                    // Skip this line and any continuation lines if it's a multi-line array
                                    if (!line.contains("]")) {
                                        // Multi-line array - skip until we find the closing bracket
                                        while ((line = reader.readLine()) != null && !line.contains("]")) {
                                            // Skip array continuation lines
                                        }
                                        // The line with ']' is also skipped
                                    }
                                } else {
                                    // Keep non-exclude lines in the section
                                    fileContent.append(line).append("\n");
                                }
                                line = reader.readLine();
                            }
                            // Add the updated exclude list
                            fileContent.append(buildExcludeLine()).append("\n");
                            excludeAdded = true;
                            // Process the line that broke the loop (next section header or null)
                            if (line != null) {
                                fileContent.append(line).append("\n");
                            }
                        } else {
                            // Section exists but no exclude key - add exclude after the header
                            fileContent.append(buildExcludeLine()).append("\n");
                            excludeAdded = true;
                        }
                        continue;
                    }

                    fileContent.append(line).append("\n");
                }
            }

            // If section wasn't found, append it at the end
            if (!sectionFound && !excludeAdded) {
                fileContent.append("\n").append(TOOL_HATCH_BUILD_TARGETS_SDIST_HEADER).append("\n");
                fileContent.append(buildExcludeLine()).append("\n");
            }

            TomlUtils.writeTomlFile(file, fileContent.toString());
            logger.info("Added target/ to [tool.hatch.build.targets.sdist] exclude list in {}", file.getName());
            return true;
        } catch (IOException e) {
            throw new BatonException("Error performing hatch sdist exclude migration", e);
        }
    }

    private String buildExcludeLine() {
        List<String> excludes = new ArrayList<>();
        if (existingExcludes != null) {
            excludes.addAll(existingExcludes);
        }
        if (!excludes.contains(TARGET_DIR)) {
            excludes.add(TARGET_DIR);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(EXCLUDE_KEY).append(" = [");
        for (int i = 0; i < excludes.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(excludes.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
