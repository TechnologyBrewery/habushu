[[Return to Examples Documentation]](../../README.md)

# Pyenv Version Enforcement

Projects will often want to norm on a specific version or version range. The Maven Enforcer is a well established 
approach to ensuring minimum environmental constraints are met during builds. Rather than build our own approach, we 
have created the Maven Enforcer Rule, [requirePyenvVersion](../../../docs/CONFIGURATION_README.md#requirepyenvversion) that can be applied to your project for pyenv versions.

## Build the project

Run `mvn clean install -Ppyenv-enforcer-example` from this module or run `mvn clean install -pl :habushu-pyenv-enforcer-rule -Ppyenv-enforcer-example` from the root directory to build this module.

## Configuration

The `pom.xml` file includes the `requirePyenvVersion` rule in the Maven Enforcer Plugin configuration:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>enforce-pyenv</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <requirePyenvVersion>
                        <!-- 
                            NOTE: you will need to XML encode if your leading character is a less than sign:
                                Example: <=2.1.1. becomes &lt;=2.1.1
                         -->
                        <version>>=1.2.21</version>
                    </requirePyenvVersion>
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