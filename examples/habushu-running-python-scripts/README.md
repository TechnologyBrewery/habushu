[[Return to Main Documentation]](../../README.md)

### Running Custom Python Scripts During Build Phases ###

In addition to creating a custom Maven lifecycle that automates the execution of a predictable Poetry/uv-based 
workflow, Habushu exposes a `run-command-in-virtual-env` plugin goal that provides developers with the ability to 
execute any Python command or script within the project's virtual environment through `poetry run`/`uv run` during 
the desired build phase.

For example, developers may use this feature to bind a Habushu module's `compile` phase to the appropriate Python 
command that generates gRPC/protobuf bindings as an automated part of the build following dependency installation:

```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <extensions>true</extensions>
    <configuration>
        ...
    </configuration>
    <executions>
        <execution>
            <configuration>
                <runCommandArgs>python -m grpc_tools.protoc -I=src
                    --python_out=src/habushu_poetry_package/generated src/person.proto</runCommandArgs>
            </configuration>
            <id>generate-protobuf-bindings</id>
            <phase>compile</phase>
            <goals>
                <goal>run-command-in-virtual-env</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### runCommandArgs ####

**Only applicable when executing the `run-command-in-virtual-env` plugin goal.**

Whitespace-delimited command arguments that will be provided to `run` to execute.

Default: None
