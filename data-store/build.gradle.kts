plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val jacksonVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val googleCloudVersion:String by rootProject.extra
  val dockerComposeJunitRuleVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra
  val mockitoVersion:String by rootProject.extra

  implementation(project(":prime-modules"))
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")
  
  api("com.google.cloud:google-cloud-core:$googleCloudVersion")
  implementation("com.google.cloud:google-cloud-datastore:$googleCloudVersion")

  testImplementation("com.palantir.docker.compose:docker-compose-rule-junit4:$dockerComposeJunitRuleVersion")

  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")

  testImplementation("org.mockito:mockito-core:$mockitoVersion")
}

apply(from = "../gradle/jacoco.gradle")