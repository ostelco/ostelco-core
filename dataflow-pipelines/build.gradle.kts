import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val googleCloudVersion:String by rootProject.extra
  val beamVersion:String by rootProject.extra
  val junit5Version:String by rootProject.extra

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation(project(":analytics-grpc-api"))

  implementation("com.google.cloud:google-cloud-pubsub:$googleCloudVersion")

  implementation("org.apache.beam:beam-sdks-java-core:$beamVersion")
  implementation("org.apache.beam:beam-runners-google-cloud-dataflow-java:$beamVersion")

  implementation("ch.qos.logback:logback-classic:1.2.3")

  testRuntimeOnly("org.hamcrest:hamcrest-all:1.3")
  testRuntimeOnly("org.apache.beam:beam-runners-direct-java:$beamVersion")

  testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
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