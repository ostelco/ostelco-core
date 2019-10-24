import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  api(kotlin("script-util"))
  api(kotlin("script-runtime"))
  
  api(kotlin("compiler-embeddable"))
  api(kotlin("scripting-compiler-embeddable"))

  // api(kotlin("compiler"))
  // api(kotlin("scripting-compiler"))

  implementation(project(":prime-modules"))
  implementation("com.fasterxml.jackson.core:jackson-databind:${Version.jacksonDatabind}")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))

  testImplementation("org.mockito:mockito-core:${Version.mockito}")
}

apply(from = "../gradle/jacoco.gradle.kts")