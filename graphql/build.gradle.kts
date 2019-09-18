plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val dropwizardVersion:String by rootProject.extra
  val mockitoVersion:String by rootProject.extra
  val jjwtVersion:String by rootProject.extra

  implementation(project(":prime-modules"))

  implementation("com.graphql-java:graphql-java:12.0")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation("org.mockito:mockito-core:$mockitoVersion")
  testImplementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
}

apply(from = "../gradle/jacoco.gradle")