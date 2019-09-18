import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val opencensusVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val junit5Version:String by rootProject.extra

  implementation(project(":prime-modules"))

  implementation("io.opencensus:opencensus-api:$opencensusVersion")
  runtimeOnly("io.opencensus:opencensus-impl:$opencensusVersion")
  implementation("io.opencensus:opencensus-exporter-trace-stackdriver:$opencensusVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")

  testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
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