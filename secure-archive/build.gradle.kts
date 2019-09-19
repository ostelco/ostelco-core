plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val jacksonVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val googleCloudVersion:String by rootProject.extra
  val tinkVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra

  implementation(project(":prime-modules"))
  
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")
  implementation("com.google.cloud:google-cloud-datastore:$googleCloudVersion")
  implementation("com.google.cloud:google-cloud-storage:$googleCloudVersion")
  implementation("com.google.crypto.tink:tink:$tinkVersion")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
}

apply(from = "../gradle/jacoco.gradle")