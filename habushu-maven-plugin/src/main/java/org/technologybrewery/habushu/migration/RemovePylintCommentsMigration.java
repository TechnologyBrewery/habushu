package org.technologybrewery.habushu.migration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.BatonException;
import org.technologybrewery.habushu.migration.poetryv2migrations.AbstractPoetryMigration;
import org.technologybrewery.habushu.util.HabushuUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides functionality to remove pylint comments (for pylint filtering) from python files
 */
public class RemovePylintCommentsMigration extends AbstractPoetryMigration {
    public static final String PYLINT_COMMENT = "# pylint";
    public static final String PYLINT_COMMENT_REGEX = "(?m)^\\s*#\\s*pylint\\b";

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        boolean foundPylintComment = false;
        try {
            String content = Files.readString(file.toPath());
            Pattern pattern = Pattern.compile(PYLINT_COMMENT_REGEX);
            Matcher matcher = pattern.matcher(content);

            foundPylintComment = matcher.find();
        } catch (IOException e) {
            throw new BatonException("Problem reading python file while checking for pylint comment", e);
        }

        return foundPylintComment;
    }

    @Override
    protected boolean performMigration(File pythonFile) {
        StringBuilder fileContent = new StringBuilder(StringUtils.EMPTY);
        try (BufferedReader reader = new BufferedReader(new FileReader(pythonFile))) {
            String line = reader.readLine();
            while (line != null) {
                if (line.contains(PYLINT_COMMENT)) {
                    removePylintCommentFromLine(line, fileContent);
                } else {
                    fileContent.append(line).append("\n");
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new BatonException("Problem reading python file while removing pylint comments!", e);
        }

        try {
            HabushuUtil.writeFile(pythonFile, fileContent.toString());
        } catch (IOException e) {
            throw new BatonException("Problem writing file while removing pylint comments from file!", e);
        }

        return true;
    }

    /**
     * Extracts the non-comment text from the line and adds it to the StringBuilder
     * @param line The line with a pylint comment
     * @param fileContent the StringBuilder that contains non-pylint-comment text
     */
    private static void removePylintCommentFromLine(String line, StringBuilder fileContent) {
        int pylintStartIndex = line.indexOf(PYLINT_COMMENT);
        String cleanLine = line.substring(0, pylintStartIndex -1);

        if(!cleanLine.trim().isEmpty()) {
            fileContent.append(cleanLine).append("\n");
        }
    }
}
