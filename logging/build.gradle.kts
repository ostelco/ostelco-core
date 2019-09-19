plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra

  implementation(kotlin("stdlib-jdk8"))

  implementation("io.dropwizard:dropwizard-logging:$dropwizardVersion")

  implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
}

apply(from = "../gradle/jacoco.gradle")