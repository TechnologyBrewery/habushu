[[Return to Examples Documentation]](../README.md)

# Containerizing a Python Application

This example leverages the [containerize-dependencies](../../docs/HABUSHU_LIFECYCLE_README.md#containerize-dependencies)
goal to package a Python application (including monorepo dependencies) in a Docker container. The default configuration
shows the basic options for controlling the Docker build. There is also a `custom-docker-template` Maven profile that
shows more advanced customization of the Dockerfile logic using [Apache Velocity](https://velocity.apache.org) templates.
Because the generated Dockerfile logic for building the virtual environment is always regenerated and re-inserted,
switching from the default Dockerfile to the custom template example is as simple as running:

```shell
mvn clean install -pl :habushu-containerize -Pcustom-docker-template
```

See the [configuration documentation](../../docs/CONFIGURATION_README.md#containerization-configurations) for other
customization options.
