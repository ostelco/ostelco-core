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
  implementation("com.google.cloud:google-cloud-datastore:${Version.googleCloudDataStore}")

  api("io.arrow-kt:arrow-core-data:${Version.arrow}")
  api("io.arrow-kt:arrow-core-extensions:${Version.arrow}")
  api("io.arrow-kt:arrow-typeclasses:${Version.arrow}")
  api("io.arrow-kt:arrow-extras-data:${Version.arrow}")
  api("io.arrow-kt:arrow-extras-extensions:${Version.arrow}")
  api("io.arrow-kt:arrow-effects-data:${Version.arrow}")
  api("io.arrow-kt:arrow-effects-extensions:${Version.arrow}")
  api("io.arrow-kt:arrow-effects-io-extensions:${Version.arrow}")

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

tasks.register("version") {
  doLast {
    println(version)
  }
}

apply(from = "../gradle/jacoco.gradle.kts")
