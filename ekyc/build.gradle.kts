import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val jjwtVersion:String by rootProject.extra
  val cxfVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val junit5Version:String by rootProject.extra

  implementation(project(":prime-modules"))

  implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
  implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

  implementation("org.apache.cxf:cxf-rt-rs-security-jose:$cxfVersion")

  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")


  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation(project(":ext-myinfo-emulator"))

  testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
}

tasks.test {

  // native support to Junit5 in Gradle 4.6+
  useJUnitPlatform {
    includeEngines("junit-jupiter")
  }
  testLogging {
    exceptionFormat = FULL
    events("PASSED", "FAILED", "SKIPPED")
  }
}

apply(from = "../gradle/jacoco.gradle")