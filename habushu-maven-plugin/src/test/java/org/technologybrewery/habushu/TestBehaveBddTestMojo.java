package org.technologybrewery.habushu;

import org.technologybrewery.habushu.exec.CommandHelper;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Contains method to make testing easier and set deploy Mojo values that would be done by Maven in normal use.
 */
public class TestBehaveBddTestMojo extends BehaveBddTestMojo {

    public TestBehaveBddTestMojo() {
        super();
    }

    protected File getPyProjectTomlFile() {
        return new File("src/test/resources/behave-bdd-test-pyproject.toml");
    }

    protected File getPythonProjectBaseDir() {
        return new File("src/test/resources/");
    }

    protected CommandHelper getCommandHelper() {
        return new CommandHelper() {
            @Override
            public String execute(List<String> arguments) {
                return "";
            }

            @Override
            public int executeAndLogOutput(List<String> arguments) {
                return 0;
            }

            @Override
            public int executeAndLogOutput(List<String> arguments, Map<String, String> environmentVariables) {
                return 0;
            }

            @Override
            public int executeWithSensitiveArgsAndLogOutput(List<Pair<String, Boolean>> argAndIsSensitivePairs) {
                return 0;
            }

            @Override
            public Integer executeAndLogAfterTimeout(List<String> arguments, int timeout, TimeUnit timeUnit, String messageToDisplay) {
                return 0;
            }

            @Override
            public boolean isDependencyInstalled(String packageName) {
                return false;
            }

            @Override
            public void installDevelopmentDependency(String packageName) {

            }

            @Override
            public String getProjectName() {
                return "";
            }

            @Override
            public String getProjectVersion() {
                return "";
            }
        };
    }

}
