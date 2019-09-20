import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val dropwizardVersion:String by rootProject.extra
  val zxingVersion:String by rootProject.extra
  val junit5Version:String by rootProject.extra

  implementation(project(":prime-modules"))

  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")

  implementation("com.google.zxing:core:$zxingVersion")
  implementation("com.google.zxing:javase:$zxingVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")

  testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
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