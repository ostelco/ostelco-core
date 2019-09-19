import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))
  implementation("com.fasterxml.jackson.core:jackson-databind:${Version.jackson}")

  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")
  
  api("com.google.cloud:google-cloud-core:${Version.googleCloud}")
  implementation("com.google.cloud:google-cloud-datastore:${Version.googleCloud}")

  testImplementation("com.palantir.docker.compose:docker-compose-rule-junit4:${Version.dockerComposeJunitRule}")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))

  testImplementation("org.mockito:mockito-core:${Version.mockito}")
}

apply(from = "../gradle/jacoco.gradle")