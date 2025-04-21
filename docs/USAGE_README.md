[[Return to Main Documentation]](../README.md#usage)

# Usage

A Poetry or uv project using the `src/` packaging layout only needs an appropriately configured `pom.xml` within the root level of the project to be instrumented through Habushu and participate in a Maven build lifecycle.  The following depicts the required folder structure within an example Habushu module named `spam-ham-eggs`, including the placement of the required `pom.xml` and `pyproject.toml` configurations and utilization of [behave](https://behave.readthedocs.io/en/stable/index.html) for automated testing:

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

Best practices for creating a new project (possibly based on an existing Python package or older Habushu module) and adding needed Habushu plugin declaration to the module's `pom.xml` are described below. For working examples, see the [Examples](../examples/README.md) documentation.

## Integrating Your Poetry/uv Project with Habushu and Maven

Once you have a valid project, add the following configurations to your `pom.xml` to enable your project to be managed as a part of Habushu's custom Maven build lifecycle.

Set the `<packaging>` type of your module's `pom.xml` to `habushu`:
```xml
	<packaging>habushu</packaging>
```

Add the following plugin definition to your module's `pom.xml` `<build>` section:
```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <version>3.0.0</version>
    <extensions>true</extensions>
</plugin>
```

If publishing packages to or consuming dependencies from a private PyPI repository that requires authentication, add your repository credentials to your Maven's `settings.xml` file, usually located under your `~/.m2` folder. See the [Publishing Repository Configurations](CONFIGURATION_README.md#publishing-repository-configurations) documentation for details.

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

## Adding Tests

[Gherkin feature files](https://cucumber.io/docs/gherkin/) should be added to `tests/features` while Python step implementations should be added to `tests/features/steps`. As is customary with BDD, once you add your feature, stubbed methods can be created for each step by running the build via `mvn test`.  Any step that is missing will be listed at the end of the build, allowing you to copy that snippet and then add in the implementation.  For example:
```
[INFO] Failing scenarios:
[INFO]   tests/features/example.feature:30  This is an unimplemented test for testing purposes

You can implement step definitions for undefined steps with these snippets:

@given(u'any SSL leveraged within the System')
def step_impl(context):
    raise NotImplementedError(u'STEP: Given any SSL leveraged within the System')
```

## Running Specified Tagged Tests

"integration-test" is an example profile within [habushu-poetry-package](../examples/poetry/habushu-poetry-package/README.md) used to specify a tag(s) to test.  The variable "tags" is used to specify which tags to test. To run multiple tests, comma separate them. To exclude a test, add "\~" in front of the tag.

```
mvn clean test -Ptagged-tests -Dtags="one_tag"
```

## Leveraging Maven Build Cache for Faster Builds

Habushu enables support for faster builds via
the [Maven Build Cache](https://maven.apache.org/extensions/maven-build-cache-extension/) (only available in Maven
3.9+). This functionality is enabled through three mechanisms: Maven build directory customization, the Maven Reactor,
and Maven Build Cache Configuration. All require manual action to enable.

### Maven Build Directory Customization
Update Maven's build configuration to point to the `dist` directory for output in the pom file for each Habushu module:
```xml
<build>
    <directory>dist</directory>
    ...
</build>
```

### Maven Reactor Configuration

The [Reactor](https://maven.apache.org/guides/mini/guide-multiple-modules.html#the-reactor) allows Maven to understand
the relative structure and local hierarchy of Habushu/non-Habushu modules in your build. This can be accomplished by
adding a standard Maven dependency to the modules that should be tied together. For instance, the dependency below is
used in the [habushu-poetry-package-consumer](../examples/poetry/habushu-poetry-package-consumer) example. This package depends on the [habushu-poetry-package](../examples/poetry/habushu-poetry-package) example. By adding this configuration to the `pom.xml`, any time a change occurs in `habushu-poetry-package`, all dependent Habushu modules will be rebuilt automatically.

```xml
<!-- SNIPPET from habushu-poetry-package-consumer/pom.xml: -->
<dependency>
    <!-- Follow standard Maven GAV (GroupId-ArtifactId-Version) referencing: -->
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-poetry-package</artifactId>
    <version>${project.version}</version>
    <!-- type of habushu needed for habushu modules specifically: -->
    <type>habushu</type>
</dependency>
```

### Maven Build Cache Configuration

You also need to add the following to your Maven Build Cache `maven-build-cache-config.xml` file:

```xml

<cache>
    <configuration>
        ...
        <attachedOutputs>
            <dirNames>
                <!-- Critical to enable Maven to process the artifacts created by Habushu: -->
                <dirName>../dist</dirName>
            </dirNames>
        </attachedOutputs>
        ...
    </configuration>
    <input>
        <global>
            <!-- The following glob values should be ADDED to your existing values: -->
            <glob>
                {*.xml,*.proto,*.properties,*.py,*.txt,*.toml,.env*,*.feature}
            </glob>
            <includes>
                <include>src/</include>
                <include>pyproject.toml</include>
            </includes>
            <excludes>
                <exclude>poetry.lock</exclude>
            </excludes>
        </global>
    </input>
    ...
</cache>
```
