package org.technologybrewery.habushu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.technologybrewery.habushu.exec.PoetryCommandHelper;

/**
 * Leverages the lint package to validate both source and test Python
 * directories using Poetry's run command.
 */
@Mojo(name = "validate-python-code", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ValidatePythonCodeMojo extends AbstractHabushuMojo {

    protected static final String LINT_PACKAGE = "pylint";

    /**
     * Specifies disabled checkers for lint on source module.
     */
    @Parameter(property = "habushu.sourceLintDisabledChecker", required = false, defaultValue = "C,R,W")
    private String sourceLintDisabledChecker;

    /**
     * Specifies disabled checkers for lint on test module.
     */
    @Parameter(property = "habushu.testLintDisabledChecker", required = false, defaultValue = "C,R,W")
    private String testLintDisabledChecker;

    /**
     * Specifies enabled checkers for lint on source module.
     */
    @Parameter(property = "habushu.sourceLintEnabledChecker", required = false)
    private String sourceLintEnabledChecker;

    /**
     * Specifies enabled checkers for lint on test module.
     */
    @Parameter(property = "habushu.testLintEnabledChecker", required = false)
    private String testLintEnabledChecker;

    /**
     * By default, build will stop if lint errors are found in source module. Can be configured to true so that
     * build will continue. 
     */
    @Parameter(property = "habushu.sourceFailOnLintErrors", required = false, defaultValue = "false")
    private boolean sourceFailOnLintErrors;

    /**
     * By default, build will stop if lint errors are found in test module. Can be configured to true so that
     * build will continue. 
     */
    @Parameter(property = "habushu.testFailOnLintErrors", required = false, defaultValue = "false")
    private boolean testFailOnLintErrors;



    @Override
    public void doExecute() throws MojoExecutionException {
        PoetryCommandHelper poetryHelper = createPoetryCommandHelper();

        List<String> sourceDirectoriesToValidate = new ArrayList<>();
        if (this.sourceDirectory.exists()) {
            sourceDirectoriesToValidate.add(getCanonicalPathForFile(sourceDirectory));
        }

        if (sourceDirectoriesToValidate.isEmpty()) {
            getLog().warn(String.format("Configured source (%s) directories does not exist - skipping...",
                sourceDirectory));
        }

        if (!poetryHelper.isDependencyInstalled(LINT_PACKAGE)) {
            getLog().info(
                String.format("%s dependency not specified in pyproject.toml - installing now...", LINT_PACKAGE));
            poetryHelper.installDevelopmentDependency(LINT_PACKAGE);
        }

        List<String> executeLintArgsSource = new ArrayList<>();

        executeLintArgsSource.addAll(Arrays.asList("run", LINT_PACKAGE));
        executeLintArgsSource.addAll(sourceDirectoriesToValidate);

        if (StringUtils.isNotEmpty(sourceLintDisabledChecker)) {
            executeLintArgsSource.addAll(Arrays.asList("--disable", sourceLintDisabledChecker));
        }

        if (StringUtils.isNotEmpty(sourceLintEnabledChecker)) {
            executeLintArgsSource.addAll(Arrays.asList("--enable", sourceLintEnabledChecker));
        }

        if (sourceFailOnLintErrors) {
            executeLintArgsSource.add("--exit-zero");
        }

        getLog().info("Validating code in source directory using Pylint...");
        poetryHelper.executeAndLogOutput(executeLintArgsSource);

        List<String> testDirectoriesToValidate = new ArrayList<>();

        if (this.testDirectory.exists()) {
            testDirectoriesToValidate.add(getCanonicalPathForFile(testDirectory));
        }

        if (testDirectoriesToValidate.isEmpty()) {
            getLog().warn(String.format("Configured source (%s) directories does not exist - skipping...",
                testDirectory));
        }

        List<String> executeLintArgsTest = new ArrayList<>();

        executeLintArgsTest.addAll(Arrays.asList("run", LINT_PACKAGE));
        executeLintArgsTest.addAll(testDirectoriesToValidate);

        if (StringUtils.isNotEmpty(testLintDisabledChecker)) {
            executeLintArgsTest.addAll(Arrays.asList("--disable", testLintDisabledChecker));
        }

        if (StringUtils.isNotEmpty(testLintEnabledChecker)) {
            executeLintArgsTest.addAll(Arrays.asList("--enable", testLintEnabledChecker));
        }

        if (testFailOnLintErrors) {
            executeLintArgsTest.add("--exit-zero");
        }

        getLog().info("Validating code in test directory using Pylint...");
        poetryHelper.executeAndLogOutput(executeLintArgsTest);
    }
}
