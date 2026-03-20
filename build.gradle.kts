import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    java
    application
    `maven-publish`
}

group   = "co.pandascore"
version = "1.1.0"

repositories { mavenCentral() }

dependencies {
    implementation("com.rabbitmq:amqp-client:5.20.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.slf4j:slf4j-api:2.0.13")
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
                url         = "https://github.com/PandaScore/pandascore-sdk-java"

                licenses {
                    license {
                        name = "Proprietary"
                        url  = "https://github.com/PandaScore/pandascore-sdk-java"
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
                    connection          = "scm:git:git://github.com/PandaScore/pandascore-sdk-java.git"
                    developerConnection = "scm:git:ssh://github.com/PandaScore/pandascore-sdk-java.git"
                    url                 = "https://github.com/PandaScore/pandascore-sdk-java"
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            // Override with GITHUB_REPOSITORY env var for testing on forks
            // e.g. GITHUB_REPOSITORY=PandaScore/pandascore-sdk-java
            // Defaults to PandaScore/pandascore-sdk-java for production
            val repo = providers.environmentVariable("GITHUB_REPOSITORY")
                .orElse("PandaScore/pandascore-sdk-java")
                .get()
            url  = uri("https://maven.pkg.github.com/$repo")
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
