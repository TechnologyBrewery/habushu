@config-migration
Feature: Automatically Migrate upgrading users to new configurations

  Scenario: A fully customized configuration is migrated
    Given a POM with a habushu configuration of
        """
        <pythonVersion>3.14.1</pythonVersion>
        <dockerPoetryVersion>1.5.0</dockerPoetryVersion>
        <dockerPoetryPluginBundleVersion>1.2.1</dockerPoetryPluginBundleVersion>
        <dockerUvVersion>0.9</dockerUvVersion>
        <dockerPoetryBuilderStageTemplatePath>a/template/path</dockerPoetryBuilderStageTemplatePath>
        <dockerPoetryFinalStageTemplatePath>some/other/path</dockerPoetryFinalStageTemplatePath>
        <defaultSourceSet>
          <fileset>
            <directory>my_module</directory>
            <includes>
              <include>**/*.py</include>
            </includes>
          </fileset>
        </defaultSourceSet>
        """
    When the configuration migration runs
    Then the configuration migration succeeds
    And the habushu configuration is updated to
        """
        <pythonVersion>3.14.1</pythonVersion>
        <dockerBuilderStageTemplate>a/template/path</dockerBuilderStageTemplate>
        <dockerFinalStageTemplate>some/other/path</dockerFinalStageTemplate>
        """

  Scenario: A UV template configuration is migrated
    Given a POM with a habushu configuration of
        """
        <dockerUvBuilderStageTemplatePath>another/template</dockerUvBuilderStageTemplatePath>
        <dockerUvFinalStageTemplatePath>final/template</dockerUvFinalStageTemplatePath>
        """
    When the configuration migration runs
    Then the configuration migration succeeds
    And the habushu configuration is updated to
        """
        <dockerBuilderStageTemplate>another/template</dockerBuilderStageTemplate>
        <dockerFinalStageTemplate>final/template</dockerFinalStageTemplate>
        """

  Scenario: A configuration with only deleted properties is removed
    Given a POM with a habushu configuration of
        """
        <dockerPoetryVersion>1.5.0</dockerPoetryVersion>
        <dockerPoetryPluginBundleVersion>1.2.1</dockerPoetryPluginBundleVersion>
        """
    When the configuration migration runs
    Then the configuration migration succeeds
    And the habushu configuration is removed from the plugin definition

 Scenario: A configuration with skipPoetryLockUpdate is renamed
   Given a POM with a habushu configuration of
        """
        <skipPoetryLockUpdate>true</skipPoetryLockUpdate>
        """
   When the configuration migration runs
   Then the configuration migration succeeds
   And the habushu configuration is updated to
        """
        <skipLockUpdate>true</skipLockUpdate>
        """
