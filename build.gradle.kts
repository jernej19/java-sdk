import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    `java-library`
    application
    `maven-publish`
}

group   = "co.pandascore"
version = "1.0.0"

// Derive the GitHub repository slug ("owner/repo") for publishing.
// In CI, GITHUB_REPOSITORY is set automatically by GitHub Actions.
// Locally, fall back to the canonical PandaScore repo. The slug must be
// lowercased because GitHub Packages Maven URLs are case-sensitive and
// will reject mixed-case owner/repo paths with HTTP 422.
val githubRepoSlug: String =
    (System.getenv("GITHUB_REPOSITORY") ?: "PandaScore/pandascore-sdk-java").lowercase()

repositories { mavenCentral() }

dependencies {
    api("com.rabbitmq:amqp-client:5.20.0")
    api("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("com.squareup.okhttp3:logging-interceptor:4.12.0")
    api("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

application {
    mainClass.set("com.pandascore.sdk.examples.BasicExample")
}

java {
    withSourcesJar()   // customers can navigate to SDK source in their IDE
    withJavadocJar()   // replaces the manual javadocJar task below
}

tasks.test {
    useJUnitPlatform()
}

tasks.javadoc {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}

// The manual javadocJar task is superseded by java { withJavadocJar() } above.
// Keeping it commented out to avoid duplicate archive registration.
// tasks.register<Jar>("javadocJar") { ... }

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name        = "PandaScore Java SDK"
                description = "Real-time esports betting data feed client for PandaScore AMQPS streams"
                url         = "https://github.com/$githubRepoSlug"

                licenses {
                    license {
                        name = "Proprietary"
                        url  = "https://github.com/$githubRepoSlug"
                    }
                }

                developers {
                    developer {
                        id           = "pandascore"
                        name         = "PandaScore"
                        organization = "PandaScore"
                    }
                }

                scm {
                    connection          = "scm:git:git://github.com/$githubRepoSlug.git"
                    developerConnection = "scm:git:ssh://github.com/$githubRepoSlug.git"
                    url                 = "https://github.com/$githubRepoSlug"
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            // Publish to the GitHub repository this build is running in.
            // In GitHub Actions, GITHUB_REPOSITORY is set automatically (e.g. "jernej19/java-sdk").
            // For local publishes, set GITHUB_REPOSITORY in your environment or use publishToMavenLocal.
            // The slug is lowercased at the top of this file — GitHub Packages Maven URLs are
            // case-sensitive and reject mixed-case owner/repo paths with HTTP 422.
            url  = uri("https://maven.pkg.github.com/$githubRepoSlug")
            credentials {
                // Set GITHUB_ACTOR and GITHUB_TOKEN in your environment,
                // or add them to ~/.gradle/gradle.properties as:
                //   githubUser=YOUR_USERNAME
                //   githubToken=YOUR_TOKEN
                username = providers.environmentVariable("GITHUB_ACTOR")
                    .orElse(providers.gradleProperty("githubUser"))
                    .orNull
                password = providers.environmentVariable("GITHUB_TOKEN")
                    .orElse(providers.gradleProperty("githubToken"))
                    .orNull
            }
        }
    }
}
