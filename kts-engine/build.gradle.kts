plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val mockitoVersion:String by rootProject.extra

  api("org.jetbrains.kotlin:kotlin-script-util:$kotlinVersion")
  api("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
  
  api("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
  api("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlinVersion")

  // api("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
  // api("org.jetbrains.kotlin:kotlin-scripting-compiler:$kotlinVersion")

  implementation(project(":prime-modules"))
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")

  testImplementation("org.mockito:mockito-core:$mockitoVersion")
}

apply(from = "../gradle/jacoco.gradle")