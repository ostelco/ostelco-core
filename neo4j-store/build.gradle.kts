import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))
  api(project(":kts-engine"))
  implementation("com.fasterxml.jackson.core:jackson-databind:${Version.jacksonDatabind}")

  implementation("org.neo4j:neo4j-graphdb-api:${Version.neo4j}")
  implementation("org.neo4j.driver:neo4j-java-driver:${Version.neo4jDriver}")
  implementation("org.neo4j:neo4j-slf4j:${Version.neo4j}")

  testImplementation("com.palantir.docker.compose:docker-compose-rule-junit4:${Version.dockerComposeJunitRule}")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))

  testImplementation("org.mockito:mockito-core:${Version.mockito}")
}

apply(from = "../gradle/jacoco.gradle")