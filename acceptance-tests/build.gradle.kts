import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val kotlinXCoroutinesVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val googleCloudVersion:String by rootProject.extra
  val stripeVersion:String by rootProject.extra
  val jjwtVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion") {
    exclude(module = "kotlin-stdlib-common")
  }
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinXCoroutinesVersion")

  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")
  implementation(project(":prime-customer-api"))
  implementation(project(":diameter-test"))
  implementation(project(":ocs-grpc-api"))

  implementation("com.google.cloud:google-cloud-pubsub:$googleCloudVersion")

  implementation("com.stripe:stripe-java:$stripeVersion")

  implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
  implementation("org.zalando.phrs:jersey-media-json-gson:0.1")

  implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")

  implementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
}

application {
  mainClassName = ""
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}