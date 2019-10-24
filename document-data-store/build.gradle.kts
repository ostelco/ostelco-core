plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))
  implementation(project(":data-store"))

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
}

apply(from = "../gradle/jacoco.gradle.kts")