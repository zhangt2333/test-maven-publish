plugins {
    java
    `maven-publish`
    signing
}

fun Project.getProperty(key: String) =
    providers.gradleProperty(key).get()

val Project.projectGroupId: String
    get() = getProperty("projectGroupId")

val Project.projectArtifactId: String
    get() = getProperty("projectArtifactId")

val Project.projectVersion: String
    get() = getProperty("projectVersion")

val Project.projectUrl: String
    get() = getProperty("projectUrl")

val Project.projectDescription: String
    get() = getProperty("projectDescription")

val Project.isSnapshot: Boolean
    get() = projectVersion.endsWith("-SNAPSHOT")


group = projectGroupId
description = projectArtifactId
version = projectVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons:commons-lang3:3.17.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
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
            groupId = projectGroupId
            version = projectVersion
            artifactId = projectArtifactId
            pom {
                name.set(projectArtifactId)
                description.set(projectDescription)
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
