import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val neo4jDriverVersion:String by rootProject.extra

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation(project(":prime-modules"))
  implementation("org.neo4j.driver:neo4j-java-driver:$neo4jDriverVersion")

}

application {
  mainClassName = "org.ostelco.tools.migration.MainKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}