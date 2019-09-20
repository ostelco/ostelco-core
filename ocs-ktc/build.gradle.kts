plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val googleCloudVersion:String by rootProject.extra
  val dockerComposeJunitRuleVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra
  val mockitoVersion:String by rootProject.extra

  implementation(project(":prime-modules"))

  implementation("com.google.cloud:google-cloud-pubsub:$googleCloudVersion")

  testImplementation("com.palantir.docker.compose:docker-compose-rule-junit4:$dockerComposeJunitRuleVersion")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
  
  testImplementation("org.mockito:mockito-core:$mockitoVersion")

}

apply(from = "../gradle/jacoco.gradle")