[[Return to Main Documentation]](../../README.md)

#### defaultPythonStrategy ####

If the python version is not explicitly set in the `pythonVersion` configuration then `defaultPythonStrategy` is
used to determine where to get the default Python version. The default value for `defaultPythonStrategy` is
`PYTHONVERSION` which tells Habushu to use the existing projects Python version if it can be found. If the 
configuration is set to `POM` then Habushu will override the projects version with `pythonVersion` default value.
```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    ...
    <configuration>
        <!-- This will prevent the Habushu default python version from overriding the existing projects version -->
        <defaultPythonStrategy>PYTHONVERSION</defaultPythonStrategy>
        <!-- This will tell Habushu to overriding the existing projects with the default Python version -->
        <!--<defaultPythonStrategy>POM</defaultPythonStrategy>-->
    </configuration>
</plugin>
```
