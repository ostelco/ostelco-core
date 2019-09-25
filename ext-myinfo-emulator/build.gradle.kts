import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  // This is not a prime-module. Just needed access to getLogger and Dropwizard KotlinModule.
  implementation(project(":prime-modules"))

  implementation("io.dropwizard:dropwizard-core:${Version.dropwizard}")

  implementation("io.jsonwebtoken:jjwt-api:${Version.jjwt}")
  implementation("io.jsonwebtoken:jjwt-impl:${Version.jjwt}")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Version.jjwt}")

  implementation("org.apache.cxf:cxf-rt-rs-security-jose:${Version.cxf}")

  runtimeOnly("javax.xml.bind:jaxb-api:${Version.jaxb}")
  runtimeOnly("javax.activation:activation:${Version.javaxActivation}")
}

application {
  mainClassName = "org.ostelco.ext.myinfo.MyInfoEmulatorAppKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

apply(from = "../gradle/jacoco.gradle.kts")