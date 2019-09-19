plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra

  implementation(kotlin("stdlib-jdk8"))

  implementation("io.dropwizard:dropwizard-core:$dropwizardVersion")
  implementation("com.github.everit-org.json-schema:org.everit.json.schema:1.11.1")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
}

apply(from = "../../gradle/jacoco.gradle")