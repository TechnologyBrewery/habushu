[[Return to Main Documentation]](../../README.md)

# add-habushu-to-new-or-existing-uv-project
This example demonstrates how to add Habushu to a new or existing uv project.

## Setting up the Project
### Instantiate a New uv Project
To generate this project, we used the `uv init --lib` command:

```sh
$ uv init add-habushu-to-new-or-existing-uv-project --lib
Initialized project `add-habushu-to-new-or-existing-uv-project` at `habushu/examples/add-habushu-to-new-or-existing-uv-project`
$ tree add-habushu-to-new-or-existing-poetry-project
├── README.md
├── pyproject.toml
└── src
    └── add_habushu_to_new_or_existing_uv_project
        ├── __init__.py
        ├── helloworld.py
        └── py.typed
```

### Instantiate uv in an Existing Project
To migrate an existing Python package, consider using `uv init`.

## Adding Habushu to a uv Project
To add Habushu to your project, create a `pom.xml` file in the top level of the project. Then, follow the steps outline in [Integrating Your Poetry/uv Project with Habushu and Maven](../../README.md#integrating-your-poetryuv-project-with-habushu-and-maven-).

