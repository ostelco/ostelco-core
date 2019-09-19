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
  val jaxbVersion:String by rootProject.extra
  val javaxActivationVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val junit5Version:String by rootProject.extra

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation (project(":ocs-grpc-api"))
  implementation (project(":analytics-grpc-api"))

  implementation (project(":diameter-stack"))
  implementation (project(":diameter-ha"))

  implementation("com.google.cloud:google-cloud-pubsub:$googleCloudVersion")
  implementation("com.google.cloud:google-cloud-core-grpc:$googleCloudVersion")
  implementation("com.google.cloud:google-cloud-storage:$googleCloudVersion")

  implementation("javax.xml.bind:jaxb-api:$jaxbVersion")
  runtimeOnly("javax.activation:activation:$javaxActivationVersion")

  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
  implementation("ch.qos.logback:logback-classic:1.2.3")
  implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
  implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")

  compile("com.google.cloud:google-cloud-logging-logback:0.97.0-alpha")

  testImplementation(project(":diameter-test"))
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")

  testImplementation("junit:junit:4.12")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$junit5Version")
}

application {
  mainClassName = "org.ostelco.ocsgw.OcsApplication"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

version = "1.1.27"

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