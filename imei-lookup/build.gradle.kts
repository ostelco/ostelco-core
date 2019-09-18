plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val dropwizardVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra

  implementation(project(":prime-modules"))

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")

  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

apply(from = "../gradle/jacoco.gradle")