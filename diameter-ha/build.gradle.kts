import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  api("org.mobicents.diameter:jdiameter-api:1.7.1-123") {
    exclude(module = "netty-all")
  }
  api("org.mobicents.diameter:jdiameter-impl:1.7.1-123") {
    exclude(module = "netty-all")
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
    exclude(group = "log4j", module = "log4j")
  }
  implementation("org.slf4j:log4j-over-slf4j:${Version.slf4j}")

  compile("io.lettuce:lettuce-core:5.2.0.RELEASE")

  testImplementation(kotlin("test-junit"))
  testRuntimeOnly("org.hamcrest:hamcrest-all:1.3")
  testImplementation("org.mockito:mockito-all:1.10.19")
}

apply(from = "../gradle/jacoco.gradle.kts")