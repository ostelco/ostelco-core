import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))

  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")

  implementation("com.google.zxing:core:${Version.zxing}")
  implementation("com.google.zxing:javase:${Version.zxing}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")

  testImplementation("org.junit.jupiter:junit-jupiter-api:${Version.junit5}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.junit5}")
}

tasks.test {

  val mandrillApiKey: Any? by project
  mandrillApiKey?.also { environment("MANDRILL_API_KEY", it) }

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