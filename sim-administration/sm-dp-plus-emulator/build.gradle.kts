import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {
  implementation(project(":sim-administration:jersey-json-schema-validator"))
  implementation(project(":sim-administration:simcard-utils"))
  implementation(project(":sim-administration:es2plus4dropwizard"))
  implementation(project(":sim-administration:ostelco-dropwizard-utils"))

  implementation(kotlin("reflect"))
  implementation(kotlin("stdlib-jdk8"))

  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")

  implementation("io.dropwizard:dropwizard-core:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-auth:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-jdbi:${Version.dropwizard}")

  implementation("org.conscrypt:conscrypt-openjdk-uber:2.2.1")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}")
  implementation("org.apache.commons:commons-csv:${Version.csv}")

  runtimeOnly("javax.xml.bind:jaxb-api:${Version.jaxb}")
  runtimeOnly("javax.activation:activation:${Version.javaxActivation}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
}

application {
  mainClassName = "org.ostelco.simcards.smdpplus.SmDpPlusApplicationKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

apply(from = "../../gradle/jacoco.gradle")