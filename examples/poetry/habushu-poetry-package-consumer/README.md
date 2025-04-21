[[Return to Examples Documentation]](../../README.md)

# Poetry Package Consumer

## Overview

This package demonstrates how to effectively consume another Poetry package (`habushu-poetry-package`) from within the 
same monorepo structure using Habushu. It showcases Habushu's powerful capabilities for managing dependencies in a 
monorepo environment, with particular focus on seamless development workflows.

## Key Features

- **Local Path References**: During development, the package uses local path references to the `habushu-poetry-package`, 
enabling real-time code changes to be immediately reflected without requiring re-installation.
- **Monorepo Integration**: Leverages the `poetry-monorepo-dependency-plugin` to maintain consistent dependency 
management across the monorepo.
- **Development Mode**: Uses Poetry's `develop = true` flag to create an editable installation, perfect for active 
development across multiple packages.

## How It Works

In the `pyproject.toml` file, you'll notice the dependency is defined as:

```toml
[tool.poetry.dependencies]
habushu-poetry-package = {path = "../habushu-poetry-package", develop = true}
```

This configuration:

1. Points to the local path of the dependency package
2. Uses `develop = true` to create an editable installation
3. Allows Habushu to manage the versioning automatically

When you build or publish:
- For development: Habushu maintains local references
- For release: Habushu properly resolves versions for distribution

## Habushu Integration

This project demonstrates several Habushu features: 

1. **dependency**: Includes another Habushu module as a build dependency
   ```xml
    <dependency>
        <groupId>${project.groupId}</groupId>
        <artifactId>habushu-uv-package</artifactId>
        <version>${project.version}</version>
        <type>habushu</type>
    </dependency>
   ```

2. **rewriteLocalPathDepsInArchives**: Rewrites any local path dependencies (to other projects with a `[project.version]` within the `pyproject.toml`) as versioned packaged dependencies in generated `wheel` archives.
   ```xml
    <plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <configuration>
        <rewriteLocalPathDepsInArchives>true</rewriteLocalPathDepsInArchives>
    </configuration>
    </plugin>
   ```
   
