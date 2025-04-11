[[Return to Main Documentation]](../../../README.md)

# Habushu Poetry Install From Development Repository

## Overview

This example demonstrates how to configure and use Habushu to install dependencies from a private development repository. By leveraging Habushu's configuration capabilities, developers can seamlessly fetch packages from a private repository, streamlining iterative development and testing workflows.

## Key Features

- **Flexible Configuration Options**: Support for configuring dependency installation using Poetry's native configuration or Habushu's advanced configuration.
- **Development Repository Usage**: Works seamlessly with packages published to private repositories, such as those set up in the corresponding [Habushu Poetry Publish to Development Repository](../habushu-poetry-publish-to-dev-repo/README.md) example.
- **Simplified Workflow**: Allows rapid testing of development packages before transitioning to production.

## Benefits

- **Streamlined Installation**: Easily install private development packages without manual intervention.
- **Seamless Testing**: Directly fetch the latest changes made to the development repository for rapid testing.
- **Multi-Tool Compatibility**: Works with both Poetry and Habushu configurations for maximum flexibility.
- **Secure and Reliable**: Supports encrypted credentials for accessing private repositories, ensuring secure installations.

## Prerequisites
- Complete the [Habushu Poetry Publish to Development Repository](../habushu-poetry-publish-to-dev-repo) example.
- Uncomment lines 11 and 24 in the `./pyproject.toml`.

## Habushu Configuration
Follow the instructions in the Habushu Poetry Publish to Development Repository's [Example Setup](../habushu-poetry-publish-to-dev-repo/README.md#example-setup) through the [Update your pom.xml](../habushu-poetry-publish-to-dev-repo/README.md#update-your-pomxml) sections. In this example, the following configuration is located in the `install-example` profile. Outside the `habushu` repository, you can save this configuration in the `build` section of your project/module's `pom.xml` file.

## Build the project
Run `mvn clean install -Pinstall-example` from this module or run `mvn clean install -pl :habushu-poetry-install-from-dev-repo -Pinstall-example` from the root directory to build this module.
