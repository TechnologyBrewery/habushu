[[Return to Main Documentation]](../../../README.md)

# Habushu Poetry Package Consumer

## Overview

This package demonstrates how to effectively consume another Poetry package (`habushu-poetry-package`) from within the 
same monorepo structure using Habushu. It showcases Habushu's powerful capabilities for managing dependencies in a 
monorepo environment, with particular focus on seamless development workflows.

## Key Features

- **Implicit Version Handling**: Habushu automatically manages versioning between development and release versions, 
eliminating the need for manual version updates during development cycles.
- **Local Path References**: During development, the package uses local path references to the `habushu-poetry-package`, 
enabling real-time code changes to be immediately reflected without requiring reinstallation.
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

## Benefits

- **Simplified Development**: Make changes to `habushu-poetry-package` and immediately see the effects in this consumer 
package without re-installation.
- **Consistent Versioning**: Habushu handles version resolution automatically, ensuring consistency across the monorepo.
- **Seamless Transitions**: Easily switch between development and release modes without changing dependency configurations.
- **Reduced Maintenance**: No need to manually update version numbers across multiple packages during development.
