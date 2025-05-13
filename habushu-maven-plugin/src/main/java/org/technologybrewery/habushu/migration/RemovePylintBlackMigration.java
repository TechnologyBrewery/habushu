package org.technologybrewery.habushu.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.technologybrewery.baton.BatonException;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;
import org.technologybrewery.habushu.migration.poetryv2migrations.AbstractPoetryMigration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides functionality to remove black and pylint dependencies from the project (pyproject.toml)
 */
public class RemovePylintBlackMigration extends AbstractPoetryMigration {
    public static final String BLACK_PYLINT_REGEX = "(?m)^\\s*(black|pylint)\\s*=\\s*\"[^\"]+\"";

    @Override
    protected boolean shouldExecuteOnFile(File file) {
        // Search the file for occurrences of pylint and behave
        boolean foundBlackOrPylint = false;
        if (isPoetryProject(file)){
            try {
                String content = Files.readString(file.toPath());
                Pattern pattern = Pattern.compile(BLACK_PYLINT_REGEX);
                Matcher matcher = pattern.matcher(content);

                foundBlackOrPylint = matcher.find();
            } catch (IOException e) {
                throw new BatonException("Problem reading pyproject.toml file while checking for pylint and black dependencies", e);
            }
        }

        return foundBlackOrPylint;
    }

    @Override
    protected boolean performMigration(File pyProjectTomlFile) {
        PoetryCommandHelper poetryHelper = new PoetryCommandHelper(pyProjectTomlFile.getParentFile());
        List<String> arguments = new ArrayList<>();
        arguments.add("remove");
        arguments.add("black");
        arguments.add("pylint");
        poetryHelper.execute(arguments);

        return true;
    }
}
