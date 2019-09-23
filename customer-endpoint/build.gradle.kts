import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation("org.mockito:mockito-core:${Version.mockito}")
  testImplementation("org.assertj:assertj-core:${Version.assertJ}")

  testImplementation("io.jsonwebtoken:jjwt-api:${Version.jjwt}")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:${Version.jjwt}")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:${Version.jjwt}")

  testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
}

apply(from = "../gradle/jacoco.gradle")