[[Return to Examples Documentation]](../../README.md)

# Containerizing Dependencies with Poetry

This  example leverages the [containerize-dependencies](../../../docs/HABUSHU_LIFECYCLE_README.md#containerize-dependencies) goal in the `prepare-package` phase to prepare a containerized virtual environment. The default example uses the following configs: [dockerfile](../../../docs/CONFIGURATION_README.md#dockerfile), [dockerBuilderBase](../../../docs/CONFIGURATION_README.md#dockerBuilderBase), [dockerFinalBase](../../../docs/CONFIGURATION_README.md#dockerFinalBase) and [updateDockerfile](../../../docs/CONFIGURATION_README.md#updatedockerfile). The`custom-docker-template` profile example uses the following additional configs: [dockerTemplatePath](../../../docs/CONFIGURATION_README.md#dockertemplatepath), [dockerPoetryBuilderStageTemplatePath](../../../docs/CONFIGURATION_README.md#dockerpoetrybuilderstagetemplatepath), [dockerPoetryFinalStageTemplatePath](../../../docs/CONFIGURATION_README.md#dockerpoetryfinalstagetemplatepath).

For additional Poetry-specific containerization configurations, see the [Poetry-Specific Containerization Configuration](../../../docs/CONFIGURATION_README.md#poetry-specific-containerization-configurations) documentation.

