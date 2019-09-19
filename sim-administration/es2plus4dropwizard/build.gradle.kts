plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val swaggerVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val mockitoVersion:String by rootProject.extra
  
  implementation(project(":sim-administration:jersey-json-schema-validator"))
  implementation(project(":prime-modules"))
  
  implementation("io.swagger.core.v3:swagger-core:$swaggerVersion")
  implementation("io.swagger.core.v3:swagger-jaxrs2:$swaggerVersion")
  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")
  
  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation("org.mockito:mockito-core:$mockitoVersion")
}

apply(from = "../../gradle/jacoco.gradle")