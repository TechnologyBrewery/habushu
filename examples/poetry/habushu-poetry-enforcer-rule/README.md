[[Return to Examples Documentation]](../../README.md)

# Poetry Version Enforcement

Habushu supports a broad range of Poetry versions, however, projects will often want to norm on a specific version or
version range. For instance, 2.x `pyproject.toml` formats are generally not compatible for versions of Poetry less
than 2.x. The Maven Enforcer is a well established approach to ensuring minimum environmental constraints are met
during builds. Rather than build our own approach, we have created Maven Enforcer Rule, [requirePoetryVersion](../../../docs/CONFIGURATION_README.md#requirepoetryversion), that can be applied to your
project for Poetry versions.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>enforce-poetry</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <requirePoetryVersion>
                        <!-- 
                            NOTE: you will need to XML encode if your leading character is a less than sign:
                                Example: <=2.1.1. becomes &lt;=2.1.1
                         -->
                        <version>>=2.0.0</version>
                    </requirePoetryVersion>
                </rules>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>org.technologybrewery.habushu</groupId>
            <artifactId>habushu-maven-plugin</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</plugin>
```