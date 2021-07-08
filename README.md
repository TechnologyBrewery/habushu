# Habushu #
[![Maven Central](https://img.shields.io/maven-central/v/org.bitbucket.cpointe.habushu/root.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.bitbucket.cpointe.habushu%22%20AND%20a%3A%22habushu%22)
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit)

In Okinawa, habushu (pronounced HA-BU-SHU) is a sake that is made with venomous snake. The alcohol in the snake assists in dissolving the snake's venom and making it non-poinsonous. In Maven, habushu allows virtual environment-based python projects to be included as part a Maven build. This brings some order and consistency to what can otherwise be haphazardly structured projects.

## Approach ##
Habushu is implemented as a series of [Maven](https://maven.apache.org/) plugins that tie together existing tooling in an opinionated fashion similar to how Maven structures Java projects.  By taking care of manual steps and bringing a predictable order of execution and core naming conventions, habushu increases time spent on impactful work rather than every developer or data scientist building out scripts that meet their personal preferences.  It's likely that no one person will agree with all the opinions habushu brings forth, but the value in being able to run entire builds from a single `mvn clean install` command regardless of your prior experience with the projects adds substantial value - both locally and in DevSecOps scenarios.

## Using Habushu ##

### Adding to your Maven project
Adding habushu to your project is easy.  

#### Update Packaging ####
Change the packaging of your module's pom to use habushu:
```
	<packaging>habushu</packaging>
```

#### Add the Habushu Plugin ####
Add the following plugin to your module's pom's build section:
```
	<plugin>
		<groupId>org.bitbucket.cpointe.habushu</groupId>
		<artifactId>habushu-maven-plugin</artifactId>
		<version>${project.version}</version>
		<extensions>true</extensions>
	</plugin>
	<plugin>
		<artifactId>maven-clean-plugin</artifactId>
		<version>3.1.0</version>
		<configuration>
		  	<excludeDefaultDirectories>true</excludeDefaultDirectories>
		  	<filesets>
				<fileset>
					<directory>target</directory>
					<excludes>
						<exclude>build-accelerator/**</exclude>
						<exclude>virtualenvs/**</exclude>
					</excludes>
				</fileset>
			</filesets>
		</configuration>
	</plugin>
```

#### Configure Maven Settings.xml ####
Add the following to your Maven's settings.xml file, usually located under the .m2 folder.

```
    <server>
        <id>ID of the PyPi hosted repository</id>
        <username>username for the repository</username>
        <password>encrypted password for the repository</password>
    </server>
```

#### Resulting Build Lifecycle ####
After performing the steps above, your module will leverage the habushu lifecycle rather than the default lifecycle. It consists of the following stages. 

Configuration for options described below can be accomplished as described in the Test Configuration Options section below.

##### clean #####
Leverages the standard [`maven-clean-plugin`](https://maven.apache.org/plugins/maven-clean-plugin/index.html) to clear out portions of the `target` directory when clean is passed to the build. A standard build will not clear the `build-accelerator` or `virtualenvs` directories, containing the hashed dependency file and the virtual environment configuration, respectively. These directories may be forcibly cleaned with the `habushu.force.clean` build option.

##### resources #####
Habushu has extended the default [`maven-resources-plugin`](https://maven.apache.org/plugins/maven-resources-plugin/) to copy anything in `src/main/python`, `src/main/resources` into the `target/staging` directory. The project's `pom.xml` and Venv dependency file are also included. These copies can then be used for testing and will be included in the zip file produced later in the lifecycle. All configurations options are listed in the plugin's documentation.  The following configuration options can be specified via standard Maven configuration for plugins:

* _venvDependencyFile:_ The location of your Venv dependency file.  By default, this will point to `requirements.txt` directly within the root of the module.
* _pythonSourceDirectory:_ The directory in which your source code should be placed.  By default, `src/main/python`.  It is highly discouraged to change this value.
* _resourcesDirectory:_ The directory in which your resources should be placed.  BY default, `src/main/resources`.  It is highly discouraged to change this value.

##### configure-environment #####
Create or update your Venv virtual environment. This ensures it is valid and that any tests are run in the versioned controlled environment. The following configuration options can be specified via standard Maven configuration for plugins:  

* _venvDirectory:_ The root location of your virtual environment.  By default, this will point to the /virtualenvs/ folder directly under the project build directory (usually the target folder).
* _workingDirectory:_ The location in which any venv commands will be run.  By default, this is `target`.
* _pythonSourceDirectory:_ The directory in which your source code should be placed.  By default, `src/main/python`.  It is highly discouraged to change this value.
* _pathToVirtualEnvironment:_ The path to your specified virtual environment.  By default, this will simply add your environmentName onto the file path of your `venvDirectory`.
* _pythonVersion:_ The version of python you wish to use. Currently we only support versions 3.7.X, other versions may be supported in the future. By default, this is set to 3.7.10. If your preinstalled version of python does not match what is specified then habushu will install python for you using pyenv.

##### test #####
Run behave if any files exist within the `src/test/python/features` directory. More information on authoring and running tests can be found later in this document, including configuration options.

##### zip #####
Habushu has extended the [`maven-assembly-plugin`](http://maven.apache.org/plugins/maven-assembly-plugin/) to create a zip file from all the files in the `target/staging` directory. All configurations options are listed in the plugin's documentation.

##### package-and-release-python #####
Habushu contains a lifecycle phase that will package a client Python project into a wheel file (.whl), and optionally upload it to a specified remote repository.
To release a wheel file to the remote repository, use the `habushu.perform.release` build option in Maven.

For a successful release to your remote PyPi hosted repository, you will need to configure the following options:

* _repositoryUrl:_ The URL of the remote repository.
* _repositoryId:_ The ID of the remote repository.

Additionally, the following configuration options can be specified via standard Maven configuration for plugins:

* _stagingDirectory:_ The directory in which the build stages files.  By default, this is `{project.basedir}/target/staging`.
* _distDirectory:_ The directory in which the wheel file is located after it is built.  By default, this is `{project.basedir}/target/staging/dist`.
* _packageWheelScript:_ The bash script that will package a Python project into wheel format.
* _uploadWheelScript:_ The bash script that will upload a Python wheel to the remote repository.
* _repositoryId:_ The ID of a remote repository to upload the generated Python wheel to.
* _repositoryUrl:_ The full URL (including leading https://) of the repository to upload the generated Python wheel to.

Python release process and versioning:

The process for releasing Python artifacts differs from the standard Maven release cycle in a number of ways.  Python does not recognize "SNAPSHOT" releases, and instead uses a versioning scheme generally agreed upon by Python developers to differentiate between major, minor, and micro releases.  Accordingly, there is no snapshot repository - the habushu-maven-plugin is configured to point to a release repository for Python projects.  Python also does not use an SCM connection to tag builds from Git like Maven; instead, the developer will need to utilize the project's setup.py file to tag the build themselves with any relevant information prior to packaging and releasing the build artifact.

Steps to run a release:
1. Make any changes to the project using the habushu-maven-plugin.
2. Edit the project's `setup.py` file to change the version number or other build information.  This should differ in a meaningful way from prior versions, so that a new artifact is created.
3. Run the build with the `habushu.perform.release` option, which will upload the generated wheel file to your remote PyPi hosted repository.

Important notes:
Versioning for wheel files is specific, and should follow standard convention.
[Wheel file naming convention](https://www.python.org/dev/peps/pep-0491/#file-name-convention)

##### install #####
Moves the assembly creates in the zip lifecycle to the local machine's `.m2/respository` per the standard Maven lifecycle. This includes information for versioning SNAPSHOT (development) and released versions via GAV (groupId, artifactId, version) naming conventions and allows the artifact to be pulled by GAV with any local Maven-compliant dependency fetching algorithm.  If needed, more information is available on the [`maven-install-plugin`](https://maven.apache.org/plugins/maven-install-plugin/) page.

##### deploy #####
The same as install, but deploys the artifact and associated GAV files to a server to reuse across environments via Maven-compliant dependency fetching algorithms. This will often include digital signature files to ensure the integrity of deployed files (automatic within the standard Maven process for released versions). If needed, more information is available on the [`maven-deploy-plugin`](https://maven.apache.org/plugins/maven-deploy-plugin/) page.

### Adding Tests ###
Habushu leverages [behave](https://behave.readthedocs.io/en/stable/index.html) to run Cucumber tests quiskly and easily.

#### Add Feature Files ####
You can add your [Gherkin feature files](https://cucumber.io/docs/gherkin/) to the `src/test/python/features` directory.  You can change the location of where test code is located via the `pythonTestDirectory` configuration value using [standard Maven configuration procedures](https://maven.apache.org/guides/mini/guide-configuring-plugins.html), though this is highely discouraged.
 
#### Run Build to Generate Step Stubs ####
As is customary with Cucumber testing, once you add your feature, stubbed methods can be created for each step by running the build via `mvn clean test`.  Any step that is missing will be listed at the end of the build, allowing you to copy that snippet and then add in the implementation.  For example:
```
[INFO] Failing scenarios:
[INFO]   ../../src/test/python/features/example.feature:30  This is an unimplemented test for testing purposes

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

## Building Habushu ##
If you are working on habushu, please be aware of some nuances in working with a plugin that defines a lifecycle and packaging. We use `habushu-mixology` to immediately test our plugin. However, if the `habushu-maven-plugin` has not been previously built (or was built with critical errors), you need to manually build the `habushu-maven-plugin` first.  To assist, there are two profiles available in the build:

* `mvn clean install -Pbootstrap`: will build the `habushu-maven-plugin` so `habushu-mixology` can be executed in subsequent default builds.  Note that this also means that mixology lifecycle changes require two builds to test - one to build the lifecycle, then a second to use that updated lifecycle.  Code changes within the existing lifecycle work via normal builds without the need for a second pass.
* `mvn clean install -Pdefault`: (ACTIVE BY DEFAULT - `-Pdefault` does not need to be specified) builds all modules.
* `mvn clean install -Dhabushu.force.clean`: will clean out the virtual environment configuration and dependency file to force the full re-construction of the environment.

## Common Issues ##

### Pyenv not installed
Pyenv is used to change your python version and to run all python scripts. If you encouter an error for pyenv not being installed, it will direct you to a github repository with directions on how to install pyenv.

### Plugin Does Not Yet Exist ###
If you encounter the following error, please see the Building Habushu section for details on how to use the bootstrap profile to get around this issue.
```
[WARNING] The POM for org.bitbucket.cpointe.habushu:habushu-maven-plugin:jar:0.0.1-SNAPSHOT is missing, no dependency information available
[ERROR] [ERROR] Some problems were encountered while processing the POMs:
[ERROR] Unresolveable build extension: Plugin org.bitbucket.cpointe.habushu:habushu-maven-plugin:0.0.1-SNAPSHOT or one of its dependencies could not be resolved: Could not find artifact org.bitbucket.cpointe.habushu:habushu-maven-plugin:jar:0.0.1-SNAPSHOT @ 
[ERROR] Unknown packaging: habushu @ line 15, column 13
``` 
### Zlib/readline/openssl not available ###

If you encounter an error such as ones below, ensure that you have Zlib, Readline, and openssl@1.1 installed to your system and on your path. Depending on your operatng system, installation methods may vary.
```
zipimport.ZipImportError: can't decompress data; zlib not available
# or
pip is configured with locations that require TLS/SSL, however the ssl module in Python is not available.
```
If the errors persist while on macos, try the following (note the zlib folder version may be different for you):

# For zlib
```
brew install zlib
brew install xquartz
ln -s /usr/local/Cellar/zlib/1.2.11/include/* /usr/local/include
```
Then add the following to your .bashrc and .bash_profile
```
#.bashrc
export LDFLAGS="${LDFLAGS} -L/usr/local/opt/zlib/lib"
export CPPFLAGS="${CPPFLAGS} -I/usr/local/opt/zlib/include"
export PKG_CONFIG_PATH="${PKG_CONFIG_PATH} /usr/local/opt/zlib/lib/pkgconfig"
#.bash_profile
```
Then make sure that you restart your terminal

# For openssl
Add the following to the beginning your .bash_profile:
```
export PATH="/usr/local/opt/openssl@1.1/bin:$PATH"
export PKG_CONFIG_PATH="/usr/local/opt/openssl@1.1/lib/pkgconfig"
```

### C Compiler Issues ###

Occasionally, updates to MacOS machines will cause issues with the C compiler that is installed through Xcode (sometimes also called "Apple Developer Tools").  The Habushu build makes use of Xcode and the C compiler for downloading and installing Python dynamically when an installation cannot be found that matches the Python version provided to the build.

If you run into issues with the C compiler or the Python download/installation process, please reinstall Xcode.  On a MacOS, this will force a reinstallation of the C compiler as well.

The following commands reinstall XCode.

```
sudo rm -rf /Library/Developer/CommandLineTools
sudo xcode-select --install
```
