plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
}

apply(from = "../../gradle/jacoco.gradle")