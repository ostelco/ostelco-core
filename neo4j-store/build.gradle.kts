plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val jacksonVersion:String by rootProject.extra
  val neo4jVersion:String by rootProject.extra
  val neo4jDriverVersion:String by rootProject.extra
  val dockerComposeJunitRuleVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra
  val mockitoVersion:String by rootProject.extra

  implementation(project(":prime-modules"))
  api(project(":kts-engine"))
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

  implementation("org.neo4j:neo4j-graphdb-api:$neo4jVersion")
  implementation("org.neo4j.driver:neo4j-java-driver:$neo4jDriverVersion")
  implementation("org.neo4j:neo4j-slf4j:$neo4jVersion")

  testImplementation("com.palantir.docker.compose:docker-compose-rule-junit4:$dockerComposeJunitRuleVersion")

  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")

  testImplementation("org.mockito:mockito-core:$mockitoVersion")
}

apply(from = "../gradle/jacoco.gradle")