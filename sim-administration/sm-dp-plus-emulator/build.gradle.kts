import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val csvVersion:String by rootProject.extra
  val jaxbVersion:String by rootProject.extra
  val javaxActivationVersion:String by rootProject.extra

  implementation(project(":sim-administration:jersey-json-schema-validator"))
  implementation(project(":sim-administration:simcard-utils"))
  implementation(project(":sim-administration:es2plus4dropwizard"))
  implementation(project(":sim-administration:ostelco-dropwizard-utils"))

  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")

  implementation("io.dropwizard:dropwizard-core:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-auth:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-jdbi:$dropwizardVersion")

  implementation("org.conscrypt:conscrypt-openjdk-uber:2.2.1")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
  implementation("org.apache.commons:commons-csv:$csvVersion")

  runtimeOnly("javax.xml.bind:jaxb-api:$jaxbVersion")
  runtimeOnly("javax.activation:activation:$javaxActivationVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
}

application {
  mainClassName = "org.ostelco.simcards.smdpplus.SmDpPlusApplicationKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

apply(from = "../../gradle/jacoco.gradle")