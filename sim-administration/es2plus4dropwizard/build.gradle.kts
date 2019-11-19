import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":sim-administration:jersey-json-schema-validator"))
  implementation(project(":prime-modules"))
  
  implementation("io.swagger.core.v3:swagger-core:${Version.swagger}")
  implementation("io.swagger.core.v3:swagger-jaxrs2:${Version.swagger}")
  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")
  
  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation("org.mockito:mockito-core:${Version.mockito}")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_12.majorVersion
  }
}

apply(from = "../../gradle/jacoco.gradle.kts")