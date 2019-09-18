plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val googleCloudVersion:String by rootProject.extra
  val firebaseVersion:String by rootProject.extra
  val slf4jVersion:String by rootProject.extra

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
  implementation("com.google.cloud:google-cloud-datastore:$googleCloudVersion")
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

  // TODO vihang: this dependency is added only for @Exclude annotation for firebase
  implementation("com.google.firebase:firebase-admin:$firebaseVersion")
  implementation("org.slf4j:slf4j-api:$slf4jVersion")
}