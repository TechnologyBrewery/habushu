[[Return to Main Documentation]](../../../README.md)

#### uv Enforcer Rule ####

Projects will often want to norm on a specific version or version range. The Maven Enforcer is a well established 
approach to ensuring minimum environmental constraints are met during builds. Rather than build our own approach, we 
have created Maven Enforcer Rule that can be applied to your project for uv versions.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>enforce-uv</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <requireUvVersion>
                        <!-- 
                            NOTE: you will need to XML encode if your leading character is a less than sign:
                                Example: <=2.1.1. becomes &lt;=2.1.1
                         -->
                        <version>>=0.6.0</version>
                    </requireUvVersion>
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

#### requireUvVersion ####

The **REQUIRED** rule name to use with the Maven Enforcer Plugin to validate uv versions.

#### version ####

The **REQUIRED** version or version range uv must have to pass the Enforcer check. 