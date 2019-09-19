import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))

  implementation("com.graphql-java:graphql-java:12.0")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation("org.mockito:mockito-core:${Version.mockito}")
  testImplementation("io.jsonwebtoken:jjwt-api:${Version.jjwt}")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:${Version.jjwt}")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:${Version.jjwt}")
}

apply(from = "../gradle/jacoco.gradle")