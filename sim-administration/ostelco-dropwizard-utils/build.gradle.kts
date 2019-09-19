plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val swaggerVersion:String by rootProject.extra
  val javaxActivationApiVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val csvVersion:String by rootProject.extra

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  implementation("io.dropwizard:dropwizard-core:$dropwizardVersion")
  implementation("io.swagger.core.v3:swagger-jaxrs2:$swaggerVersion")


  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")

  implementation("io.dropwizard:dropwizard-core:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-auth:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-jdbi:$dropwizardVersion")

  implementation("org.conscrypt:conscrypt-openjdk-uber:2.2.1")

  testImplementation("javax.activation:javax.activation-api:$javaxActivationApiVersion")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
  implementation("org.apache.commons:commons-csv:$csvVersion")
  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
}

apply(from = "../../gradle/jacoco.gradle")