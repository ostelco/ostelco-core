import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

version = "1.0.0"

dependencies {

  val dropwizardVersion:String by rootProject.extra
  val kotlinXCoroutinesVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra
  val googleCloudVersion:String by rootProject.extra
  val arrowVersion:String by rootProject.extra
  val mockitoVersion:String by rootProject.extra

  api(project(":model"))
  
  implementation(project(":data-store"))

  implementation("io.dropwizard:dropwizard-core:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinXCoroutinesVersion")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

  implementation(kotlin("stdlib-jdk8"))
  implementation("com.google.cloud:google-cloud-datastore:$googleCloudVersion")

  implementation("io.arrow-kt:arrow-core:$arrowVersion")
  implementation("io.arrow-kt:arrow-typeclasses:$arrowVersion")
  implementation("io.arrow-kt:arrow-instances-core:$arrowVersion")
  implementation("io.arrow-kt:arrow-effects:$arrowVersion")
  
  runtimeOnly("io.dropwizard:dropwizard-json-logging:$dropwizardVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation("org.mockito:mockito-core:$mockitoVersion")
  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))

}

application {
  mainClassName = "org.ostelco.storage.scaninfo.shredder.ScanInfoShredderApplicationKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

apply(from = "../gradle/jacoco.gradle")