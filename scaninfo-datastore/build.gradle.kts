plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val jacksonVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val googleCloudVersion:String by rootProject.extra
  val tinkVersion:String by rootProject.extra
  val dockerComposeJunitRuleVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra
  val mockitoVersion:String by rootProject.extra

  implementation(project(":prime-modules"))
  implementation(project(":data-store"))
  
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")
  implementation("com.google.cloud:google-cloud-datastore:$googleCloudVersion")
  implementation("com.google.cloud:google-cloud-storage:$googleCloudVersion")
  implementation("com.google.crypto.tink:tink:$tinkVersion")

  testImplementation("com.palantir.docker.compose:docker-compose-rule-junit4:$dockerComposeJunitRuleVersion")

  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")

  testImplementation(project(":secure-archive"))
  
  testImplementation("org.mockito:mockito-core:$mockitoVersion")
}

apply(from = "../gradle/jacoco.gradle")