plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val dropwizardVersion:String by rootProject.extra
  val jjwtVersion:String by rootProject.extra

  implementation(project(":prime-modules"))
  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
}

apply(from = "../gradle/jacoco.gradle")