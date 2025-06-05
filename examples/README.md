[[Return to Main Documentation]](../README.md#examples)

# Examples

To illustrate using most of the [configurations](../docs/CONFIGURATION_README.md), we have developed a number of [examples](). Each example has a working module along with a `README.md` file that explains the specific
configuration options.

## General Habushu Examples
- [Default Python Strategy](habushu-default-python-strategy/README.md) - Handles the strategy for setting the default python version if no version is explicitly set in [pythonVersion](../docs/CONFIGURATION_README.md#pythonversion)
- [Managed Dependencies](habushu-managed-dependencies/README.md) - Supports common definition of dependency versions across Maven modules
- [Running Python Scripts](habushu-running-python-scripts/README.md) - Supports running custom python scripts during build phases with the [runCommandArgs](../docs/CONFIGURATION_README.md#runcommandargs) configuration

## Poetry-Specific Examples
- [Poetry Version Enforcement](poetry/habushu-poetry-enforcer-rule/README.md) - Enforces a specific version or version range of Poetry
- [A Simple Poetry Package](poetry/habushu-poetry-package/README.md)
  - Outlines how to integrate Habushu into a Poetry project
  - Outlines the maven commands for the build lifecycle of a Poetry project
  - Illustrates the `mvn test` command and Habushu's [behaveOptions](../docs/CONFIGURATION_README.md#behaveoptions) configuration in the `integration-test` profile
- [Poetry Package Consumer](poetry/habushu-poetry-package-consumer/README.md)
  - Consumes another Poetry package from within the same monorepo structure using Habushu
  - Enables the `rewriteLocalPathDepsInArchives` configuration
  - Disables the `behaveExcludeManualTag` configuration in the `tagged-tests` profile
- [Containerizing Dependencies with Poetry](poetry/habushu-poetry-containerize/README.md) - Containerizes dependencies in a Docker container
- Configure Habushu to use a private development repository for installation and/or publication of packages:
    - [Publish Package to Development Repository](poetry/habushu-poetry-publish-to-dev-repo/README.md)
    - [Install Package From Development Repository](poetry/habushu-poetry-install-from-dev-repo/README.md)
- [Poetry Dependency Groups](poetry/habushu-poetry-dependency-groups/README.md) - Outlines how to use the [withGroups](../docs/CONFIGURATION_README.md#withgroups) and [withoutGroups](../docs/CONFIGURATION_README.md#withoutgroups) configurations

## uv-Specific Examples
- [uv Version Enforcement](uv/habushu-uv-enforcer-rule/README.md) - Enforces a specific version or version range of uv
- [A Simple uv Package](uv/habushu-uv-package/README.md)
  - Outlines how to integrate Habushu into an uv project
  - Outlines the maven commands for the build lifecycle of an uv project
  - Illustrates the `mvn test` command and Habushu's [behaveOptions](../docs/CONFIGURATION_README.md#behaveoptions) configuration in the `integration-test` profile
- [uv Package Consumer](uv/habushu-uv-package-consumer/README.md) 
  - Consumes another uv package from within the same monorepo structure using Habushu
  - Enables the [rewriteLocalPathDepsInArchives](../docs/CONFIGURATION_README.md#rewritelocalpathdepsinarchives) configuration
- [Containerizing Dependencies with uv](uv/habushu-uv-containerize/README.md) - Containerizes dependencies in a Docker container
- Configure Habushu to use private development repository for installation and/or publication of packages:
    - [Publish Package to Development Repository](uv/habushu-uv-publish-to-dev-repo/README.md)
    - [Install Package From Development Repository](uv/habushu-uv-install-from-dev-repo/README.md)
- [uv Dependency Groups](uv/habushu-uv-dependency-groups/README.md) - Outlines how to use the [withGroups](../docs/CONFIGURATION_README.md#withgroups) configuration
- [Testing with pytest](uv/habushu-uv-pytest/README.md) - Configure Habushu to use pytest for automated tests