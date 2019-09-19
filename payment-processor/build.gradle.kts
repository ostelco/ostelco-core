plugins {
  kotlin("jvm")
  `java-library`
  idea
}

dependencies {

  val stripeVersion:String by rootProject.extra
  val googleCloudVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra

  implementation(project(":prime-modules"))
  implementation(project(":data-store"))

  implementation("com.stripe:stripe-java:$stripeVersion")

  implementation("com.google.cloud:google-cloud-pubsub:$googleCloudVersion")
  implementation("com.google.cloud:google-cloud-datastore:$googleCloudVersion")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
}

sourceSets.create("integration") {
  java.srcDirs("src/integration-tests/kotlin")
  resources.srcDirs("src/integration-tests/resources")
  compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
  runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

val integration = tasks.create("integration", Test::class.java) {
  description = "Runs the integration tests."
  group = "Verification"
  testClassesDirs = sourceSets.getByName("integration").output.classesDirs
  classpath = sourceSets.getByName("integration").runtimeClasspath
}

configurations.named("integrationImplementation") {
  extendsFrom(configurations["implementation"])
  extendsFrom(configurations["runtime"])
  extendsFrom(configurations["runtimeOnly"])
  extendsFrom(configurations["testImplementation"])
}

tasks.build.get().dependsOn(integration)

apply(from = "../gradle/jacoco.gradle")

idea {
  module {
    testSourceDirs.add(File("src/integration-tests/kotlin"))
  }
}