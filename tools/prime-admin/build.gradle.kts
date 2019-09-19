import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {

  val kotlinVersion:String by rootProject.extra

  implementation(kotlin("stdlib-jdk8"))

  implementation(project(":prime"))
  implementation(project(":neo4j-store"))
  
  implementation("com.google.cloud.sql:postgres-socket-factory:1.0.15")

  implementation(kotlin("test"))
  implementation(kotlin("test-junit"))
}

application {
  mainClassName = "org.ostelco.tools.prime.admin.MainKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
  isZip64 = true
}