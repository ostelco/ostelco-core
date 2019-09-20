import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation("io.dropwizard:dropwizard-logging:${Version.dropwizard}")

  implementation("com.fasterxml.jackson.core:jackson-annotations:${Version.jackson}")
  implementation("com.fasterxml.jackson.core:jackson-databind:${Version.jacksonDatabind}")
}

apply(from = "../gradle/jacoco.gradle")