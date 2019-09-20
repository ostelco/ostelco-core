import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation(project(":analytics-grpc-api"))

  implementation("com.google.cloud:google-cloud-pubsub:${Version.googleCloudPubSub}")

  implementation("org.apache.beam:beam-sdks-java-core:${Version.beam}")
  implementation("org.apache.beam:beam-runners-google-cloud-dataflow-java:${Version.beam}")

  implementation("ch.qos.logback:logback-classic:1.2.3")

  testRuntimeOnly("org.hamcrest:hamcrest-all:1.3")
  testRuntimeOnly("org.apache.beam:beam-runners-direct-java:${Version.beam}")

  testImplementation("org.junit.jupiter:junit-jupiter-api:${Version.junit5}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.junit5}")
}

application {
  mainClassName = "org.ostelco.dataflow.pipelines.DeployPipelineKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
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