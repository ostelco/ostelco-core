import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation("io.dropwizard:dropwizard-core:${Version.dropwizard}")

  implementation("io.jsonwebtoken:jjwt-api:${Version.jjwt}")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:${Version.jjwt}")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Version.jjwt}")

  runtimeOnly("javax.xml.bind:jaxb-api:${Version.jaxb}")
  runtimeOnly("javax.activation:activation:${Version.javaxActivation}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
}

application {
  mainClassName = "org.ostelco.ext.authprovider.AuthProviderAppKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

apply(from = "../gradle/jacoco.gradle.kts")