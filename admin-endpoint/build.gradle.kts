import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))

  testImplementation(project(":jersey"))
  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
}

apply(from = "../gradle/jacoco.gradle.kts")