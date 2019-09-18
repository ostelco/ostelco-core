plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val kotlinXCoroutinesVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val googleCloudVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val arrowVersion:String by rootProject.extra
  val jaxbVersion:String by rootProject.extra
  val javaxActivationVersion:String by rootProject.extra

  api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  api("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinXCoroutinesVersion")

  api("io.dropwizard:dropwizard-auth:$dropwizardVersion")
  implementation("com.google.cloud:google-cloud-pubsub:$googleCloudVersion")
  implementation("com.google.cloud:google-cloud-datastore:$googleCloudVersion")

  api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

  api(project(":ocs-grpc-api"))
  api(project(":analytics-grpc-api"))
  api(project(":model"))

  api("io.dropwizard:dropwizard-core:$dropwizardVersion")
  
  api("io.arrow-kt:arrow-core:$arrowVersion")
  api("io.arrow-kt:arrow-typeclasses:$arrowVersion")
  api("io.arrow-kt:arrow-instances-core:$arrowVersion")
  api("io.arrow-kt:arrow-effects:$arrowVersion")

  runtimeOnly("javax.xml.bind:jaxb-api:$jaxbVersion")
  runtimeOnly("javax.activation:activation:$javaxActivationVersion")

  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

apply(from = "../gradle/jacoco.gradle")