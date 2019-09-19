plugins {
  kotlin("jvm")
  `java-library`
}

version = "1.0.0-SNAPSHOT"

dependencies {

  val kotlinVersion:String by rootProject.extra
  val slf4jVersion:String by rootProject.extra

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
  implementation("org.slf4j:log4j-over-slf4j:$slf4jVersion")

  compile("io.lettuce:lettuce-core:5.1.8.RELEASE")

  testImplementation(kotlin("test-junit"))
  testRuntimeOnly("org.hamcrest:hamcrest-all:1.3")
  testImplementation("org.mockito:mockito-all:1.10.19")
}

apply(from = "../gradle/jacoco.gradle")