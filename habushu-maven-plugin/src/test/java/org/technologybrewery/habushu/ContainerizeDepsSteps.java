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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainerizeDepsSteps {
    protected String targetDefaultSingleMonorepoDepPath = "target/test-classes/containerize-dependencies/"
            + "default-single-monorepo-dep";

    protected String poetryMonorepoDepPath = targetDefaultSingleMonorepoDepPath + "/poetry-monorepo";
    protected String uvMonorepoDepPath = targetDefaultSingleMonorepoDepPath + "/uv-monorepo";
    protected File dockerfile;
    protected String mavenProjectPath;
    private final String POM_FILE = "pom.xml";
    private final String POETRY = "poetry";

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

    private String getPackageManagerProjectPath(String packageManager){
        if (POETRY.equalsIgnoreCase(packageManager)) {
            return poetryMonorepoDepPath;
        } else {
            return uvMonorepoDepPath;
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

    @When("the containerize-dependencies goal is executed")
    public void the_containerize_dependencies_goal_is_executed() throws MojoExecutionException, MojoFailureException {
        mojo.execute();
    }

    @Then("the source files of the {string}-based dependency and transitive Habushu-type dependencies are staged for containerization")
    public void
    the_source_files_of_the_package_manager_based_dependency_and_transitive_habushu_type_dependencies_are_staged_for_containerization(String packageManager) {
        assertStaged(mojo.getStagingPath(), packageManager);
    }

    @Given("a dockerfile to update")
    public void a_dockerfile_to_update() {
        setDockerfile("/src/main/resources/docker/Dockerfile");
    }

    @Then("all the source files to build the {string}-based dependency are staged in the build directory")
    public void all_the_source_files_to_build_the_package_manager_based_dep_are_staged_in_the_build_directory(String packageManager) {
        assertStaged(mojo.getStagingPath(), packageManager);
    }

    @Then("the Dockerfile is updated to leverage a virtual environment for the dependency")
    public void dockerfile_is_updated_to_build_a_virtual_env_for_the_dependency() {
        String updatedDockerfile = getUpdatedDockerfile();

        // Confirm the Habushu logic is present in the Dockerfile
        Assertions.assertTrue(updatedDockerfile.contains(ContainerizeDepsDockerfileHelper.HABUSHU_BUILDER_STAGE + ContainerizeDepsDockerfileHelper.HABUSHU_COMMENT_START),
                "The Dockerfile is updated with `#HABUSHU_BUILDER_STAGE - HABUSHU GENERATED CODE (DO NOT MODIFY)` comment ");
        Assertions.assertTrue(updatedDockerfile.contains(ContainerizeDepsDockerfileHelper.HABUSHU_BUILDER_STAGE + ContainerizeDepsDockerfileHelper.HABUSHU_COMMENT_END),
                "The Dockerfile is updated with `#HABUSHU_BUILDER_STAGE - HABUSHU GENERATED CODE (END)` comment ");
        Assertions.assertTrue(updatedDockerfile.contains(ContainerizeDepsDockerfileHelper.HABUSHU_FINAL_STAGE + ContainerizeDepsDockerfileHelper.HABUSHU_COMMENT_START),
                "The Dockerfile is updated with `#HABUSHU_FINAL_STAGE - HABUSHU GENERATED CODE (DO NOT MODIFY)` comment ");
        Assertions.assertTrue(updatedDockerfile.contains(ContainerizeDepsDockerfileHelper.HABUSHU_FINAL_STAGE + ContainerizeDepsDockerfileHelper.HABUSHU_COMMENT_END),
                "The Dockerfile is updated with `#HABUSHU_FINAL_STAGE - HABUSHU GENERATED CODE (END)` comment ");

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

    private void assertStaged(Path actual, String packageManager) {
        Set<Path> actualFiles;
        try {
            actualFiles = getRelativizedPaths(actual);
        } catch (Exception e) {
            throw new RuntimeException();
        }

        assertFile(actualFiles, "extensions/extensions-python-dep-X/src/python_dep_x/python_dep_x.py");
        assertFile(actualFiles, "extensions/extensions-python-dep-X/pyproject.toml");
        assertFile(actualFiles, "extensions/extensions-python-dep-X/README.md");
        assertFile(actualFiles, "foundation/foundation-python-dep-Y/src/python_dep_y/python_dep_y.py");
        assertFile(actualFiles, "foundation/foundation-python-dep-Y/pyproject.toml");
        assertFile(actualFiles, "foundation/foundation-python-dep-Y/README.md");

        if (POETRY.equalsIgnoreCase(packageManager)){
            assertFile(actualFiles, "extensions/extensions-python-dep-X/poetry.toml");
            assertFile(actualFiles, "foundation/foundation-python-dep-Y/poetry.toml");
        }
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

    public String getUpdatedDockerfile() {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(dockerfile.getPath()), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

}
