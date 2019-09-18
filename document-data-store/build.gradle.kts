plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra

  implementation(project(":prime-modules"))
  implementation(project(":data-store"))

  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

apply(from = "../gradle/jacoco.gradle")