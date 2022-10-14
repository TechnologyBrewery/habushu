# Habushu #
[![Maven Central](https://img.shields.io/maven-central/v/org.bitbucket.cpointe.habushu/habushu.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.bitbucket.cpointe.habushu%22%20AND%20a%3A%22habushu%22)
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit)

In Okinawa, habushu (pronounced HA-BU-SHU) is a sake that is made with venomous snake. The alcohol in the snake assists in dissolving the snake's venom and making it non-poinsonous. **In Maven, Habushu allows virtual environment-based Python projects to be included as part a Maven build. This brings some order and consistency to what can otherwise be haphazardly structured projects.**

## Why Do You Need Habushu? ##

Habushu is implemented as a series of [Maven](https://maven.apache.org/) plugins that tie together existing tooling in an opinionated fashion similar to how Maven structures Java projects.  

**By taking care of manual steps and bringing a predictable order of execution and core naming conventions, Habushu increases time spent on impactful work rather than every developer or data scientist building out scripts that meet their personal preferences.**  

No one person will agree with all the opinions implemented by Habushu. The value in being able to run entire builds from a single `mvn clean install` command regardless of your prior experience with the projects adds substantial value - both locally and in DevSecOps scenarios.

## Requirements ##

In order to use Habushu, the following prerequisites must be installed:

* Maven 3.6+
* Java 11+
* [Poetry 1.2+](https://python-poetry.org/)
* [Pyenv](https://github.com/pyenv/pyenv)

Additionally, Habushu may install and manage:

* [poetry-lock-groups-plugin](https://pypi.org/project/poetry-lock-groups-plugin/)

## Usage ##

Habushu automates a consistent and predictable build lifecycle by delegating *nearly all* commands related to dependency management, virtual environment activation, and package publishing to [Poetry](https://python-poetry.org/).  As a result, Habushu projects are Poetry projects and are expected to align with the conventions, structure, and configurations utilized by Poetry projects with the `src/` packaging layout.  

A Poetry project using the `src/` packaging layout only needs an appropriately configured `pom.xml` within the root level of the project to instrumented through Habushu and participate in a Maven build lifecycle.  The following depicts the required folder structure within an example Habushu module named `spam-ham-eggs`, including the placement of the required `pom.xml` and `pyproject.toml` configurations and utilization of [behave](https://behave.readthedocs.io/en/stable/index.html) for automated testing:

```
	spam-ham-eggs
	├── pyproject.toml
	├── pom.xml
	├── src
	│   └── spam_ham_eggs
	│       └── __init__.py
	└── tests
	    └── features
	        ├── spam_ham_eggs.feature
    	    └── steps
	            └── spam_ham_eggs_step.py
```

Best practices for creating a new Poetry project (possibly based on an existing Python package or older Habushu module) and adding needed Habushu plugin declaration to the module's `pom.xml` are described below.

### Creating a New Poetry Project ###
If starting from scratch, use the `poetry new --src` command to create a new Poetry project:

```sh
$ poetry new spam-ham-eggs --src
Created package spam_ham_eggs in spam-ham-eggs
$ ls spam-ham-eggs 
README.rst     pyproject.toml src            tests
```

If migrating an existing Python package, consider using `poetry init` and use the interactive guide to create the desired `pyproject.toml` configuration with the appropriate dependencies.  

If migrating an earlier release of Habushu, follow the same process, but note the following required changes:

* Dependencies specified in `requirements.txt` must be specified in `pyproject.toml` - either use `poetry add` or add them interactively via `poetry init`
* Python source and test files must be migrated into the folder structure described above, which aligns with the standard `src/` packaging layout.  Assuming that the package name is `spam_ham_eggs`, `src/main/python/*` from the existing Habushu project must be moved into `src/spam_ham_eggs` and `src/test/python/*` from the existing Habushu project must be moved into `tests`
* Previously, Habushu 1.x modules depended on each other via Maven `<dependency>` declarations.  This approach is deprecated as Habushu 2.x+ expects that other Habushu modules are published to PyPI repositories and consumed as Python packages using Poetry's built-in dependency management capabilties.  For Habushu module dependencies within the same Maven multi-module build hierarchy, consider using editable development installs:
  
    ```toml
    # pyproject.toml
    [tool.poetry.dependencies]
    my-package = {path = "../spam-eggs-ham-dependency", develop = true}
    ```

### Integrating Your Poetry Project with Habushu and Maven ###

Once you have a valid Poetry project, add the following configurations to your `pom.xml` to enable your Poetry project to be managed as a part of Habushu's custom Maven build lifecycle.

Set the `<packaging>` type of your module's `pom.xml` to `habushu`:
```xml
	<packaging>habushu</packaging>
```

Add the following plugin definition to your module's `pom.xml` `<build>` section:
```xml
	<plugin>
		<groupId>org.bitbucket.cpointe.habushu</groupId>
		<artifactId>habushu-maven-plugin</artifactId>
		<version>2.0.0</version>
		<extensions>true</extensions>
	</plugin>
```

If publishing packages to or consuming dependencies from a private PyPI repository that requires authentication, add your repository credentials to your Maven's `settings.xml` file, usually located under your `~/.m2` folder. See the Configuration section below on how to 

```xml
    <server>
        <!-- ID of the PyPI repository - this ID will be used to reference this repository in
             the habushu-maven-plugin configuration -->
        <id>private-pypi-repo</id>
        
        <!-- Username of the account by which to access the PyPI repository -->
        <username>pypi-repo-username</username>
        
        <!-- Password of account by which to access the PyPI repository; should be encrypted as per Maven best practices  -->
        <password>{encrypted-pypi-repo-password}</password>
    </server>
```

### Adding Tests ###

Habushu leverages [behave](https://behave.readthedocs.io/en/stable/index.html) to institute behavior-driven development (BDD) and quickly execute tests that implement [Gerkin features](https://cucumber.io/docs/gherkin/).

[Gherkin feature files](https://cucumber.io/docs/gherkin/) should be added to `tests/features` while Python step implementations should be added to `tests/features/steps`. As is customary with BDD, once you add your feature, stubbed methods can be created for each step by running the build via `mvn test`.  Any step that is missing will be listed at the end of the build, allowing you to copy that snippet and then add in the implementation.  For example:
```
[INFO] Failing scenarios:
[INFO]   tests/features/example.feature:30  This is an unimplemented test for testing purposes

You can implement step definitions for undefined steps with these snippets:

@given(u'any SSL leveraged within the System')
def step_impl(context):
    raise NotImplementedError(u'STEP: Given any SSL leveraged within the System')
```

### Running Specified Tagged Tests ###

"tagged-tests" is an example profile within habushu-mixology-consumer used to specify a tag(s) to test.  The variable "tags" is used to specify which tags to test. To run multiple tests, comma separate them. To exclude a test, add "\~" in front of the tag. 

Ex: mvn clean test -Ptagged-tests -Dtags="one_tag"

### Running Custom Python Scripts During Build Phases ###

In addition to creating a custom Maven lifecycle that automates the execution of a predictable Poetry-based workflow, Habushu exposes a `run-command-in-virtual-env` plugin goal that provides developers with the ability to [execute any Python command or script](https://python-poetry.org/docs/cli/#run) within the Poetry project's virtual environment through `poetry run` during the desired build phase. 

For example, developers may use this feature to bind a Habushu module's `compile` phase to the appropriate Python command that generates gRPC/protobuf bindings as an automated part of the build following dependency installation:

```xml
	<plugin>
		<groupId>org.bitbucket.cpointe.habushu</groupId>
		<artifactId>habushu-maven-plugin</artifactId>
		<extensions>true</extensions>
		<configuration>
		...
		</configuration>
		<executions>
			<execution>
				<configuration>
					<runCommandArgs>python -m grpc_tools.protoc -I=src
									--python_out=src/habushu_mixology/generated src/person.proto</runCommandArgs>
				</configuration>
				<id>generate-protobuf-bindings</id>
				<phase>compile</phase>
				<goals>
					<goal>run-command-in-virtual-env</goal>
				</goals>
			</execution>
		</executions>
	</plugin>
```

## Configuration ##

All Habushu configurations may be set either via the `habushu-maven-plugin`'s `<configuration>` definition, Maven POM properties, or `-D` on the line and follow a consistent naming pattern for the different configuration approaches.  For setting configurations via POM properties or `-D` on the command line, all configuration keys may be prepended with `habushu.`.  For example, `pythonVersion` controls the version of Python utilized by Habushu and may be configured using the following approaches:

1. Plugin `<configuration>`

```xml
	<plugin>
		<groupId>org.bitbucket.cpointe.habushu</groupId>
		<artifactId>habushu-maven-plugin</artifactId>
		<extensions>true</extensions>
		<configuration>
			<pythonVersion>3.10.4</pythonVersion>
		</configuration>
	</plugin>
```

2. `-D` via command line

```shell
mvn clean install -Dhabushu.pythonVersion=3.10.4
```

3. POM properties

```xml
	<properties>
		<habushu.pythonVersion>3.10.4</habushu.pythonVersion>
	</properties>
```

**NOTE:** The above list's order reflects the precedence in which configurations will be applied.  For example, configuration values that are specified in the plugin's `<configuration>` definition will always take precedence, while system properties via the command line (`-D`) will take precedence over `<properties>` definitions.

#### pythonVersion ####

The desired version of Python to use. Habushu delegates to `pyenv` for managing versions of Python depending on the configuration `usePyenv`.

Default: `3.9.13`

#### usePyenv ####

If true then Habushu will delegate to `pyenv` for managing and (if needed) installing the specified version of Python. If false then it will look for the available Python installation on the PATH. If Python is not found or if the version does not match the configured `pythonVersion`, the build will fail.

Default: `true`

#### behaveOptions ####

Options that should be passed to the `behave` command when executing tests. If this value is provided, then **behaveExcludeManualTag** is ignored. 

`behave` supports a [number of command line options](https://behave.readthedocs.io/en/stable/behave.html#command-line-arguments) - developers may adjust the default test execution behavior to optimize productivity, such as selectively executing features associated with a specific in-flight tag (`mvn clean test -Dhabushu.behaveOptions="--tags wip-feature"`) or changing logging behavior (`mvn clean test -Dhabushu.behaveOptions="--no-logcapture --no-capture"`).

Default: None

#### behaveExcludeManualTag ####

Exclude any BDD scenario or feature file tagged with `@manual`.  

**NOTE:** If **behaveOptions** are provided, this property is ignored.

Default: `true`

#### rewriteLocalPathDepsInArchives ####

Enables the use of the [poetry-monorepo-dependency-plugin](https://pypi.org/project/poetry-monorepo-dependency-plugin/) to rewrite 
any local path dependencies (to other Poetry projects) as versioned packaged dependencies in generated `wheel`/`sdist` archives. If `true`,
Habushu will replace invocations of Poetry's `build` and `publish` commands with the extensions of those commands exposed by the
`poetry-monorepo-dependency-plugin`, which are `build-rewrite-path-deps` and `publish-rewrite-path-deps`, respectively.

Typically, this flag will only be `true` when deploying/releasing Habushu modules within a CI environment that are part of a monorepo project
structure which multiple Poetry projects depend on one another.
    
Default: `false`

#### pypiRepoId ####

Specifies the `<id>` of the `<server>` element declared within the utilized Maven `settings.xml` configuration that represents the PyPI repository
to which this project's archives will be published and/or used as a secondary repository from which dependencies may be installed. This property is **REQUIRED** if publishing to or consuming dependencies from a private PyPI repository that requires authentication - it is expected that the relevant `<server>` element provides the needed authentication details.

If this property is **not** specified, this property will default to `pypi` and the execution of the `deploy` lifecycle phase will publish this package to the official public PyPI repository.  Downstream package publishing functionality will use the relevant `settings.xml` `<server>` declaration with `<id>pypi</id>` as credentials for publishing the package to PyPI. If developers want to use PyPI's [API tokens](https://pypi.org/help/#apitoken) instead of username/password credentials, they may do so by manually executing the appropriate Poetry command (`poetry config pypi-token.pypi my-token`) in an ad-hoc fashion prior to running `deploy`.

This property will typically be specified as a command line option during the `deploy` lifecycle phase.  For example, given the following configuration in the utilized `settings.xml`:

```xml
    <server>
        <id>private-pypi-repo</id>
        <username>pypi-repo-username</username>
        <password>{encrypted-pypi-repo-password}</password>
    </server> 
```

The following command may be utilized to publish the package to the specified private PyPI repository at `https://private-pypi-repo-url/repository/pypi-repo/`:

```sh 
$ mvn deploy -Dhabushu.pypiRepoId=private-pypi-repo -Dhabushu.pypiRepoUrl=https://private-pypi-repo-url/repository/pypi-repo/
```
Default: `pypi`

#### pypiRepoUrl ####

Specifies the URL of the private PyPI repository to which this project's archives will be published and/or used as a secondary repository from which dependencies may be installed. This property is **REQUIRED** if publishing to or consuming dependencies from a private PyPI repository.  

If the Habushu project depends on internal packages that may only be found on a private PyPI repository, developers should specify this property through the plugin's `<configuration>` definition:

```xml
	<plugin>
		<groupId>org.bitbucket.cpointe.habushu</groupId>
		<artifactId>habushu-maven-plugin</artifactId>
		<extensions>true</extensions>
		<configuration>
			<pypiRepoUrl>https://private-pypi-repo-url/repository/pypi-repo</pypiRepoUrl>
		</configuration>
	</plugin>
```

Default: None

#### pypiSimpleSuffix ####

Specifies the path to the simple index relative to the pypiRepoUrl.  Certain private repository solutions use non-standard paths (ie: devpi uses `+simple`).

Default: `simple`

#### decryptPassword ####

Specifies whether Habushu should attempt to decrypt the remote server password provided in Maven's `settings.xml file`.  If `false`, the password will be retrieved as-is and assumed to be unencrypted.

Warning: Storage of plain-text passwords is a security risk!  This functionality is best used when the password is stored in a safe manner outside of Maven's native credential system, and is decrypted prior to execution (Jenkins credentials, for instance).

Default: `true`

#### withGroups ####

Specifies which Poetry dependency groups to include within the installation.  Example usage:
```shell
mvn clean install -Dhabushu.withGroups=dev,test
```
or
```xml
<plugin>
    <groupId>org.bitbucket.cpointe.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        <withGroups>
            <withGroup>dev</withGroup>
            <withGroup>test</withGroup>
        </withGroups>
    </configuration>
</plugin>
```

Default: None

#### withoutGroups ####
Specifies Poetry dependency groups to exclude within the installation.  Example usage:

```shell
mvn clean install -Dhabushu.withoutGroups=dev,test
```
or
```xml
<plugin>
    <groupId>org.bitbucket.cpointe.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        <withoutGroups>
            <withoutGroup>dev</withoutGroup>
            <withoutGroup>test</withoutGroup>
        </withoutGroups>
    </configuration>
</plugin>
```

Default: None

#### forceSync ####

A value of `true` will result in Poetry installing packages with the --sync parameter.

Default: `false`

#### runCommandArgs ####

**Only applicable when executing the `run-command-in-virtual-env` plugin goal**

Whitespace-delimited command arguments that will be provided to `poetry run` to execute. For example, the following property configuration will execute `poetry run python -V` within the project's virtual environment during the `validate` phase of the build:

```xml
	<plugin>
		<groupId>org.bitbucket.cpointe.habushu</groupId>
		<artifactId>habushu-maven-plugin</artifactId>
		<extensions>true</extensions>
		<configuration>
		...
		</configuration>
		<executions>
			<execution>
				<configuration>
					<runCommandArgs>python -V</runCommandArgs>
				</configuration>
				<id>get-python-version</id>
				<phase>validate</phase>
				<goals>
					<goal>run-command-in-virtual-env</goal>
				</goals>
			</execution>
		</executions>
	</plugin>
```

Default: None

#### skipPoetryLockUpdate ####

Enables skipping the update of the poetry lock file on build. Note if the lock file does not exist then the install will recreate it regardless of this configuration. If the lock file has a mismatch with the toml definition then the build will fail. 

Default: `false`

#### deleteVirtualEnv ####

Enables the explicit deletion of the virtual environment that is created/managed by Poetry.

Example usage: `mvn clean -Dhabushu.deleteVirtualEnv`

Default: `false`
    
#### skipTests ####

Skips running tests.  Using this property is **NOT RECOMMENDED** but may be convenient on occasion.

Example usage: `mvn clean install -Dhabushu.skipTests=true`

Default: `false`

#### addPypiRepoAsPackageSources ####

Configures whether a private PyPi repository, if specified via **pypiRepoUrl**, is automatically added as a package source from which dependencies may be installed. This value is **only** utilized if a private PyPi repository is specified via **pypiRepoUrl**.  Developers will typically not need to configure this property, but is made available to support the manual configuration of a custom repository in `pyproject.toml` if needed.

Default: `true`

#### exportRequirementsFile ####

Enables or disables the generation of a requirements.txt file during the package phase.

Default: `true`

#### exportRequirementsFolder ####

Specifies where the requirements.txt file will be generated to during the package phase.

Default: `project-directory/dist` to be with the generated wheels.

#### exportRequirementsWithUrls ####

Whether or not the requirements.txt file should include source repository urls.

Default: `false` so will not add the --without-urls flag and thus include urls

#### exportRequirementsWithHashes ####

Whether or not the requirements.txt file should include package hashes.

Default: `false` so will not add the --without-urls flag and thus export with hashes

#### skipDeploy ####

Skips the execution of the `deploy` phase and does *not* publish the Poetry package to the configured PyPI repository. This configuration may be useful when individual Habushu modules within a larger multi-module project hierarchy should *not* be published to PyPI.

Default: `false`

#### snapshotNumberDateFormatPattern ####
    
[DateTimeFormatter](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html) compliant pattern that configures the numeric portion of `SNAPSHOT` Poetry package versions that are published to the configured PyPI repository. By default, the version of `SNAPSHOT` published packages align with [PEP-440 developmental releases](https://peps.python.org/pep-0440/#developmental-releases) and use a numeric component that corresponds to the number of seconds since the epoch. For example, if the POM version is `1.2.3-SNAPSHOT`, the package may be published by default as `1.2.3.dev1658238063`. If this property is specified, the numeric component will reflect the given date format pattern applied to the current build time. For example, if `YYYYMMddHHmm` is provided, `1.2.3.dev202207191002` be published.

Default: Number of seconds since epoch    

#### overridePackageVersion ####

Specifies whether the version of the encapsulated Poetry package should be automatically managed and overridden where necessary by Habushu. If this property is `true`, Habushu may override the `pyproject.toml` defined version in the following build phases/mojos:

* `validate`: Automatically sets the Poetry package version to the version specified in the POM. If the POM is a `SNAPSHOT`, the Poetry package version will be set to the corresponding developmental release version without a numeric component (i.e. POM version of `1.2.3-SNAPSHOT` will result in the Poetry package version being set to `1.2.3.dev`)
* `deploy`: Automatically sets the version of published Poetry packages that are `SNAPSHOT` modules to timestamped developmental release versions (i.e. POM version of `1.2.3-SNAPSHOT` will result in the published Poetry package version to to `1.2.3.dev1658238063`). After the package is published, the version of the `SNAPSHOT` module is reverted to its previous value (i.e. `1.2.3.dev`)

If this property is set to `false`, none of the above automated version management operations will be performed.

Default: `true`

#### sourceDirectory ####

Folder in which Python source files are located - should align with Poetry's project structure conventions. Developers will typically **not** modify this property but is made available for customization to support unanticipated scenarios.

Default: `${project.basedir}/src`

#### testDirectory ####

Folder in which Python test files are located - should align with Poetry's project structure conventions. Developers will typically **not** modify this property but is made available for customization to support unanticipated scenarios.

Default: `${project.basedir}/tests`

## The Habushu Build Lifecycle ##

Habushu applies a [custom Maven lifecycle that binds Poetry-based DevSecOps workflow commands](https://fermenter.atlassian.net/wiki/spaces/HAB/pages/2056749057/Dependency+Management+and+Build+Automation+through+Poetry+and+Maven) to the following phases:

##### validate #####

Ensures that Pyenv and Poetry are installed.

##### initialize #####

Utilizes Pyenv to initialize and configure the usage of the specified version of Python for the execution of all downstream Python/Poetry operations. If configured via **overridePackageVersion**, automatically syncs the Poetry project version with the appropriate version that is derived from the Habushu module's POM. 

##### compile #####

Installs dependencies defined in the project's `pyproject.toml` configuration, specifically by running `poetry lock` followed by `poetry install`. If a private PyPi repository is defined via **pypiRepoUrl**, it will be automatically added to the module's `pyproject.toml` configuration as a secondary source of dependencies, if it is not already configured in the `pyproject.toml`

##### process-classes #####

Leverages the [black formatter](https://github.com/psf/black) package to format both source and test Python directories via `poetry run`.
 
##### test #####

Uses [behave](https://github.com/behave/behave) to execute BDD scenarios that are defined in `tests/features`. By default, as per **behaveExcludeManualTag**, features/scenarios tagged with `@manual` are skipped.

##### package #####

Builds the `sdist` and `wheel` archives of this project using `poetry build`. It also generates a requirements.txt file which is useful when installing the package in a docker container where you may want to install the dependencies in a specific docker layer to optimize caching.

##### deploy #####

Publishes the generated package archives to the specified PyPI repository (defined via **pypiRepoId** and **pypiRepoUrl**).  If the current Habushu module is a `SNAPSHOT` version, temporarily set the version of the package to the appropriate developmental version, publish it to the specified PyPI repository, and then reset the version to the original value.

##### clean #####

Deletes the folder in which archives generated by the **package** phase are placed (`dist`) and if **deleteVirtualEnv** is set to `true`, deletes the project's virtual environment.

## Common Issues ##

### Pyenv/Poetry Not Installed

Pyenv is utilized to install and use the specified version of Python, while Poetry (which uses the Pyenv-managed version of Python) is utilized for all underlying build commands.  Both Pyenv and Poetry **MUST** be installed. If you encounter an error indicating that either tool is not installed or on the `PATH`, you will be prompted with installation guidance.

### Plugin Does Not Yet Exist ###

If you encounter the following error, please see the "Building Habushu" section for details on how to use the `bootstrap` profile to appropriately build the `habushu-maven-plugin` and its associated custom Maven lifecycle. This error will typically only occur when attempting to use an in-flight `SNAPSHOT` version of Habushu that is not yet published to the Maven Central repository.

```
[WARNING] The POM for org.bitbucket.cpointe.habushu:habushu-maven-plugin:jar:0.0.1-SNAPSHOT is missing, no dependency information available
[ERROR] [ERROR] Some problems were encountered while processing the POMs:
[ERROR] Unresolveable build extension: Plugin org.bitbucket.cpointe.habushu:habushu-maven-plugin:0.0.1-SNAPSHOT or one of its dependencies could not be resolved: Could not find artifact org.bitbucket.cpointe.habushu:habushu-maven-plugin:jar:0.0.1-SNAPSHOT @ 
[ERROR] Unknown packaging: habushu @ line 15, column 13
```

## Building Habushu ##

If you are working on Habushu, please be aware of some nuances in working with a plugin that defines a custom Maven build lifecycle and packaging. `habushu-mixology` and `habushu-mixology-consumer` are utilized to immediately test the `habushu-maven-plugin` and associated `habushu` lifecycle.  If the `habushu-maven-plugin` has not been previously built or there are unbuilt changes to the `habusu` lifecycle, developers must manually build the `habushu-maven-plugin` and then execute **another, separate** build of `habushu-mixology` (and any other `habushu` module) to use the updated `habushu-maven-plugin` and `habushu` lifecycle.  Developers are **not** able to build updates to the `habushu` lifecycle and test their application in `habushu-mixology` within the same build.  That said, if developers do not update the `habushu` lifecycle and simply make updates to existing `Mojo`s defined in the `habushu-maven-plugin`, a single build may be used to build `habushu-maven-plugin` and apply the updates to `habushu-mixology`. To assist, there are two profiles available in the build:

* `mvn clean install -Pbootstrap`: Builds the `habushu-maven-plugin` such that the custom `habushu` lifecycle may be utilized within subsequent builds.
  * **NOTE:** If updates are made to the `habushu` lifecycle (i.e. updates to the `habushu` lifecycle mapping configuration made in `habushu-maven-plugin/src/main/resources/META-INF/plexus/components.xml`), developers **MUST**  changes require two builds to test - one to build the lifecycle, then a second to use that updated lifecycle.  Code changes to `Mojo` classes within the existing `habushu` lifecycle work via normal builds without the need for a second pass.
* `mvn clean install -Pdefault`: (ACTIVE BY DEFAULT - `-Pdefault` does not need to be specified) builds all modules.  Developers may use this profile to build and apply changes to existing `habushu-maven-plugin` `Mojo` classes

## Release Notes ##

[Release notes can be found of the Habushu wiki](https://fermenter.atlassian.net/wiki/spaces/HAB/pages/1995505666/Release+Notes).

