import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))

  implementation("io.opencensus:opencensus-api:${Version.opencensus}")
  runtimeOnly("io.opencensus:opencensus-impl:${Version.opencensus}")
  implementation("io.opencensus:opencensus-exporter-trace-stackdriver:${Version.opencensus}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")

  testImplementation("org.junit.jupiter:junit-jupiter-api:${Version.junit5}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.junit5}")
}

tasks.test {

  // native support to Junit5 in Gradle 4.6+
  useJUnitPlatform {
    includeEngines("junit-jupiter")
  }
  testLogging {
    exceptionFormat = FULL
    events("PASSED", "FAILED", "SKIPPED")
  }
}

apply(from = "../gradle/jacoco.gradle")