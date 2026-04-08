# PandaScore Java SDK — Test Repository

Test project to verify the PandaScore Java SDK works correctly when consumed as a Maven dependency from GitHub Packages.

## Prerequisites

- **Java 17** or higher (`java -version` to check)
- **Maven** (`mvn -version` to check)
- **GitHub account** with read access to the `PandaScore/pandascore-sdk-java` repository
- **GitHub Personal Access Token** with `read:packages` scope

## Setup (one-time)

### 1. Generate a GitHub Personal Access Token

1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Select the **`read:packages`** scope only
4. Copy the token

### 2. Configure Maven authentication

Create or edit `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>github-pandascore</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_PERSONAL_ACCESS_TOKEN</password>
        </server>
    </servers>
</settings>
```

Replace `YOUR_GITHUB_USERNAME` and `YOUR_PERSONAL_ACCESS_TOKEN` with your values.

> The `<id>` must be exactly `github-pandascore` — it matches the repository id in `pom.xml`.

## Clone and run

```bash
git clone https://github.com/PandaScore/pandascore-sdk-java-test.git
cd pandascore-sdk-java-test
```

### Compile (verify SDK dependency resolves)

```bash
mvn compile
```

If this succeeds, the SDK was downloaded from GitHub Packages and all dependencies are resolved.

### Run unit tests (verify SDK classes work)

```bash
mvn test
```

This runs tests that verify:
- SDK configuration builds correctly
- Event handler creates without errors
- FeedListener interface is accessible and works
- Connection events have correct codes (100/101)
- Recovery data tracks completeness
- All SDK model classes are importable

### Run the single-connection example

Edit `src/main/java/com/example/App.java` and replace the credentials with your PandaScore credentials, then:

```bash
mvn compile exec:java -Dexec.mainClass="com.example.App"
```

### Run the multi-connection example

Edit `src/main/java/com/example/MultiConnectionApp.java` with your credentials, then:

```bash
mvn compile exec:java -Dexec.mainClass="com.example.MultiConnectionApp"
```

## Project structure

```
test-repo/
├── pom.xml                                          # Maven config with SDK dependency
├── README.md                                        # This file
├── src/
│   ├── main/java/com/example/
│   │   ├── App.java                                 # Single-connection example
│   │   └── MultiConnectionApp.java                  # Multi-connection example
│   └── test/java/com/example/
│       └── SDKIntegrationTest.java                  # Unit tests verifying SDK works
```

## Troubleshooting

| Problem | Fix |
|---|---|
| `Could not find artifact co.pandascore:pandascore-sdk-java` | Check `~/.m2/settings.xml` has correct credentials and the `<id>` is `github-pandascore` |
| `resolution is not reattempted` | Run `rm -rf ~/.m2/repository/co/pandascore` then `mvn compile -U` |
| `401 Unauthorized` | Your PAT is invalid or missing `read:packages` scope |
| `./gradlew: no such file` | This is a Maven project — use `mvn` commands, not `./gradlew` |
| Connection fails at runtime | Replace placeholder credentials in the example files with real PandaScore API credentials |

## Updating the SDK version

To test a newer SDK version, edit `pom.xml` and change the version property:

```xml
<pandascore.sdk.version>1.0.1</pandascore.sdk.version>
```

Then clear the cache and recompile:

```bash
rm -rf ~/.m2/repository/co/pandascore
mvn compile -U
```
