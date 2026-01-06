import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

plugins {
    java
    jacoco
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
    implementation("org.apache.commons:commons-lang3:3.20.0")
    testImplementation(platform("org.junit:junit-bom:6.0.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// jacoco
tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
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
                "https://central.sonatype.com/repository/maven-snapshots/"
            } else {
                "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
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

tasks.named("publish") {
    doLast {
        if (!isSnapshot && mavenCentralUsername != null && mavenCentralPassword != null) {
            uploadToPortal(mavenCentralUsername!!, mavenCentralPassword!!)
        }
    }
}

fun uploadToPortal(username: String, password: String) {
    val baseUrl = "https://ossrh-staging-api.central.sonatype.com"
    val credentials = Base64.getEncoder()
        .encodeToString("$username:$password".toByteArray())
    val client = HttpClient.newHttpClient()

    fun sendReq(
        url: String,
        method: String = "GET",
        body: String = ""
    ): java.net.http.HttpResponse<String> {
        val req = HttpRequest.newBuilder()
            .uri(URI(url))
            .header("Authorization", "Bearer $credentials")
            .header("Content-Type", "application/json")
            .method(method, if (body.isEmpty())
                HttpRequest.BodyPublishers.noBody()
            else HttpRequest.BodyPublishers.ofString(body))
            .build()
        val respBodyHandler = HttpResponse.BodyHandlers.ofString()
        return client.send(req, respBodyHandler)
    }

    try {
        // Search open repositories
        val searchResp = sendReq("$baseUrl/manual/search/repositories?state=open&ip=client")
        if (searchResp.statusCode() != 200) {
            println("Failed to search repositories, status code: ${searchResp.statusCode()}")
            return
        }

        // Extract and close repositories
        """"key"\s*:\s*"([^"]+)"""".toRegex()
            .findAll(searchResp.body())
            .forEach { match ->
                val repoKey = match.groupValues[1]
                sendReq("$baseUrl/manual/upload/repository/$repoKey?publishing_type=user_managed",
                    "POST", "{}")
                println("Upload staging repository: $repoKey")
            }
    } catch (e: Exception) {
        println("Failed to upload staging repositories: ${e.message}")
    }
}
