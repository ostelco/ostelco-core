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

  implementation(project(":ocs-grpc-api"))

  implementation(project(":diameter-stack"))
  implementation(project(":diameter-ha"))

  implementation("com.google.cloud:google-cloud-pubsub:${Version.googleCloudPubSub}")
  implementation("com.google.cloud:google-cloud-core-grpc:${Version.googleCloud}")
  implementation("com.google.cloud:google-cloud-storage:${Version.googleCloudStorage}")

  implementation("javax.xml.bind:jaxb-api:${Version.jaxb}")
  runtimeOnly("javax.activation:activation:${Version.javaxActivation}")

  implementation("com.fasterxml.jackson.core:jackson-databind:${Version.jacksonDatabind}")
  implementation("ch.qos.logback:logback-classic:1.2.3")
  implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
  implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")

  implementation("com.google.cloud:google-cloud-logging-logback:${Version.googleCloudLogging}")

  testImplementation(project(":diameter-test"))
  testImplementation("org.junit.jupiter:junit-jupiter-api:${Version.junit5}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.junit5}")

  testImplementation("junit:junit:4.12")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:${Version.junit5}")
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

apply(from = "../gradle/jacoco.gradle.kts")