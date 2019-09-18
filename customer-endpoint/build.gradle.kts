plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val dropwizardVersion:String by rootProject.extra
  val mockitoVersion:String by rootProject.extra
  val assertJVersion:String by rootProject.extra
  val jjwtVersion:String by rootProject.extra

  implementation(project(":prime-modules"))

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation("org.mockito:mockito-core:$mockitoVersion")
  testImplementation("org.assertj:assertj-core:$assertJVersion")

  testImplementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

  testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
}

apply(from = "../gradle/jacoco.gradle")