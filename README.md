# Habushu #
[![Maven Central](https://img.shields.io/maven-central/v/org.bitbucket.cpointe.habushu/root.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.bitbucket.cpointe.habushu%22%20AND%20a%3A%22habushu%22)
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit)

In Okinawa, habushu (pronounced HA-BU-SHU) is a sake that is made with venomous snake. The alcohol in the snake assists in dissolving the snake's venom and making it non-poinsonous. In Maven, habushu allows Conda-based pyhton projects to be included as part a Maven build. This brings some order and consistency to what can otherwise be haphazardly structured projects.

## Approach ##
Habushu is implemented as a series of [Maven](https://maven.apache.org/) plugins that tie together existing tooling in an opinionated fashion similar to how Maven structures Java projects.  By taking care of manual steps and bringing a predictable order of execution and core naming conventions, habushu increases time spent on impactful work rather than every developer or data scientist building out scripts that meet their personal preferences.  It's likely that no one person will agree with all the opinions habushu brings forth, but the value in being able to run entire builds from a single `mvn clean install` command regardless of your prior experience with the projects adds substantial value - both locally and in DevSecOps scenarios.

## Using habushu ##

### Adding to your Maven project
To be completed - [HAB-4 will add specific habushu lifecycle](https://fermenter.atlassian.net/browse/HAB-4), so documenting setup has been deferred to that ticket.  See the [habushu-mixology module](https://bitbucket.org/cpointe/habushu/src/dev/habushu-mixology/) in the meantime for the pattern.

(HAB-4 will expand on lifecycle steps here)

### Adding Tests ###
Habushu leverages [behave](https://behave.readthedocs.io/en/stable/index.html) to run Cucumber tests quiskly and easily.

#### Add Feature Files ####
You can add your [Gherkin feature files](https://cucumber.io/docs/gherkin/) to the `src/test/python/features` directory.  You can change the location of where test code is located via the `pythonTestDirectory` configuration value using [standard Maven configuration procedures](https://maven.apache.org/guides/mini/guide-configuring-plugins.html), though this is highely discouraged.

#### Run Build to Generate Step Stubs ####
As is customary with Cucumber testing, once you add your feature, stubbed methods can be created for each step by running the build via `mvn clean test`.  Any step that is missing will be listed at the end of the build, allowing you to copy that snippet and then add in the implementation.  For example:
```
ERROR conda.cli.main_run:execute(33): Subprocess for 'conda run ['behave', '/Users/sakelover/dev/habushu/habushu-mixology/src/test/python/features']' command failed.  (See above for error)

You can implement step definitions for undefined steps with these snippets:

@given(u'any SSL leveraged within the System')
def step_impl(context):
    raise NotImplementedError(u'STEP: Given any SSL leveraged within the System')
```

#### Author Step Implementations ####
Following the standard behave convention, add your Step classes to the `src/test/python/features/steps` folder.  The stubbed methods from the prior step will be matched at runtime.

#### Test Configuration Options ####
Several configuration options are described below.  Please feel free to request additional configuration options via a [JIRA ticket](https://fermenter.atlassian.net/browse/HAB).

Per standard Maven conventions, these confgurations can be set in your POM or via the command line.

*POM Example*
```
<plugin>
	<groupId>org.bitbucket.cpointe.habushu</groupId>
	<artifactId>habushu-maven-plugin</artifactId>
	<version>LATEST VERSION GOES HERE</version>
	<configuration>
		<cucumber.options>--tags @justThisTag</cucumber.options>
	</configuration>
</plugin>
```

*Command Line Example*

`mvn clean install -Dcucumber.options="--tags @justThisTag"`

##### Include @manual Tagged Features/Scenarios #####
By default, any feature or scenario tagged with `@manual` will be automatically skipped.  You can disable this behavior by setting the `excludeManualTag` confugration options. No need to specify at all unless you want to change the default.

##### Specify a Behave / Cucumber Command Line Option #####
Behave supports a [number of command line options](https://behave.readthedocs.io/en/stable/behave.html#command-line-arguments).  One or more can bee added via the `cucumber.options` configuration.  Often, this is used to specify a specific tag or tags you are actively iterating on to speed that process.

##### Skip Tests Entirely #####
This is almost always a bad idea, but occasionally is very useful.  The standard Maven `skipTests` configuration can be added (without specifying `=true`, though that works too).