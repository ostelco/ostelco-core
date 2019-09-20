import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
  idea
}

dependencies {
  val jjwtVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra

  implementation(project(":prime-modules"))
  implementation(project(":firebase-extensions"))

  implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation(kotlin("test-junit"))
  testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
}

application {
  mainClassName = "org.ostelco.auth.AuthServerApplicationKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

apply(from = "../gradle/jacoco.gradle")
