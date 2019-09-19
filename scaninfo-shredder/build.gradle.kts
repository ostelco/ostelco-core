import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

version = "1.0.0"

dependencies {
  api(project(":model"))
  
  implementation(project(":data-store"))

  implementation("io.dropwizard:dropwizard-core:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinXCoroutines}")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}")

  implementation(kotlin("stdlib-jdk8"))
  implementation("com.google.cloud:google-cloud-datastore:${Version.googleCloud}")

  implementation("io.arrow-kt:arrow-core:${Version.arrow}")
  implementation("io.arrow-kt:arrow-typeclasses:${Version.arrow}")
  implementation("io.arrow-kt:arrow-instances-core:${Version.arrow}")
  implementation("io.arrow-kt:arrow-effects:${Version.arrow}")
  
  runtimeOnly("io.dropwizard:dropwizard-json-logging:${Version.dropwizard}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation("org.mockito:mockito-core:${Version.mockito}")
  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))

}

application {
  mainClassName = "org.ostelco.storage.scaninfo.shredder.ScanInfoShredderApplicationKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

apply(from = "../gradle/jacoco.gradle")