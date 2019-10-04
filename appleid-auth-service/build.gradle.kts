import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
  idea
}

dependencies {
  implementation(project(":prime-modules"))
  implementation(project(":firebase-extensions"))

  implementation("io.jsonwebtoken:jjwt-api:${Version.jjwt}")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:${Version.jjwt}")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Version.jjwt}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation(kotlin("test-junit"))
  testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}")
}

application {
  mainClassName = "org.ostelco.auth.AuthServerApplicationKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

apply(from = "../gradle/jacoco.gradle")
