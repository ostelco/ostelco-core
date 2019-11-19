import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))

  implementation("io.dropwizard:dropwizard-core:${Version.dropwizard}")
  implementation("io.swagger.core.v3:swagger-jaxrs2:${Version.swagger}")

  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")

  implementation("io.dropwizard:dropwizard-core:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-auth:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-jdbi:${Version.dropwizard}")

  implementation("org.conscrypt:conscrypt-openjdk-uber:2.2.1")

  testImplementation("javax.activation:javax.activation-api:${Version.javaxActivationApi}")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}")
  implementation("org.apache.commons:commons-csv:${Version.csv}")
  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_12.majorVersion
  }
}

apply(from = "../../gradle/jacoco.gradle.kts")