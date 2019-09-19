plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val dropwizardVersion:String by rootProject.extra

  implementation(project(":prime-modules"))

  testImplementation(project(":jersey"))
  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
}

apply(from = "../gradle/jacoco.gradle")