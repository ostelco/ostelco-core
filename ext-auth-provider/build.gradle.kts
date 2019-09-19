import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val jjwtVersion:String by rootProject.extra
  val jaxbVersion:String by rootProject.extra
  val javaxActivationVersion:String by rootProject.extra

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation("io.dropwizard:dropwizard-core:$dropwizardVersion")

  implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

  runtimeOnly("javax.xml.bind:jaxb-api:$jaxbVersion")
  runtimeOnly("javax.activation:activation:$javaxActivationVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

application {
  mainClassName = "org.ostelco.ext.authprovider.AuthProviderAppKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

apply(from = "../gradle/jacoco.gradle")