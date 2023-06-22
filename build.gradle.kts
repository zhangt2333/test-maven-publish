plugins {
    java
    `maven-publish`
    signing
}

group = "io.github.zhangt2333"
description = "test-maven-publish"
version = "0.0.3-SNAPSHOT"
val projectUrl = "https://github.com/zhangt2333/test-maven-publish"
val isSnapshot = version.toString().endsWith("-SNAPSHOT")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons:commons-lang3:3.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.test {
    useJUnitPlatform()
}

java {
    withJavadocJar()
    withSourcesJar()
}

val signingKeyId: String? by project         // env.ORG_GRADLE_PROJECT_signingKeyId
val signingKey: String? by project           // env.ORG_GRADLE_PROJECT_signingKey
val signingPassword: String? by project      // env.ORG_GRADLE_PROJECT_signingPassword
val mavenCentralUsername: String? by project // env.ORG_GRADLE_PROJECT_mavenCentralUsername
val mavenCentralPassword: String? by project // env.ORG_GRADLE_PROJECT_mavenCentralPassword

publishing {
    if (mavenCentralUsername != null && mavenCentralPassword != null) {
        repositories {
            val repoUrl = if (isSnapshot) {
                "https://s01.oss.sonatype.org/content/repositories/snapshots"
            } else {
                "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2"
            }
            maven {
                name = "Sonatype"
                url = uri(repoUrl)
                credentials {
                    username = mavenCentralUsername
                    password = mavenCentralPassword
                }
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            // components
            from(components["java"])
            // https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:resolved_dependencies
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            // metadata
            pom {
                name.set(project.description)
                description.set("Just for testing maven publish")
                url.set(projectUrl)
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("zhangt2333")
                        name.set("Teng Zhang")
                        email.set("35210901+zhangt2333@users.noreply.github.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://${projectUrl.removePrefix("https://")}.git")
                    developerConnection.set("scm:git:ssh://${projectUrl.removePrefix("https://")}.git")
                    url.set(projectUrl)
                }
            }
        }
    }
}

signing {
    isRequired = !isSnapshot // Gradle Module Metadata currently does not support signing snapshots
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}
