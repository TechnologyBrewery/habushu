[[Return to Main Documentation]](../../README.md)

#### managedDependencies ####

Optional set of dependencies to manage across modules extending a parent pom. This allows packages to be managed to a
specific version, which is often useful to ensure that information assurance patches, common versions, etc. are enforced
across a series of modules. Can be used with the next several variables to control automatic update, logging, or failing
the build when mismatches are found between the managed dependency operator/version and what is currently specified.
Looks at dependencies in `[tool.poetry.dependencies]`, `[tool.poetry.group.dev.dependencies]`, and any
`[tool.poetry.group.<subgroup>]` of your `pyproject.toml`.
```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    ...
    <configuration>
        <managedDependencies>
            <packageDefinition>
                <packageName>black</packageName>
                <operatorAndVersion>^23.3.0</operatorAndVersion>
                <!-- 
                  Active defaults to true, but can be used to overriden in child pom.xml files to remove or add 
                  managed dependencies at each level: 
                  -->
                <active>true</active>
            </packageDefinition>
            <packageDefinition>
                <packageName>some-local-module</packageName>
                <!-- 
                  Will follow rules in overridePackageVersion:compile to 
                  update to a final version of 1.2.3.* to resolve 1.2.3.dev versions 
                  UNLESS there is a ^ or Poetry version < 1.5.0
                  -->
                <operatorAndVersion>1.2.3-SNAPSHOT</operatorAndVersion>
            </packageDefinition>
        </managedDependencies>
        ...
    </configuration>
</plugin>
```

#### updateManagedDependenciesWhenFound ####

Determines if managed dependency mismatches are automatically updated when encountered.

Default: `true`

#### failOnManagedDependenciesMismatches ####

Determines if the build should be failed when managed dependency mismatches are found.

Default: `false`