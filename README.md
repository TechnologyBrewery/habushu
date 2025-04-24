# Habushu #
[![Maven Central](https://img.shields.io/maven-central/v/org.technologybrewery.habushu/habushu.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.technologybrewery.habushu%22%20AND%20a%3A%22habushu%22)
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://opensource.org/licenses/mit)
[![Build (github)](https://github.com/TechnologyBrewery/habushu/actions/workflows/maven.yaml/badge.svg)](https://github.com/TechnologyBrewery/habushu/actions/workflows/maven.yaml)

Habushu is a Maven-plugin that allows virtual environment-based Python projects to be included as part a Maven build. This brings some order and consistency to what can otherwise be haphazardly structured projects.

## Why Do You Need Habushu? ##

Habushu is implemented as a series of [Maven](https://maven.apache.org/) plugins that tie together existing tooling in an opinionated fashion similar to how Maven structures Java projects.

**By taking care of manual steps and bringing a predictable order of execution and core naming conventions, Habushu increases time spent on impactful work rather than every developer or data scientist building out scripts that meet their personal preferences.**

No one person will agree with all the opinions implemented by Habushu. The value in being able to run entire builds from a single `mvn clean install` command regardless of your prior experience with the projects adds substantial value - both locally and in DevSecOps scenarios.

More information about how Habushu can be used and benefit your project can be found in [this blog post](https://codifyiq.substack.com/p/habushu-supporting-python-in-polyglot), which covers how Habushu helps achieve repeatable Python builds in polyglot monorepos.

## Requirements ##

In order to use Habushu, the following prerequisites must be installed:

* Maven 3.9+ or mvnd 1.0.2+
* Java 11+

If you would like to use Habushu with Poetry-based projects, you must install:
* [Poetry 1.5+](https://python-poetry.org/)
* [Pyenv](https://github.com/pyenv/pyenv)

If you would like to use Habushu with uv-based projects, you must install:
* [uv 0.5+](https://docs.astral.sh/uv/)

Additionally, Habushu may install and manage:

* [poetry-monorepo-dependency-plugin](https://pypi.org/project/poetry-monorepo-dependency-plugin/) for Poetry-based projects
* [uv-monorepo-dependency-tool](https://pypi.org/project/uv-monorepo-dependency-tool/) for uv-based projects

## Usage
Habushu automates a consistent and predictable build lifecycle by delegating *nearly all* commands related to dependency management, virtual environment activation, and package publishing to [Poetry](https://python-poetry.org/) or [uv](https://docs.astral.sh/uv/). As a result, Habushu projects can be either Poetry or uv projects and are expected to align with the conventions, structure, and configurations utilized by these tools. For additional details on integrating Poetry or uv projects with Habushu/Maven, see the [Usage](docs/USAGE_README.md) documentation.

## Configuration
Habushu provides a number of configuration options. For additional details and the list of all configuration options, see the [Configuration](docs/CONFIGURATION_README.md) documentation.

## Habushu Build Lifecycle
Habushu applies a custom lifecycle to Poetry or uv-based projects. For additional details, see the [Habushu Build Lifecycle](docs/HABUSHU_LIFECYCLE_README.md) documentation.

## Examples
Habushu provides extensive example projects using various configurations. For the full list of examples, see the [Examples](examples/README.md) documentation.

## Common Issues ##

### Pyenv/Poetry Not Installed

Pyenv is utilized to install and use the specified version of Python, while Poetry (which uses the Pyenv-managed version of Python) is utilized for all underlying build commands.  Both Pyenv and Poetry **MUST** be installed. If you encounter an error indicating that either tool is not installed or on the `PATH`, you will be prompted with installation guidance.

## Poetry v2.0.0+ Changes ##
When on Poetry v2.0.0 and later, Baton migrations will automatically run on all `pyproject.toml` and `poetry.toml` files in your Habushu project.
This update introduces a number of migrations that ensure configurations are more in line with [PEP 621](https://peps.python.org/pep-0621/).
- `PoetryToProjectMigration`: Adds `[project]` section into TOML file and moves relevant fields from `[tool.poetry]` to `[project]`
- `PoetryToProjectRequiresPythonMigration`: Relocates the Python dependency from `[tool.poetry.dependencies]` to the `requires-python` entry under `[project]`
- `PoetryToProjectDynamicMigration`: Introduces a `dynamic` field under `[project]` that automatically includes `version` and `dependency`, if not already present. If a single `readme` (as a string) is included in `[tool.poetry]`, then migrates it from `[tool.poetry]` to `[project]`. If multiple `readme` values are defined (as a table) in `[tool.poetry]`, `readme` is added to the `dynamic` field list.
- `PoetryTomlMigration`: Removes deprecated configurations from the `poetry.toml` file
- `PoetryRemoveEmptyTomlMigration`: Removes any empty `[tool.poetry]` and `[tool.poetry.dependencies]` headers from `pyproject.toml`

**Note:** Baton migrations are forward compatible only. If you upgrade to Poetry v2.0+ and later decide to downgrade, you will need to manually revert the changes in your TOML files.

## Building Habushu
For details on how to build Habushu for development, see the [Building Habushu](docs/BUILDING_HABUSHU_README.md) documentation.