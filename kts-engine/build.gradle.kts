plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val mockitoVersion:String by rootProject.extra

  api(kotlin("script-util"))
  api(kotlin("script-runtime"))
  
  api(kotlin("compiler-embeddable"))
  api(kotlin("scripting-compiler-embeddable"))

  // api(kotlin("compiler"))
  // api(kotlin("scripting-compiler"))

  implementation(project(":prime-modules"))
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))

  testImplementation("org.mockito:mockito-core:$mockitoVersion")
}

apply(from = "../gradle/jacoco.gradle")