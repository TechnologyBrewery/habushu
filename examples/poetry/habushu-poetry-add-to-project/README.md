[[Return to Main Documentation]](../../../README.md)

# habushu-poetry-add-to-project
This example demonstrates how to add Habushu to a new or existing Poetry project.

## Setting up the Project
### Instantiate a New Poetry Project 
To generate this project, we used the `poetry new --src` command:

```sh
$ poetry new habushu-poetry-add-to-project --src 
Created package add_habushu_to_new_or_existing_poetry_project in habushu-poetry-add-to-project
$ tree habushu-poetry-add-to-project
├── README.md
├── poetry.toml
├── pyproject.toml
├── src
│   └── add_habushu_to_new_or_existing_poetry_project
│       └── __init__.py
└── tests
    └── __init__.py
```
**NOTE:** The above includes an optional `poetry.toml` which includes additional configuration settings for Poetry. This file is not required but should be included in version control to ensure consistent builds.

### Instantiate Poetry in an Existing Project
To migrate an existing Python package, consider using `poetry init` and use the interactive guide to create the desired `pyproject.toml` configuration with the appropriate dependencies.

If migrating an earlier release of Habushu, follow the same process, but note the following required changes:

* Dependencies specified in `requirements.txt` must be specified in `pyproject.toml` - either use `poetry add` or add them interactively via `poetry init`
* Python source and test files must be migrated into the folder structure described above, which aligns with the standard `src/` packaging layout.  Assuming that the package name is `habushu_poetry_simple_package`, `src/main/python/*` from the existing Habushu project must be moved into `src/habushu_poetry_simple_package` and `src/test/python/*` from the existing Habushu project must be moved into `tests`
* Previously, Habushu 1.x modules depended on each other via Maven `<dependency>` declarations.  This approach is deprecated as Habushu 2.x+ expects that other Habushu modules are published to PyPI repositories and consumed as Python packages using Poetry's built-in dependency management capabilties.  For Habushu module dependencies within the same Maven multi-module build hierarchy, consider using editable development installs:

    ```toml
    # pyproject.toml
    [tool.poetry.dependencies]
    my-package = {path = "../habushu-poetry-simple-package", develop = true}
    ```

## Adding Habushu to a Poetry Project
To add Habushu to your project, create a `pom.xml` file in the top level of the project. Then, follow the steps outline in [Integrating Your Poetry/uv Project with Habushu and Maven](../../../README.md#integrating-your-poetryuv-project-with-habushu-and-maven-).

