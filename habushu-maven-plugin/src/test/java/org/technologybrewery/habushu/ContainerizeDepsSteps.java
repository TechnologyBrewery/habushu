package org.technologybrewery.habushu;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.technologybrewery.habushu.util.ContainerizeDepsDockerfileHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainerizeDepsSteps {
    private static final String LIB_WHEEL = "renamed_python_dep_Y-1.0.0-py3-none-any.whl";
    private static final String APP_WHEEL = "extensions_python_dep_X-1.0.0.dev0-py3-none-any.whl";
    private static final String POM_FILE = "pom.xml";
    private static final String POETRY = "poetry";
    public static final String CUSTOM_REPO = "https://pypi.example.com/main";
    public static final String CUSTOM_DEV_REPO = "https://pypi.example.com/dev/";

    protected String targetDefaultSingleMonorepoDepPath = "target/test-classes/containerize-dependencies/"
            + "default-single-monorepo-dep";

    protected String poetryMonorepoDepPath = targetDefaultSingleMonorepoDepPath + "/poetry-monorepo";
    protected String uvMonorepoDepPath = targetDefaultSingleMonorepoDepPath + "/uv-monorepo";
    protected File dockerfile;
    protected String mavenProjectPath;

    private final ContainerizeDepsMojoTestWrapper mojoTestCase = new ContainerizeDepsMojoTestWrapper();

    private ContainerizeDepsMojo mojo;

    @Before("@containerizeDependencies")
    public void configureMavenTestSession() throws Exception {
        // important for registering Habushu's mojos to AbstractTestCase.mojoDescriptors,
        // which ensures that lookupConfiguredMojo will return the configured mojo
        mojoTestCase.configurePluginTestHarness();
    }

    @After("@containerizeDependencies")
    public void tearDownMavenPluginTestHarness() throws Exception {
        mojoTestCase.clearMavenProjectFiles();
        mojoTestCase.tearDownPluginTestHarness();
        if (mojo != null && Files.exists(mojo.getStagingPath())) {
            FileUtils.deleteDirectory(mojo.getStagingPath().toFile());
        }
    }

    @Given("a single {string}-based dependency with packaging type Habushu")
    public void a_single_package_manager_based_dependency_with_packaging_type_habushu(String packageManager) throws Exception {
        String projectPath = getPackageManagerProjectPath(packageManager);

        mavenProjectPath = projectPath + "/extensions/extensions-monorepo-dep-consuming-application";

        // enables us to mock a maven build with the --also-make flag
        mojoTestCase.addMavenProjectFile(new File(projectPath + "/extensions/extensions-python-dep-X/pom.xml"));
        mojoTestCase.addMavenProjectFile(new File(projectPath + "/foundation/foundation-python-dep-Y/pom.xml"));

        mojo = (ContainerizeDepsMojo) mojoTestCase.lookupConfiguredMojo(
                new File(mavenProjectPath, POM_FILE), "containerize-dependencies"
        );
        mojo.session.getRequest().setBaseDirectory(new File(projectPath));

    }

    @Given("the pypiRepoUrl is set to a custom repository")
    public void thePypiRepoUrlIsSetToACustomRepository() {
        mojo.pypiRepoUrl = CUSTOM_REPO;
    }

    @Given("habushu is configured to use a dev repository")
    public void habushuIsConfiguredToUseADevRepository() {
        mojo.useDevRepository = true;
    }

    @Given("the dev repository url is set to a custom repository")
    public void theDevRepositoryUrlIsSetToACustomRepository() {
        mojo.devRepositoryUrl = CUSTOM_DEV_REPO;
    }

    @When("the containerize-dependencies goal is executed")
    public void the_containerize_dependencies_goal_is_executed() throws MojoExecutionException, MojoFailureException {
        mojo.execute();
    }

    @Then("the wheels of the dependency and transitive monorepo dependencies are staged in the build directory")
    public void the_wheels_of_the_dependency_are_staged_in_the_build_directory() {
        assertStaged(mojo.getStagingPath());
    }

    @Given("a dockerfile to update")
    public void a_dockerfile_to_update() {
        setDockerfile("/src/main/resources/docker/Dockerfile");
    }

    @Then("the Dockerfile installs the wheels to a virtual environment in the correct order")
    public void the_dockerfile_installs_the_wheels_to_avirtual_environment_in_the_correct_order() throws IOException {
        List<String> updatedDockerfile = getUpdatedDockerfile();

        // Confirm the Habushu logic is present in the Dockerfile
        Assertions.assertTrue(updatedDockerfile.contains(ContainerizeDepsDockerfileHelper.HABUSHU_BUILDER_STAGE + ContainerizeDepsDockerfileHelper.HABUSHU_COMMENT_START),
                "The Dockerfile is updated with `#HABUSHU_BUILDER_STAGE - HABUSHU GENERATED CODE (DO NOT MODIFY)` comment ");
        Assertions.assertTrue(updatedDockerfile.contains(ContainerizeDepsDockerfileHelper.HABUSHU_BUILDER_STAGE + ContainerizeDepsDockerfileHelper.HABUSHU_COMMENT_END),
                "The Dockerfile is updated with `#HABUSHU_BUILDER_STAGE - HABUSHU GENERATED CODE (END)` comment ");

        var install = new FileLoc();
        var depX = new FileLoc();
        var depY = new FileLoc();
        for (int ln = 0; ln < updatedDockerfile.size(); ln++) {
            String eachLine = updatedDockerfile.get(ln);
            int col = eachLine.indexOf("pip install");
            if (col >= 0) {
                install.ln = ln;
                install.col = col;
            }
            col = eachLine.indexOf(APP_WHEEL);
            if (col >= 0 && install.before(ln, col) ) {
                depX.ln = ln;
                depX.col = col;
            }
            col = eachLine.indexOf(LIB_WHEEL);
            if (col >= 0 && install.before(ln, col)) {
                depY.ln = ln;
                depY.col = col;
            }
            if (depX.found() && depY.found()) {
                break;
            }
        }

        Assertions.assertTrue(depX.found(), "extensions-python-dep-X wheel not installed in Dockerfile");
        Assertions.assertTrue(depY.found(), "extensions-python-dep-Y wheel not installed in Dockerfile");
        Assertions.assertTrue(depY.before(depX), "Wheels not installed in the correct order in Dockerfile");
    }

    @Then("the Dockerfile is updated to leverage a virtual environment for the dependency")
    public void dockerfile_is_updated_to_build_a_virtual_env_for_the_dependency() throws IOException {
        List<String> updatedDockerfile = getUpdatedDockerfile();

        Assertions.assertTrue(updatedDockerfile.contains(ContainerizeDepsDockerfileHelper.HABUSHU_FINAL_STAGE + ContainerizeDepsDockerfileHelper.HABUSHU_COMMENT_START),
                "The Dockerfile is updated with `#HABUSHU_FINAL_STAGE - HABUSHU GENERATED CODE (DO NOT MODIFY)` comment ");
        Assertions.assertTrue(updatedDockerfile.contains(ContainerizeDepsDockerfileHelper.HABUSHU_FINAL_STAGE + ContainerizeDepsDockerfileHelper.HABUSHU_COMMENT_END),
                "The Dockerfile is updated with `#HABUSHU_FINAL_STAGE - HABUSHU GENERATED CODE (END)` comment ");
    }

    @Then("the original logic in the Dockerfile is preserved")
    public void original_logic_in_the_Dockerfile_is_preserved() throws IOException {
        List<String> updatedDockerfile = getUpdatedDockerfile();

        // Confirm the pre-existing logic is still present in the Dockerfile
        String dockerfileCommentLine = "# syntax=docker/dockerfile:1";
        String dockerfileFromLine = "FROM scratch";
        String dockerfileAddLine = "ADD hello /";
        String dockerfileCmdLine = "CMD [\"/hello\"]";

        Assertions.assertTrue(updatedDockerfile.contains(dockerfileCommentLine),
                "The Dockerfile still contains the comment line.");
        Assertions.assertTrue(updatedDockerfile.contains(dockerfileFromLine),
                "The Dockerfile still contains the original FROM line.");
        Assertions.assertTrue(updatedDockerfile.contains(dockerfileAddLine),
                "The Dockerfile still contains the original ADD line.");
        Assertions.assertTrue(updatedDockerfile.contains(dockerfileCmdLine),
                "The Dockerfile still contains the original CMD line.");
    }

    @Then("the Dockerfile uses the custom index to install wheels")
    public void theDockerfileUsesTheCustomIndexToInstallWheels() throws IOException {
        List<String> updatedDockerfile = getUpdatedDockerfile();
        String repoLine = null;
        for (String line : updatedDockerfile) {
            if(line.contains(CUSTOM_REPO)) {
                repoLine = line;
                break;
            }
        }
        Assertions.assertNotNull(repoLine, "Custom repository was not added to the Dockerfile");
        Assertions.assertTrue(repoLine.contains("--index-url"),
                "The Dockerfile does not use --index-url to set the custom repository.");
    }

    @Then("the Dockerfile adds the custom dev index during wheel installation")
    public void theDockerfileAddsTheCustomDevIndexDuringWheelInstallation() throws IOException {
        List<String> updatedDockerfile = getUpdatedDockerfile();
        String repoLine = null;
        for (String line : updatedDockerfile) {
            if(line.contains(CUSTOM_DEV_REPO)) {
                repoLine = line;
                break;
            }
        }
        Assertions.assertNotNull(repoLine, "Custom dev repository was not added to the Dockerfile");
        Assertions.assertTrue(repoLine.contains("--extra-index-url"),
                "The Dockerfile does not use --extra-index-url to set the dev repository.");
        Assertions.assertTrue(repoLine.contains("--pre"),
                "The Dockerfile does enable pre-release versions for the dev repository.");
    }

    @Given("a dockerfile already updated")
    public void a_dockerfile_already_updated() {
        setDockerfile("/src/main/resources/docker/UpdatedDockerfile");
    }

    @Given("a dockerfile without any habushu builder or final stage comment tag")
    public void a_dockerfile_without_comment_tag() {
        setDockerfile("/src/main/resources/docker/NoHabushuCommentDockerfile");
    }

    @Given("updateDockerfile set false")
    public void updateDockerfile_to_false() {
        mojo.setUpdateDockerfile(false);
    }

    private void setDockerfile(String dockerPath) {
        dockerfile = new File(mavenProjectPath + dockerPath);
        mojo.setDockerfile(dockerfile);
    }

    private void assertStaged(Path actual) {
        Set<Path> actualFiles;
        try {
            actualFiles = getRelativizedPaths(actual);
        } catch (Exception e) {
            throw new RuntimeException();
        }

        assertFile(actualFiles, LIB_WHEEL);
        assertFile(actualFiles, APP_WHEEL);
    }

    private static void assertFile(Set<Path> actualFiles, String path) {
        Assertions.assertTrue(actualFiles.contains(Paths.get(path)), "Could not find: " + path);
    }

    private Set<Path> getRelativizedPaths(Path rootDir) throws IOException {
        try (Stream<Path> paths = Files.walk(rootDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(rootDir::relativize)
                    .collect(Collectors.toSet());
        }
    }

    private List<String> getUpdatedDockerfile() throws IOException {
        return Files.readAllLines(dockerfile.toPath(), StandardCharsets.UTF_8);
    }

    private String getPackageManagerProjectPath(String packageManager){
        if (POETRY.equalsIgnoreCase(packageManager)) {
            return poetryMonorepoDepPath;
        } else {
            return uvMonorepoDepPath;
        }
    }

    private static class FileLoc {
        int ln = -1;
        int col = -1;

        boolean found() {
            return ln >= 0 && col >= 0;
        }

        boolean before(FileLoc other) {
            return before(other.ln, other.col);
        }

        boolean before(int line, int column) {
            return found() && (ln < line || (ln == line && col < column));
        }
    }
}
