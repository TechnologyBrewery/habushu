### Leveraging the containerize-dependencies Goal to Prepare a Containerized Virtual Environment ###
The `containerize-dependencies` goal will collect a single `habushu` dependency specified in the project's `pom.xml`,
including all transitive habushu-packaged dependencies. After collecting the set of necessary dependencies, Habushu will
copy the project files for each dependency to a staging directory, while preserving the original structure of the
dependency modules to ensure that any path-based dependencies can be leveraged as-is. This directory can then be copied
onto a Docker container and used to create a virtual environment capable of running the target Habushu project.

The plugin will automatically inject logic for building and using this virtual environment into a pre-existing
Dockerfile by default. The `dockerfile` configuration must be set to the target Dockerfile. To disable the Dockerfile
update altogether, set the `updateDockerfile` configuration to `false`. Because the virtual environment that is created
is dependent on the platform for which it is built, Habushu defaults to using `python:3.12` to build the virtual
environment and  `python:3.12-slim` as the final image that packages/runs the virtual environment. This can be
customized with the `dockerBuilderBase`, `dockerFinalBase`, `dockerUser`, `dockerPoetryVersion`, `dockerPoetryPluginBundleVersion`, and `dockerPoetryMonorepoDependencyPluginVersion` configurations, but care must be taken to
ensure the builder image platform is sufficiently similar to the final image platform so that the virtual environment is
compatible.

```xml
<plugin>
    <groupId>org.technologybrewery.habushu</groupId>
    <artifactId>habushu-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>containerize-deps</id>
            <goals>
                <goal>containerize-dependencies</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <dockerfile>${project.basedir}/src/main/docker/Dockerfile</dockerfile>
    </configuration>
</plugin>
```


To control the exact insertion location of the Dockerfile build logic, use the `#HABUSHU_BUILDER_STAGE`and
`#HABUSHU_FINAL_STAGE` comment tags in the `Dockerfile` in the preferred location. The `#HABUSHU_BUILDER_STAGE` tag will
be replaced with the builder stage logic to copy the generated dependencies files onto a Docker container and then create
the virtual environment by building the target Habushu project. The `#HABUSHU_FINAL_STAGE` tag will be replaced with the
final stage logic to copy over the built virtual environment to the final Docker build stage.
```dockerfile
#HABUSHU_BUILDER_STAGE

FROM redhat/ubi9-minimal:latest AS builder
RUN microdnf install -y python3.11

#HABUSHU_FINAL_STAGE

ENTRYPOINT ["/opt/venv/bin/python3.11", "-m", "pets.main", "--enable_docs_url", "True"]
```

The plugin will only examine dependencies that are of the type `habushu` in the dependencies block of the `pom.xml` file.
```xml
<dependencies>
    <dependency>
        <groupId>your-group-id</groupId>
        <artifactId>your-artifact-id</artifactId>
        <version>your-version</version>
        <type>habushu</type>
    </dependency>
</dependencies>
```
Any transitive monorepo dependencies should also use this convention to ensure they are captured by the plugin.

