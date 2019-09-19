plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val dropwizardVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra

  implementation(project(":prime-modules"))

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
}

apply(from = "../gradle/jacoco.gradle")