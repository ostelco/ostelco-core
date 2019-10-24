import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
}

apply(from = "../../gradle/jacoco.gradle.kts")