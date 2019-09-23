import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))
  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation("io.jsonwebtoken:jjwt-api:${Version.jjwt}")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:${Version.jjwt}")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:${Version.jjwt}")
}

apply(from = "../gradle/jacoco.gradle")