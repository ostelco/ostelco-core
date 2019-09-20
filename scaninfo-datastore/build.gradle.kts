import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))
  implementation(project(":data-store"))
  
  implementation("com.fasterxml.jackson.core:jackson-databind:${Version.jacksonDatabind}")

  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")
  implementation("com.google.cloud:google-cloud-datastore:${Version.googleCloudDataStore}")
  implementation("com.google.cloud:google-cloud-storage:${Version.googleCloudStorage}")
  implementation("com.google.crypto.tink:tink:${Version.tink}")

  testImplementation("com.palantir.docker.compose:docker-compose-rule-junit4:${Version.dockerComposeJunitRule}")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))

  testImplementation(project(":secure-archive"))
  
  testImplementation("org.mockito:mockito-core:${Version.mockito}")
}

apply(from = "../gradle/jacoco.gradle")