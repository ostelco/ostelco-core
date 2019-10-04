import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))

  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")

  testImplementation("org.junit.jupiter:junit-jupiter-api:${Version.junit5}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.junit5}")
}

tasks.test {

  val slackWebHookUri: Any? by project
  slackWebHookUri?.also { environment("SLACK_WEBHOOK_URI", it) }

  val slackChannel: Any? by project
  slackChannel?.also { environment("SLACK_CHANNEL", it) }

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