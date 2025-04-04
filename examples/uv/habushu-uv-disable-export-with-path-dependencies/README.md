[[Return to Main Documentation]](../../../README.md)

# habushu-export-requriements-with-path-dependencies
This example demonstrates how to disable the default exportRequirementsWithoutPathDependencies configuration in a uv project.

## Configuration 
By default, the `exportRequirementsWithoutPathDependencies` configuration is set to `true`, which excludes locally pathed dependencies from the requirements.txt export file. Disabling this feature allows locally pathed dependencies to be included in the requirements.txt export file. 
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.technologybrewery.habushu</groupId>
            <artifactId>habushu-maven-plugin</artifactId>
            <configuration>
                <exportRequirementsWithoutPathDependencies>false</exportRequirementsWithoutPathDependencies>
            </configuration>
        </plugin>
    </plugins>
</build>
```