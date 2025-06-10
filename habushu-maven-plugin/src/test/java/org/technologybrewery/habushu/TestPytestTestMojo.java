package org.technologybrewery.habushu;

import org.technologybrewery.habushu.exec.CommandHelper;
import org.technologybrewery.habushu.exec.UvCommandHelper;

import java.io.File;

/**
 * Contains method to make testing easier and set deploy Mojo values that would be done by Maven in normal use.
 */
public class TestPytestTestMojo extends PytestTestMojo {

    public TestPytestTestMojo() {
        super();
    }

    protected File getPyProjectTomlFile() {
        return new File("src/test/resources/pytest-test-pyproject.toml");
    }

    protected File getPythonProjectBaseDir() {
        return new File("src/test/resources/");
    }

    protected CommandHelper getCommandHelper() {
        return new MockUvCommandHelper(getPyProjectTomlFile());
    }

    private class MockUvCommandHelper extends UvCommandHelper {

        public MockUvCommandHelper(File workingDirectory) {
            super(workingDirectory);
        }

        @Override
        public boolean isDependencyInstalled(String packageName) {
            return false;
        }
    }
}