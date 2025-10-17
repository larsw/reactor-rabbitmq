# GitHub Actions Workflows

This repository includes two GitHub Actions workflows for automated building, testing, and publishing.

## Workflows

### 1. Build and Test (`build-and-test.yml`)

**Triggers:**
- On every push to any branch
- On pull requests to `master` or `main` branches

**What it does:**
- Sets up Java 21 with Gradle caching
- Starts a RabbitMQ service container for integration tests
- Builds the project using Gradle
- Runs all tests
- Archives test results and build artifacts

**Requirements:**
- No special configuration needed
- Tests run against RabbitMQ 3.9 container

### 2. Publish to GitHub Packages (`publish-packages.yml`)

**Triggers:**
- Automatically when a GitHub release is created
- Manually via workflow dispatch (in the Actions tab)

**What it does:**
- Sets up Java 21 with Gradle caching
- Builds the project
- Publishes Maven artifacts to GitHub Packages

**Requirements:**
- No additional secrets needed - uses built-in `GITHUB_TOKEN`
- Requires `packages: write` permission (automatically granted)

## Publishing Packages

### Via Release

1. Create a new release in GitHub (Releases → Draft a new release)
2. Tag the release appropriately (e.g., `v1.5.7`)
3. Publish the release
4. The workflow will automatically build and publish packages

### Via Manual Dispatch

1. Go to Actions tab in GitHub
2. Select "Publish to GitHub Packages" workflow
3. Click "Run workflow"
4. Optionally specify a version (otherwise uses `gradle.properties`)
5. Click "Run workflow" button

## Using Published Packages

Once published, you can consume the packages from GitHub Packages:

### Maven

Add to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/larsw/reactor-rabbitmq</url>
    </repository>
</repositories>

<dependency>
    <groupId>io.projectreactor.rabbitmq</groupId>
    <artifactId>reactor-rabbitmq</artifactId>
    <version>1.5.7-SNAPSHOT</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/larsw/reactor-rabbitmq")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation 'io.projectreactor.rabbitmq:reactor-rabbitmq:1.5.7-SNAPSHOT'
}
```

### Authentication

To consume packages from GitHub Packages, you need to authenticate:

1. Create a Personal Access Token (PAT) with `read:packages` scope
2. Configure your build tool with your GitHub username and token

For Maven, add to `~/.m2/settings.xml`:

```xml
<servers>
    <server>
        <id>github</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>YOUR_GITHUB_TOKEN</password>
    </server>
</servers>
```

For Gradle, add to `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

## Local Publishing (Development)

For local development and testing, you can publish to Maven Local:

```bash
./gradlew publishToMavenLocal
```

This publishes to `~/.m2/repository` without requiring any authentication.
