plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra

  implementation(project(":prime-modules"))
  implementation(project(":data-store"))

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
}

apply(from = "../gradle/jacoco.gradle")