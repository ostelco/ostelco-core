import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val dropwizardVersion:String by rootProject.extra
  val junit5Version:String by rootProject.extra

  implementation(project(":prime-modules"))

  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")

  testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
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