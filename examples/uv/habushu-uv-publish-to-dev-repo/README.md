[[Return to Examples Documentation]](../../README.md)

# Publish Package to Development Repository

## Overview

This example demonstrates how to use Habushu to publish a package to a development repository, streamlining the deployment process for development and testing purposes. With Habushu, you can configure a package's build settings to automatically push to a private development repository, ensuring a smooth workflow for private deployments and iterative testing before release.

## Key Features

- **Development Repository Configuration**: Easily configure your `pom.xml` to publish directly to a private development repository.
- **Secure Credentials**: Leverages Maven's secure password encryption to safely store repository credentials.
- **Private Repository Usage**: Guides on setting up a private PyPI server to act as a development repository.
- **Flexible Configuration**: Includes options for enabling or disabling URL suffix additions for custom repositories.

## How It Works

The example involves configuring `pom.xml` and Maven settings to enable the publishing of packages to a private repository during development. The configuration includes:

1. Activate development repository publishing by setting the following parameters in the `pom.xml`:

```xml
<configuration>
    <useDevRepository>true</useDevRepository>
    <devRepositoryId>YOUR-REPO-NAME</devRepositoryId>
    <devRepositoryUrl>http://YOUR-URL-ADDERSS/</devRepositoryUrl>
    <!-- Set enableDevRepositoryUrlUploadSuffix to false if your devRepositoryUrl 
    does not have an upload suffix  -->
    <enableDevRepositoryUrlUploadSuffix>false</enableDevRepositoryUrlUploadSuffix>
</configuration>
```

2. Store credentials securely in Maven settings using an encrypted password:

```xml
<server>
    <id>YOUR-REPO-NAME</id>
    <username>YOUR-USERNAME</username>
    <password>{ENCRYPTED-PASSWORD}</password>
</server>
```

3. Build and deploy the package with the command:

```bash
mvn clean deploy
```

When the project is built, Habushu will push the packaged module to the configured development repository.

## Benefits

- **Simplified Development**: Quickly deploy packages to a development repository for private testing.
- **Secure Integration**: Ensures development repository credentials are securely managed.
- **Customizable Repository Settings**: Flexible settings allow easy integration with a variety of private repository setups.
- **Streamlined Workflow**: Enables rapid iteration during development by decoupling testing deployments from production releases.

### Example Setup
This section is required purely to execute this example. If you already have a private repository, you can skip this section.

#### Install Required Packages 
The `pypiserver` and `passlib` packages are required to execute this example.
```bash
pip install pypiserver passlib
```

#### Make the Private PyPi Directory
```bash
mkdir ~/private-pypi
```

#### Create the Username and Password File for `pypiserver`
```bash
htpasswd -c ~/.htpasswd myuser
# Set the password to whatever you want. e.g., `password`
```

#### Start the Private Server
In a separate terminal, start the private server and keep it running until you are completed with this example.
```bash
pypi-server run -p 8080 -a update,download,list --passwords ~/.htpasswd ~/private-pypi
````

### Save your Development Repository Credentials in Maven
Encrypt the password you chose by running `mvn --encrypt-password <your-password>`. For example, 

```bash
% mvn --encrypt-password password
{j5M3wbZdMlYHNEgrFNEPuDMN20jlMOkryKoKSjNzyc8=}
```
Then update your `~/.m2/settings.xml`.

```xml
<!-- ~/.m2/settings.xml -->
<server>
    <id>private-repo</id>
    <username>myuser</username>
    <password>{j5M3wbZdMlYHNEgrFNEPuDMN20jlMOkryKoKSjNzyc8=}</password>
</server>
```

### Update your pom.xml
In this example, the following configuration is located in the `deploy-example` profile. Outside the `habushu` repository, you can save this configuration in the `build` section of your project/module's `pom.xml` file.

```xml
 <configuration>
    <useDevRepository>true</useDevRepository>
    <devRepositoryId>private-repo</devRepositoryId>

    <!-- Leave off simple/ suffix to enable seamless upload and download capabilities -->
    <devRepositoryUrl>http://127.0.0.1:8080/</devRepositoryUrl>
    <enableDevRepositoryUrlUploadSuffix>false</enableDevRepositoryUrlUploadSuffix>
 </configuration>
```

### Build the project
To publish this package/module to the development repository, run  `mvn clean deploy -pl :habushu-uv-publish-to-dev-repo -Pdeploy-example` from the root directory.