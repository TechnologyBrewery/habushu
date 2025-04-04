[[Return to Main Documentation]](../../../README.md)

# Habushu UV with a Development Repository Dependency

## Overview

This example demonstrates how to configure a uv project with Habushu to use a development repository for dependencies. 
It shows how to add and manage dependencies in a uv project while leveraging Habushu's capabilities for development 
repository integration.

## Key Features

- **Development Repository Integration**: Configure your uv project to use a development repository for dependencies
- **Repository URL Customization**: Specify custom repository URL suffixes for development environments

## How It Works

The example uses Habushu's Maven plugin to configure a uv project with development repository settings. This is 
particularly useful when:

1. You need to use a development or staging PyPI repository during development
2. You want to maintain consistent dependency management across environments
3. You need to specify custom repository URL paths for uploading packages

## Configuration Details

In the `pom.xml` file, the key configuration elements are:

```xml
<configuration>
    <enableDevRepositoryUrlUploadSuffix>true</enableDevRepositoryUrlUploadSuffix>
    <devRepositoryUrlUploadSuffix>legacy/</devRepositoryUrlUploadSuffix>
</configuration>
```

This configuration:

1. Enables a custom upload suffix for the development repository URL
2. Sets the development repository URL upload suffix to `legacy/`

## Benefits

- **Simplified Development**: Easily switch between development and production repositories
- **Customizable Repository URLs**: Configure custom repository paths for different environments