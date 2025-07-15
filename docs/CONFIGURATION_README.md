[[Return to Main Documentation]](../README.md#configuration)

# Configuration 

All Habushu configurations may be set either via the `habushu-maven-plugin`'s `<configuration>` definition, Maven POM properties, or `-D` on the line and follow a consistent naming pattern for the different configuration approaches.  For setting configurations via POM properties or `-D` on the command line, all configuration keys may be prepended with `habushu.`.  For example, `pythonVersion` controls the version of Python utilized by Habushu and may be configured using the following approaches:

1. Plugin `<configuration>`

```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        <pythonVersion>3.12.9</pythonVersion>
    </configuration>
</plugin>
```

2. `-D` via command line

```shell
mvn clean install -Dhabushu.pythonVersion=3.12.9
```

3. POM properties

```xml
<properties>
    <habushu.pythonVersion>3.12.9</habushu.pythonVersion>
</properties>
```

**NOTE:** The above list's order reflects the precedence in which configurations will be applied.  For example, configuration values that are specified in the plugin's `<configuration>` definition will always take precedence, while system properties via the command line (`-D`) will take precedence over `<properties>` definitions.

## Configuration Options
- [General Habushu Configurations](#general-habushu-configurations)
  - [pythonVersion](#pythonversion)
  - [defaultPythonStrategy](#defaultpythonstrategy)
  - [rewriteLocalPathDepsInArchives](#rewritelocalpathdepsinarchives)
  - [forceSync](#forcesync)
  - [skipLockUpdate](#skiplockupdate)
  - [deleteVirtualEnv](#deletevirtualenv)
  - [skipDeploy](#skipdeploy)
  - [snapshotNumberDateFormatPattern](#snapshotnumberdateformatpattern)
  - [overridePackageVersion](#overridepackageversion)
  - [sourceDirectory](#sourcedirectory)
  - [testDirectory](#testdirectory)
  - [testPackage](#testpackage)
  - [mavenArtifactFile](#mavenartifactfile)
  - [workingDirectory](#workingdirectory)
  - [distDirectory](#distdirectory)
  - [targetDirectory](#targetdirectory)
  - [lint](#lint)
  - [useFormatter](#useformatter)
  - [runCommandArgs](#runcommandargs)
- [Dependency Group Configurations](#dependency-group-configurations)
  - [withGroups](#withgroups)
  - [withoutGroups](#withoutgroups)
- [Managed Dependencies Configurations](#managed-dependencies-configurations)
  - [managedDependencies](#manageddependencies)
  - [updateManagedDependenciesWhenFound](#updatemanageddependencieswhenfound)
  - [failOnManagedDependenciesMismatches](#failonmanageddependenciesmismatches)
- [Behave Configurations](#behave-configurations)
  - [behaveOptions](#behaveoptions)
  - [disableOutputCapture](#disableoutputcapture)
  - [behaveExcludeManualTag](#behaveexcludemanualtag) 
  - [behaveTestEnvironmentVariables](#behavetestenvironmentvariables)
  - [skipTests](#skiptests)
  - [outputCucumberStyleTestReports](#outputcucumberstyletestreports)
  - [omitSkippedTests](#omitskippedtests)
- [Pytest Configurations](#pytest-configurations)
  - [pytestOptions](#pytestoptions)
  - [enableVerbose](#enableverbose)
  - [pytestTestEnvironmentVariables](#pytesttestenvironmentvariables)
  - [skipTests](#skiptests)
- [Requirements.Txt Configurations](#requirementstxt-configurations)
  - [exportRequirementsFile](#exportrequirementsfile)
  - [exportRequirementsFolder](#exportrequirementsfolder)
  - [exportRequirementsWithUrls](#exportrequirementswithurls)
  - [exportRequirementsWithHashes](#exportrequirementswithhashes)
  - [exportRequirementsWithoutPathDependencies](#exportrequirementswithoutpathdependencies)
- [Repository Configurations](#repository-configurations)
  - [decryptPassword](#decryptpassword)
  - [pypiPushRetries](#pypipushretries)
  - [Development Repository Configurations](#development-repository-configurations)
    - [useDevRepository](#usedevrepository)
    - [devRepositoryId](#devrepositoryid)
    - [devRepositoryUrl](#devrepositoryurl)
    - [enableDevRepositoryUrlUploadSuffix](#enabledevrepositoryurluploadsuffix)
    - [devRepositoryUrlUploadSuffix](#devrepositoryurluploadsuffix)
  - [Publishing Repository Configurations](#publishing-repository-configurations)
    - [pypiRepoId](#pypirepoid)
    - [pypiRepoUrl](#pypirepourl)
    - [enablePypiSimpleSuffix](#enablepypisimplesuffix)
    - [pypiSimpleSuffix](#pypisimplesuffix)
    - [pypiUploadSuffix](#pypiuploadsuffix)
    - [addPypiRepoAsPackageSources](#addpypirepoaspackagesources)
- [Containerization Configurations](#containerization-configurations)
  - [stagingDirectory](#stagingdirectory)
  - [updateDockerfile](#updatedockerfile)
  - [dockerfile](#dockerfile)
  - [dockerBuilderBase](#dockerbuilderbase)
  - [dockerFinalBase](#dockerfinalbase)
  - [dockerUser](#dockeruser)
  - [dockerVenvDirectoryPermissions](#dockerVenvDirectoryPermissions)
  - [dockerContext](#dockercontext)
  - [dockerTemplatePath](#dockertemplatepath)
  - [dockerBuilderStageTemplate](#dockerbuilderstagetemplate)
  - [dockerFinalStageTemplate](#dockerfinalstagetemplate)
- [Version Enforcement Configurations](#version-enforcement-configurations)
  - [version](#version)
  - [Poetry-Specific Version Enforcement Configurations](#poetry-specific-version-enforcement-configurations) 
    - [requirePoetryVersion](#requirePoetryVersion)
    - [requirePyenvVersion](#requirePyenvVersion)
  - [uv-Specific Version Enforcement Configurations](#uv-specific-version-enforcement-configurations)
    - [requireUvVersion](#requireUvVersion)
- [Poetry-Specific Configurations](#poetry-specific-configurations)
  - [usePyenv](#usepyenv)
  - [useInProjectVirtualEnvironment](#useinprojectvirtualenvironment)
  - [poetryMonorepoDependencyPluginVersion](#poetrymonorepodependencypluginversion)

## General Habushu Configurations

### pythonVersion

The desired version of Python to use. If `pythonVersion` is not set then Habushu will use the `defaultPythonStrategy`
configuration to determine where to get the default Python version.

Default: `3.12.9`

### defaultPythonStrategy

If set to `PYTHONVERSION`, then Habushu will use the Python version supplied in the `.python-version` file located in the project directory. If set to `POM`, then Habushu will use the default value for `pythonVersion`.

**Note:** If set to `PYTHONVERSION` and no `.python-version` file is found in the project directory the build will fail with an error. 

Default: `POM`

**Example:** [Default Python Strategy](../examples/habushu-default-python-strategy/README.md)

### rewriteLocalPathDepsInArchives

Rewrites any local path dependencies (to other projects with a `[project.version]` within the `pyproject.toml`) as versioned packaged dependencies in generated `wheel` archives.
For Poetry projects, Habushu uses the [poetry-monorepo-dependency-plugin](https://pypi.org/project/poetry-monorepo-dependency-plugin/)'s `build-rewrite-path-deps` and `publish-rewrite-path-deps` commands in place of Poetry's `build` and `publish` commands, respectively.
For uv projects, Habushu uses the [uv-monorepo-dependency-tool](https://pypi.org/project/uv-monorepo-dependency-tool/)'s `build-rewrite-path-deps` command in place of uv's `build` command.

Typically, this flag will only be `true` when deploying/releasing Habushu modules within a CI environment that are part of a monorepo project
structure which multiple Poetry projects depend on one another.

Default: `false`

**Examples:** 
- [Poetry Package Consumer](../examples/poetry/habushu-poetry-package-consumer/README.md)
- [uv Package Consumer](../examples/uv/habushu-uv-package-consumer/README.md)

### forceSync

If using a Poetry version older than `2.0.0`, a value of `true` will result in Poetry installing packages with the `--sync` parameter. Otherwise, a value of `true` will result in Poetry installing packages via the `poetry sync` command.

Default: `false`

**Example:** TODO

### skipLockUpdate

Typically enabled when running CI, this configuration enables skipping the update of Poetry or uv lock file via their `lock` commands. If their lock files does not exist, the subsequent execution of `install` will create it regardless of this configuration. If `poetry.lock` or `uv.lock` have a mismatch with its `pyproject.toml` definition, the build will fail.

Default: `false`

**Example:** TODO

### deleteVirtualEnv

Enables the explicit deletion of the virtual environment that is created/managed by Poetry/uv.
NOTE: Poetry uses its built in command to delete virtual environment while uv doesn't support command to do so, it will be manually deleted.

Example usage: `mvn clean -Dhabushu.deleteVirtualEnv`

Default: `false`

**Example:** TODO

### skipDeploy

Skips the execution of the `deploy` phase and does *not* publish the package to the configured PyPI repository. This configuration may be useful when individual Habushu modules within a larger multi-module project hierarchy should *not* be published to PyPI.

Default: `false`

### snapshotNumberDateFormatPattern

[DateTimeFormatter](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html) compliant pattern that configures the numeric portion of `SNAPSHOT` package versions that are published to the configured PyPI repository. By default, the version of `SNAPSHOT` published packages align with [PEP-440 developmental releases](https://peps.python.org/pep-0440/#developmental-releases) and use a numeric component that corresponds to the number of seconds since the epoch. For example, if the POM version is `1.2.3-SNAPSHOT`, the package may be published by default as `1.2.3.dev1658238063`. If this property is specified, the numeric component will reflect the given date format pattern applied to the current build time. For example, if `YYYYMMddHHmm` is provided, `1.2.3.dev202207191002` be published.

Default: Number of seconds since epoch

### overridePackageVersion

Enables the encapsulated package's `pyproject.toml` defined version to be automatically managed and overridden by Habushu in the following build phases/mojos:

* `initialize`: Automatically sets the package version to the version specified in the POM. If the POM is a `SNAPSHOT`, the package version will be set to the corresponding developmental release version without a numeric component (i.e. POM version of `1.2.3-SNAPSHOT` will result in the package version being set to `1.2.3.dev`). If the version is a release candidate (`rc`), `alpha`, or `beta` version in [SemVer 2.0](https://semver.org/#spec-item-9) format then it is translated to the equivalent [PEP 440](https://peps.python.org/pep-0440/#public-version-identifiers) format.
* `compile`: Automatically set up any `managedDependency` that is a `SNAPSHOT` to instead include `.*`.  `.*` is used rather than `.dev` so the latest dev version will resolve as `.dev` is not a valid version.  Same result as what happens in the `validate` step above, but operating on `managedDependency` entries instead.  This is useful when you are working with multi-module builds where several versions are covered via managed dependencies.  See [managedDependencies](#managed-dependencies) for more context.
* `deploy`: Automatically sets the version of published packages that are `SNAPSHOT` modules to timestamped developmental release versions (i.e. POM version of `1.2.3-SNAPSHOT` will result in the published package version of `1.2.3.dev1658238063`). After the package is published, the version of the `SNAPSHOT` module is reverted to its previous value (i.e. `1.2.3.dev`)

If this property is set to `false`, none of the above automated version management operations will be performed.

Default: `true`

### sourceDirectory

Folder in which Python source files are located - should align with standard project structure conventions. Developers will typically **not** modify this property but is made available for customization to support unanticipated scenarios.

Default: `${project.basedir}/src`

### testDirectory

Folder in which Python test files are located - should align with standard project structure conventions.
Developers will typically **not** modify this property but is made available for customization to support unanticipated scenarios.

Default: `${project.basedir}/tests`

### testPackage

The test package used for testing. Supporting test package is either `behave` or `pytest`. If neither was set, it will always fall back to `behave`.

Default: `behave`
**Note:** Auto detection for the test package will take precedence over this configuration. If there is no `behave` or `pytest` configured in the `pyproject.toml` file, this configuration will take place.

### mavenArtifactFile

Location of the artifact that will be published for this module.  Maven wants to install an artifact with the pom file.
Habushu is really just interested in publishing the pom file for Maven Reactor use, but pushing an artifact makes this
process more smooth.  This parameter can be updated to customize the artifact used.  A placeholder file is explicitly
used as the default to prevent confusion if we published a real artifact (e.g., wheel file) that would never be used in
normal circumstances.

Default: `${project.basedir}/target/habushu.placeholder.txt`

### workingDirectory

Controls where the clean plugin will determine where Poetry projects are located - should always be the basedir of the encapsulating Maven project.

Default: `${project.basedir}`

### distDirectory

Controls where the clean plugin will delete dist artifacts.

Default: `${project.basedir}/dist`

### targetDirectory

Controls where the clean plugin will delete target artifacts.

Default: `${project.basedir}/target`

### lint

Enables linting for the module. Leverages the [ruff linter](https://docs.astral.sh/ruff/linter/) package in the `process-classes` lifecycle step.

Default: `true`

### useFormatter

Enables formatting for the module. Leverages the [ruff formatter](https://docs.astral.sh/ruff/formatter/) package in the `process-classes` lifecycle step.

Default: `true`

### runCommandArgs

**Only applicable when executing the [run-command-in-virtual-env](HABUSHU_LIFECYCLE_README.md#run-command-in-virtual-env) plugin goal.**

Whitespace-delimited command arguments that will be provided to `run` to execute.

Default: None

**Example:** [Running Python Scripts](../examples/habushu-running-python-scripts/README.md)

## Dependency Group Configurations
### withGroups

Specifies which dependency groups to include within the installation.  Example usage:
```shell
mvn clean install -Dhabushu.withGroups=dev,test
```
or
```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
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

**Examples:**
- [Poetry Dependency Groups](../examples/poetry/habushu-poetry-dependency-groups/README.md)
- [uv Dependency Groups](../examples/uv/habushu-uv-dependency-groups/README.md)

### withoutGroups
Specifies dependency groups to exclude within the installation.  Example usage:

```shell
mvn clean install -Dhabushu.withoutGroups=dev,test
```
or
```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
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

**NOTE:** While Habushu does support this configuration for `uv`, `uv sync --no-group <group>` is underdeveloped and does not possess
strong useful functionality at this time.

**Examples:**
- [Poetry Dependency Groups](../examples/poetry/habushu-poetry-dependency-groups/README.md)

## Managed Dependencies Configurations

**Example:** [Managed Dependencies](../examples/habushu-managed-dependencies/README.md)

### managedDependencies

Optional set of dependencies to manage across modules extending a parent pom. This allows packages to be managed to a
specific version, which is often useful to ensure that information assurance patches, common versions, etc. are enforced
across a series of modules. Can be used with the next several variables to control automatic update, logging, or failing
the build when mismatches are found between the managed dependency operator/version and what is currently specified.
Looks at the various dependencies sections of your `pyproject.toml`.

Default: None

**Example:** [Managed Dependencies](../examples/habushu-managed-dependencies/README.md)

### updateManagedDependenciesWhenFound

Determines if managed dependency mismatches are automatically updated when encountered.

Default: `true`

### failOnManagedDependenciesMismatches

Determines if the build should be failed when managed dependency mismatches are found.

Default: `false`
## Behave Configurations

### behaveOptions

Options that should be passed to the `behave` command when executing tests. If this value is provided, then `behaveExcludeManualTag` is ignored.

`behave` supports a [number of command line options](https://behave.readthedocs.io/en/stable/behave.html#command-line-arguments) - developers may adjust the default test execution behavior to optimize productivity, such as selectively executing features associated with a specific in-flight tag (`mvn clean test -Dhabushu.behaveOptions="--tags wip-feature"`).

Default: None

**Examples:**
- [A Simple Poetry Project](../examples/poetry/habushu-poetry-package)
- [A Simple uv Project](../examples/uv/habushu-uv-package)

### disableOutputCapture

Allow stdout, stderr and logs to be printed to the console during `behave` tests.

**Note:** If users want more granular control over logging output, they can set `disableOutputCapture` to `false` and use `behaveOptions`:
`mvn clean test -Dhabushu.disableOutputCapture=false -Dhabushu.behaveOptions="--no-logcapture --no-capture"`

Default: `true`

### behaveExcludeManualTag

Exclude any BDD scenario or feature file tagged with `@manual`.

**NOTE:** If `behaveOptions` are provided, this property is ignored.

Default: `true`

**Example:** [Poetry Package Consumer](../examples/poetry/habushu-poetry-package-consumer/README.md)

### behaveTestEnvironmentVariables

Enables the ability to set environment variables when executing the behave tests. The environment variables should be
defined in the pom.xml:
```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        <behaveTestEnvironmentVariables>
            <ENV_VAR>VALUE</ENV_VAR>
        </behaveTestEnvironmentVariables>
    </configuration>
</plugin>
```
To pass environment variables in via the command line using the `-D` option, use a property placeholder in the pom.xml:
```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        <behaveTestEnvironmentVariables>
            <ENV_VAR>${habushu.ENV_VAR}</ENV_VAR>
        </behaveTestEnvironmentVariables>
    </configuration>
</plugin>
```
Then you can specify the value from the cli: `mvn clean install -Dhabushu.ENV_VAR=customValue`. A default value can be
set in the `<properties>` tag of the pom.xml.

Default: None

**Examples:**
- TODO

### skipTests

Skips running tests.  Using this property is **NOT RECOMMENDED** but may be convenient on occasion.

Example usage: `mvn clean install -Dhabushu.skipTests=true`
**Note:** this will skip both `behave` and `pytest` tests

Default: `false`

### outputCucumberStyleTestReports

Controls whether the Behave testing framework should export a `cucumber.json` and corresponding Cucumber-style HTML report instead of the default test output format.

Default: `true`

### omitSkippedTests

Controls whether skipped tests should be completely omitted from test reports rather than showing up as a skip/failure. This mimics the default behavior of Cucumber and will have no effect if `outputCucumberStyleTestReports` is not set to `true`

Default: `true`

## Pytest Configurations

### pytestOptions

Options that should be passed to the `pytest` command when executing tests. If this value is provided, then `enableVerbose` is ignored.

`pytest` supports a [number of command line options](https://docs.pytest.org/en/6.2.x/reference.html#command-line-flags) - developers may adjust the default test execution behavior to optimize productivity, such as selectively executing tests associated with a [custom registered mark](https://docs.pytest.org/en/stable/how-to/mark.html#registering-marks) in the `pyproject.toml` file (`mvn clean test -Dhabushu.pytestOptions="-m integration_test"`).

Default: None

**Examples:**
- [Pytest with uv](../examples/uv/habushu-uv-pytest)

### enableVerbose

Enable a more detail test outcome including which test passed, failed, or is skipped to be printed to the console during `pytest` test.

Default: `false`

**Examples:**
- [Pytest with uv](../examples/uv/habushu-uv-pytest)

### pytestTestEnvironmentVariables

Enables the ability to set environment variables when executing the pytest tests. The environment variables should be
defined in the pom.xml:
```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <version>${project.version}</version>
    <configuration>
      <!-- if the pytest is configured in the pyproject.toml file, you can ignore the `testPackage` configuration -->
      <testPackage>pytest</testPackage>
      <pytestTestEnvironmentVariables>
        <ENV_VAR>VALUE</ENV_VAR>
      </pytestTestEnvironmentVariables>
    </configuration>
</plugin>
```
To pass environment variables in via the command line using the `-D` option, use a property placeholder in the pom.xml:
```xml
<plugin>
  <groupId>org.technologybrewery.habushu</groupId>
  <artifactId>habushu-maven-plugin</artifactId>
  <version>${project.version}</version>
  <configuration>
    <!-- if the pytest is configured in the pyproject.toml file, you can ignore the `testPackage` configuration -->
    <testPackage>pytest</testPackage>
    <pytestTestEnvironmentVariables>
      <ENV_VAR>${habushu.ENV_VAR}</ENV_VAR>
    </pytestTestEnvironmentVariables>
  </configuration>
</plugin>
```
Then you can specify the value from the cli: `mvn clean install -Dhabushu.ENV_VAR=customValue`. A default value can be
set in the `<properties>` tag of the pom.xml.

Default: None

**Examples:**
- [Pytest with uv](../examples/uv/habushu-uv-pytest)

### skipTests

Skips running tests.  Using this property is **NOT RECOMMENDED** but may be convenient on occasion.

Example usage: `mvn clean install -Dhabushu.skipTests=true`
**Note:** this will skip both `behave` and `pytest` tests

Default: `false`

## Requirements.Txt Configurations

### exportRequirementsFile

Enables the generation of a `requirements.txt` file during the `package` phase.

Default: `true`

### exportRequirementsFolder

Specifies where the `requirements.txt` file will be generated to during the `package` phase.

Default: `project-directory/dist` to be with the generated wheels.

### exportRequirementsWithUrls

Includes source repository urls in the `requirements.txt` files. Available for Poetry projects only.

Default: `false`

### exportRequirementsWithHashes

Includes package hashes in the `requirements.txt` files.

Default: `false` 

### exportRequirementsWithoutPathDependencies

Deprecated. Use the [containerize-dependencies](HABUSHU_LIFECYCLE_README.md#containerize-dependencies) goal instead.

## Repository Configurations

### decryptPassword

Specifies whether Habushu should attempt to decrypt the remote server password provided in Maven's `settings.xml` file.  If `false`, the password will be retrieved as-is and assumed to be unencrypted.

**Warning:** Storage of plain-text passwords is a security risk!  This functionality is best used when the password is stored in a safe manner outside of Maven's native credential system, and is decrypted prior to execution (Jenkins credentials, for instance).

Default: `true`

### pypiPushRetries
Specifies the number of times a push to the configured PyPI repository will be attempted before stopping (inclusive
of the initial attempt). While this defaults to three and is fully configurable, it can be set to zero to never
retry or set to any negative number for unlimited retries.  Unlimited retries will follow a fibonacci backoff
interval.

Default: `3`

### Development Repository Configurations
Illustrations of these configurations can be found in the following examples:
**- Poetry Examples:**
  - [Publish to Development Repository](../examples/poetry/habushu-poetry-publish-to-dev-repo/README.md)
  - [Install from Development Repository](../examples/poetry/habushu-poetry-install-from-dev-repo/README.md)
**- uv Examples:**
  - [Publish to Development Repository](../examples/uv/habushu-uv-publish-to-dev-repo/README.md)
  - [Install from Development Repository](../examples/uv/habushu-uv-install-from-dev-repo/README.md)

#### useDevRepository
Instructs deployment to use a development repository rather than a release repository. This is conceptually
similar to Maven's release vs. snapshot repositories, allowing the release repository to only have formal
releases with a separate repository for all 'dev' releases.  Works in conjunction with the `devRepositoryId`
Member and `devRepositoryUrl` properties

Default: `false`

#### devRepositoryId

Specifies the `<id>` of the `<server>` element declared within the utilized Maven `settings.xml` configuration that
represents the development PyPI repository to which this project's archives will be published and/or used as a
supplemental repository from which dependencies may be installed. This property is **REQUIRED** if publishing to or
consuming dependencies from a private PyPI repository that requires authentication - it is expected that the relevant
`<server>` element provides the needed authentication details.

If this property is **not** specified, this property will default to `dev-pypi` and the execution of the `deploy`
lifecycle phase will publish this package to the official public Test PyPI repository.  Downstream package publishing
functionality will use the relevant `settings.xml` `<server>` declaration with `<id>dev-pypi</id>` as credentials for
publishing the package to PyPI. If developers want to use PyPI's [API tokens](https://pypi.org/help/#apitoken) instead of username/password
credentials, they may do so by manually executing the appropriate Poetry command
(`poetry config pypi-token.pypi my-token`) in an ad-hoc fashion prior to running `deploy`.

This property will typically be specified as a command line option during the `deploy` lifecycle phase.  For example,
given the following configuration in the utilized `settings.xml`:

```xml
<server>
    <id>dev-pypi</id>
    <username>pypi-repo-username</username>
    <password>{encrypted-pypi-repo-password}</password>
</server> 
```

The following command may be utilized to publish the package to the specified private PyPI repository at
`https://private-pypi-repo-url/repository/pypi-repo/`:

```sh 
$ mvn deploy -Dhabushu.pypiRepoId=dev-pypi -Dhabushu.pypiRepoUrl=https://private-pypi-repo-url/repository/pypi-repo/
```
Default: `dev-pypi`

#### devRepositoryUrl

Specifies the URL of the private development PyPI repository to which this project's archives will be published and/or
used as a supplemental repository from which dependencies may be installed. This property is **REQUIRED** if
publishing to or consuming dependencies from a private dev PyPI repository.

If the Habushu project depends on internal packages that may only be found on a private dev PyPI repository,
developers should specify this property through the plugin's `<configuration>` definition:

```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        <devRepositoryUrl>https://private-pypi-repo-url/repository/pypi-repo</devRepositoryUrl>
    </configuration>
</plugin>
```
Default: `https://test.pypi.org`

#### enableDevRepositoryUrlUploadSuffix

Enables whether to append the path to the upload index relative to the `devRepositoryUrl`.

Default: `true`

#### devRepositoryUrlUploadSuffix

Specifies the path to the upload index relative to the devRepositoryUrl.  Certain private repository solutions use
non-standard paths (ie: `test.pypi.org` uses `+legacy/`).

Default: `legacy/`

### Publishing Repository Configurations

See the [Development Repository Configurations](#development-repository-configurations) section for examples of a similar set of configurations.

#### pypiRepoId

Specifies the `<id>` of the `<server>` element declared within the utilized Maven `settings.xml` configuration that represents the PyPI repository
to which this project's archives will be published and/or used as a supplemental repository from which dependencies may be installed. This property is **REQUIRED** if publishing to or consuming dependencies from a private PyPI repository that requires authentication - it is expected that the relevant `<server>` element provides the needed authentication details.

If this property is **not** specified, this property will default to `pypi` and the execution of the `deploy` lifecycle phase will publish this package to the official public PyPI repository.  Downstream package publishing functionality will use the relevant `settings.xml` `<server>` declaration with `<id>pypi</id>` as credentials for publishing the package to PyPI. If developers want to use PyPI's [API tokens](https://pypi.org/help/#apitoken) instead of username/password credentials, they may do so by manually executing the appropriate Poetry command (`poetry config pypi-token.pypi my-token`) or uv command (`export UV_INDEX_PYPI_USERNAME=<YOUR-USERNAME> && export UV_INDEX_PYPI_PASSWORD=<YOUR-PASSWORD-OR-TOKEN>` in an ad-hoc fashion prior to running `deploy`.

This property will typically be specified as a command line option during the `deploy` lifecycle phase.  For example, given the following configuration in the utilized `settings.xml`:

```xml
<server>
    <id>private-pypi-repo</id>
    <username>pypi-repo-username</username>
    <password>{encrypted-pypi-repo-password}</password>
</server> 
```

The following command may be utilized to publish the package to the specified private PyPI repository at https://private-pypi-repo-url/repository/pypi-repo/:

```sh 
$ mvn deploy -Dhabushu.pypiRepoId=private-pypi-repo -Dhabushu.pypiRepoUrl=https://private-pypi-repo-url/repository/pypi-repo/
```
Default: `pypi`

#### pypiRepoUrl

Specifies the URL of the private PyPI repository to which this project's archives will be published and/or used as a supplemental repository from which dependencies may be installed. This property is **REQUIRED** if publishing to or consuming dependencies from a private PyPI repository.

If the Habushu project depends on internal packages that may only be found on a private PyPI repository, developers should specify this property through the plugin's `<configuration>` definition:

```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        <pypiRepoUrl>https://private-pypi-repo-url/repository/pypi-repo</pypiRepoUrl>
    </configuration>
</plugin>
```

Default: None

#### enablePypiSimpleSuffix

Determines whether to append the path to the simple index relative to the given PyPI repository URL.

Default: `true`

#### pypiSimpleSuffix

Specifies the path to the simple index relative to the `pypiRepoUrl`.  Certain private repository solutions use non-standard paths (ie: devpi uses `+simple`). `enablePypiSimpleSuffix` must be set to `true` to enable usage.

Default: `simple`

#### pypiUploadSuffix

Specifies the path to the upload index relative to the `pypiRepoUrl`.  Certain private repository solutions use non-standard paths.

Default: None

#### addPypiRepoAsPackageSources

Configures whether a private PyPI repository, if specified via `pypiRepoUrl`, is automatically added as a package source from which dependencies may be installed. This value is **only** utilized if a private PyPI repository is specified via `pypiRepoUrl`.  Developers will typically not need to configure this property, but is made available to support the manual configuration of a custom repository in `pyproject.toml` if needed.

Default: `true`

## Containerization Configurations

**Example:**
- [Containerizing a Python Application](../examples/habushu-containerize/README.md)

### stagingDirectory

Controls the location of where containerization files will be copied to as part of the [containerize-dependencies](HABUSHU_LIFECYCLE_README.md#containerize-dependencies) goal.

Default: `${project.build.directory}/containerize-support`

### updateDockerfile

Enables the automatic injection of containerization logic to the dockerfile for the [containerize-dependencies](HABUSHU_LIFECYCLE_README.md#containerize-dependencies) goal.

Default: `true`

### dockerfile

The Dockerfile to update with containerization logic during the [containerize-dependencies](HABUSHU_LIFECYCLE_README.md#containerize-dependencies) goal. This must be set if the `updateDockerfile` is set to `true`.

Default: None

### dockerBuilderBase

The base image to use for building the virtual env. This base image will be used to bundle the virtual environment for the target project. As the venv must be built on the same platform as the final runtime to ensure compatibility, this image must share a platform with [dockerFinalBase](#dockerfinalbase). The base image must have the `python3` resolvable via the `PATH`.

Default: `docker.io/python:3.12`

### dockerFinalBase

The base image to use for final packaging of the virtual env. This base image will be used to run the final container runtime.  As the venv must be built on the same platform as the final runtime to ensure compatibility, this image must share a platform with [dockerBuilderBase](#dockerBuilderBase) and must have Python installed to the same filesystem location.

Default: `docker.io/python:3.12-slim`

### dockerUser

The user to set as the owner of the virtual env. This is useful when the Docker build is run as a non-root user. Set to an empty string to disable.

Default: `1001`

### dockerVenvDirectoryPermissions

The directory permissions for the virtual env. Set to an empty string to disable.

Default: `744`

### dockerContext
The directory that will serve as the context for the Docker build. This directory must contain the `stagingDirectory`.

Default: `project.basedir`

### dockerTemplatePath

Overwrite with Docker template path if a custom template is preferred.

Default: Habushu Maven Plugin Classpath

### dockerBuilderStageTemplate

The default Dockerfile builder stage template. Overwrite if a custom template is preferred.

Default: `templates/dockerfile_builder_stage_template.vm`

### dockerFinalStageTemplate

The default Dockerfile final stage template. Overwrite if a custom template is preferred.

Default: `templates/dockerfile_final_stage_template.vm`

## Version Enforcement Configurations

### version

The **REQUIRED** version or version range Poetry or uv must have to pass the Enforcer check.

Default: None

### Poetry-Specific Version Enforcement Configurations

#### requirePoetryVersion
**Example:** [Poetry Version Enforcement](../examples/poetry/habushu-poetry-enforcer-rule/README.md)

The **REQUIRED** rule name to use with the Maven Enforcer Plugin to validate Poetry versions.

Default: None

#### requirePyenvVersion
**Example:** [Pyenv Version Enforcement](../examples/poetry/habushu-pyenv-enforcer-rule/README.md)

The **REQUIRED** rule name to use with the Maven Enforcer Plugin to validate Pyenv versions.

Default: None

### uv-Specific Version Enforcement Configurations

**Example:** [uv Version Enforcement](../examples/uv/habushu-uv-enforcer-rule/README.md)

#### requireUvVersion

The **REQUIRED** rule name to use with the Maven Enforcer Plugin to validate uv versions.

Default: None
 
## Poetry-Specific Configurations

### usePyenv

This configuration applies only to Poetry projects. If true, Habushu will delegate to `pyenv` for managing and (if needed) installing the specified version of Python. If false, Habushu will look for the desired version of Python on the `PATH`. If Python is not found or if the version does not match the configured `pythonVersion`, the build will fail.

Default: `true`

### useInProjectVirtualEnvironment

Enables Poetry's `virtualenvs.in-project` for this project. If configured with an existing virtual environment elsewhere,
the virtual environments will be migrated to this approach during the clean phase of the build.

While generally easier to find and use for tasks like debugging, having your virtual environment co-located in
your project may be less useful for executions like CI builds where you may want to centrally caches virtual
environments from a central location.

Default: `true`

### poetryMonorepoDependencyPluginVersion

Specifies the specific version of the`poetry-monorepo-dependency-plugin` to be installed.

Default: `latest`


