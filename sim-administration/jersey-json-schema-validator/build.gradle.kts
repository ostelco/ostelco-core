import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation("io.dropwizard:dropwizard-core:${Version.dropwizard}")
  implementation("com.github.everit-org.json-schema:org.everit.json.schema:1.12.0")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
}

apply(from = "../../gradle/jacoco.gradle.kts")