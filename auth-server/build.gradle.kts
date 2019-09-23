import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
  idea
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation("io.dropwizard:dropwizard-core:${Version.dropwizard}")

  implementation(project(":firebase-extensions"))
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}") {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
  }

  runtimeOnly("javax.xml.bind:jaxb-api:${Version.jaxb}")
  runtimeOnly("javax.activation:activation:${Version.javaxActivation}")
  
  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation(kotlin("test-junit"))
  testRuntimeOnly("org.hamcrest:hamcrest-all:1.3")
}

application {
  mainClassName = "org.ostelco.auth.AuthServerApplicationKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

sourceSets.create("integration") {
  java.srcDirs("src/integration-tests/kotlin")
  resources.srcDirs("src/integration-tests/resources")
  compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
  runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

configurations.named("integrationImplementation") {
  extendsFrom(configurations["implementation"])
  extendsFrom(configurations["runtime"])
  extendsFrom(configurations["runtimeOnly"])
  extendsFrom(configurations["testImplementation"])
}

val integration = tasks.create("integration", Test::class.java) {
  description = "Runs the integration tests."
  group = "Verification"
  testClassesDirs = sourceSets.getByName("integration").output.classesDirs
  classpath = sourceSets.getByName("integration").runtimeClasspath
}

tasks.build.get().dependsOn(integration)
integration.mustRunAfter(tasks.test)

apply(from = "../gradle/jacoco.gradle")

idea {
  module {
    testSourceDirs.add(File("src/integration-tests/kotlin"))
  }
}