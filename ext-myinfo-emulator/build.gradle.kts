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
  val cxfVersion:String by rootProject.extra
  val jaxbVersion:String by rootProject.extra
  val javaxActivationVersion:String by rootProject.extra

  implementation(kotlin("stdlib-jdk8"))

  // This is not a prime-module. Just needed access to getLogger and Dropwizard KotlinModule.
  implementation(project(":prime-modules"))

  implementation("io.dropwizard:dropwizard-core:$dropwizardVersion")

  implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
  implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

  implementation("org.apache.cxf:cxf-rt-rs-security-jose:$cxfVersion")

  runtimeOnly("javax.xml.bind:jaxb-api:$jaxbVersion")
  runtimeOnly("javax.activation:activation:$javaxActivationVersion")
}

application {
  mainClassName = "org.ostelco.ext.myinfo.MyInfoEmulatorAppKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

apply(from = "../gradle/jacoco.gradle")