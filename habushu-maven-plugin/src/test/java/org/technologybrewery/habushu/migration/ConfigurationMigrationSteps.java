package org.technologybrewery.habushu.migration;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class ConfigurationMigrationSteps {
    private String scenario;
    private Path testPom;
    private boolean shouldExecute;
    private boolean executionSucceeded;

    @Before("@config-migration")
    public void before(Scenario scenario) {
        this.scenario = scenario.getName().toLowerCase().replaceAll("\\s+", "-");
    }

    @Given("a POM with a habushu configuration of")
    public void aPomWithXmlPropertySet(String configurationXml) throws Exception {
        Xpp3Dom configuration = createConfigDomFromString(configurationXml);
        createTestPomWithConfiguration(configuration);
    }

    @When("the configuration migration runs")
    public void theMigrationRuns() {
        ContainerizeConfigMigration migration = new ContainerizeConfigMigration();
        shouldExecute = migration.shouldExecuteOnFile(testPom.toFile());
        executionSucceeded = shouldExecute && migration.performMigration(testPom.toFile());
    }

    @Then("the configuration migration succeeds")
    public void theConfigurationMigrationSucceeds() {
        Assert.assertTrue("Migration did not execute for test file: " + testPom, shouldExecute);
        Assert.assertTrue("Migration did not complete successfully: " + testPom, executionSucceeded);
    }

    @Then("the habushu configuration is updated to")
    public void theHabushuConfigurationIsUpdatedTo(String newConfigurationXml) throws Exception {
        Xpp3Dom expectedConfig = createConfigDomFromString(newConfigurationXml);
        validateConfig((message, config) -> Assert.assertEquals(message, expectedConfig, config));
    }

    @Then("the habushu configuration is removed from the plugin definition")
    public void theHabushuConfigurationIsRemovedFromThePluginDefinition() throws Exception {
        validateConfig(Assert::assertNull);
    }

    private void validateConfig(ConfigValidator validator) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model pom = reader.read(Files.newBufferedReader(testPom));

        Plugin habushuPlugin = pom.getBuild().getPluginManagement().getPlugins().get(0);
        var actualConfig = habushuPlugin.getConfiguration();
        validator.validate("Configuration not updated properly in pluginManagement of " + testPom, actualConfig);
        actualConfig = habushuPlugin.getExecutions().get(0).getConfiguration();
        validator.validate("Configuration not updated properly in pluginManagement execution of " + testPom, actualConfig);

        habushuPlugin = pom.getBuild().getPlugins().get(0);
        actualConfig = habushuPlugin.getConfiguration();
        validator.validate("Configuration not updated properly in plugins of " + testPom, actualConfig);
        actualConfig = habushuPlugin.getExecutions().get(0).getConfiguration();
        validator.validate("Configuration not updated properly in plugin execution of " + testPom, actualConfig);
    }

    private void createTestPomWithConfiguration(Xpp3Dom configuration) throws Exception {
        testPom = Path.of("target/configuration-migration/test-" + scenario + "/pom.xml");
        MavenProject testProject = createTestProject();
        Build build = testProject.getBuild();

        Plugin habushu = createHabushuPlugin(configuration);
        build.addPlugin(habushu);

        PluginManagement pluginManagement = new PluginManagement();
        pluginManagement.addPlugin(habushu);
        build.setPluginManagement(pluginManagement);

        MavenXpp3Writer writer = new MavenXpp3Writer();
        Files.createDirectories(testPom.getParent());
        BufferedWriter pomWriter = Files.newBufferedWriter(testPom, CREATE, TRUNCATE_EXISTING);
        writer.write(pomWriter, testProject.getModel());
    }

    private static Xpp3Dom createConfigDomFromString(String configurationXml) throws Exception {
        String rawConfig = "<configuration>" + configurationXml + "</configuration>";
        StringReader reader = new StringReader(rawConfig);
        return Xpp3DomBuilder.build(reader);
    }

    private static MavenProject createTestProject() {
        Model model = new Model();
        MavenProject project = new MavenProject(model);
        project.setPackaging("habushu");
        project.setArtifactId("test-artifact");
        project.setGroupId("test-group");
        project.setVersion("1.0.0");
        return project;
    }

    private static Plugin createHabushuPlugin(Xpp3Dom configuration) {
        Plugin habushuPlugin = new Plugin();
        habushuPlugin.setGroupId("org.technologybrewery.habushu");
        habushuPlugin.setArtifactId("habushu-maven-plugin");
        habushuPlugin = habushuPlugin.clone();// sets default values for executions, configuration, etc.
        habushuPlugin.setConfiguration(configuration);
        PluginExecution execution = new PluginExecution();
        execution.setId("default-exec");
        execution.setGoals(List.of("habushu-clean"));
        execution.setConfiguration(configuration);
        habushuPlugin.getExecutions().add(execution);
        return habushuPlugin;
    }

    @FunctionalInterface
    private interface ConfigValidator {
        void validate(String message, Object actualConfig);
    }
}