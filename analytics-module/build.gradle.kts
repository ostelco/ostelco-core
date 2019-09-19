plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val googleCloudVersion:String by rootProject.extra

  implementation (project(":prime-modules"))

  implementation ("com.google.cloud:google-cloud-pubsub:$googleCloudVersion")
  implementation ("com.google.code.gson:gson:2.8.5")

  testImplementation ("com.google.api:gax-grpc:1.48.1")
}
