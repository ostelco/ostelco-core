import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("test").toString()) {
    exclude(module = "kotlin-stdlib-common")
  }

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinXCoroutines}")

  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")
  implementation(project(":prime-customer-api"))
  implementation(project(":diameter-test"))
  implementation(project(":ocs-grpc-api"))

  implementation("com.google.cloud:google-cloud-pubsub:${Version.googleCloud}")

  implementation("com.stripe:stripe-java:${Version.stripe}")

  implementation("io.jsonwebtoken:jjwt-api:${Version.jjwt}")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:${Version.jjwt}")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Version.jjwt}")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}")
  implementation("org.zalando.phrs:jersey-media-json-gson:0.1")

  implementation(kotlin("test"))
  implementation(kotlin("test-junit"))

  implementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
}

application {
  mainClassName = ""
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}