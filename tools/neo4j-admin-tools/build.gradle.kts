import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation(project(":prime-modules"))
  implementation("org.neo4j.driver:neo4j-java-driver:${Version.neo4jDriver}")

}

application {
  mainClassName = "org.ostelco.tools.migration.MainKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}