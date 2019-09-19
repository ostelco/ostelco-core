import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))

  implementation("com.google.cloud:google-cloud-pubsub:${Version.googleCloud}")

  testImplementation("com.palantir.docker.compose:docker-compose-rule-junit4:${Version.dockerComposeJunitRule}")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
  
  testImplementation("org.mockito:mockito-core:${Version.mockito}")

}

apply(from = "../gradle/jacoco.gradle")