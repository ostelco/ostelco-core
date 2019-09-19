import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))

  implementation("com.google.cloud:google-cloud-pubsub:${Version.googleCloud}")
  implementation("com.google.code.gson:gson:2.8.5")

  testImplementation("com.google.api:gax-grpc:1.48.1")
}
