plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra

  implementation(kotlin("stdlib-jdk8"))

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
}

apply(from = "../../gradle/jacoco.gradle")