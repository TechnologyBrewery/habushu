[[Return to Examples Documentation]](../README.md)

# Running Python Scripts

In addition to creating a custom Maven lifecycle that automates the execution of a predictable Poetry/uv-based 
workflow, Habushu exposes a [run-command-in-virtual-env](../../docs/HABUSHU_LIFECYCLE_README.md#run-command-in-virtual-env) plugin goal that provides developers with the ability to 
execute any Python command or script within the project's virtual environment through `poetry run`/`uv run` during the desired build phase.

In this example, we use this feature to bind a Habushu module's [compile](../../docs/HABUSHU_LIFECYCLE_README.md#compile) phase to the appropriate Python 
command that generates gRPC/protobuf bindings as an automated part of the build following dependency installation:

```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
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

## Build Confirmation
When building the project with `mvnd clean install -pl :habushu-running-python-scripts`, you should the following lines in the log indicating that the `runCommandArgs` command ran successfully.
```bash
[INFO] Executing command in virtual environment via 'run'...
[INFO] Executing poetry command: poetry run python -m grpc_tools.protoc -I=src --python_out=src/habushu_running_python_scripts/generated src/person.proto
```