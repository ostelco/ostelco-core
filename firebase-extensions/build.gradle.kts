plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val firebaseVersion:String by rootProject.extra

  implementation(project(":prime-modules"))

  // Match netty via ocs-grpc-api
  api("com.google.firebase:firebase-admin:$firebaseVersion")
}