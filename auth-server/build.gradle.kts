import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
  idea
}

dependencies {

  val kotlinVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val jaxbVersion:String by rootProject.extra
  val javaxActivationVersion:String by rootProject.extra

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  implementation("io.dropwizard:dropwizard-core:$dropwizardVersion")

  implementation(project(":firebase-extensions"))
  implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion") {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
  }

  runtimeOnly("javax.xml.bind:jaxb-api:$jaxbVersion")
  runtimeOnly("javax.activation:activation:$javaxActivationVersion")
  
  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
  testRuntimeOnly("org.hamcrest:hamcrest-all:1.3")
}

application {
  mainClassName = "org.ostelco.auth.AuthServerApplicationKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
}

sourceSets.create("integration") {
  java.srcDirs("src/integration-tests/kotlin")
  resources.srcDirs("src/integration-tests/resources")
  compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
  runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

configurations.named("integrationImplementation") {
  extendsFrom(configurations["implementation"])
  extendsFrom(configurations["runtime"])
  extendsFrom(configurations["runtimeOnly"])
  extendsFrom(configurations["testImplementation"])
}

val integration = tasks.create("integration", Test::class.java) {
  description = "Runs the integration tests."
  group = "Verification"
  testClassesDirs = sourceSets.getByName("integration").output.classesDirs
  classpath = sourceSets.getByName("integration").runtimeClasspath
}

tasks.build.get().dependsOn(integration)
integration.mustRunAfter(tasks.test)

apply(from = "../gradle/jacoco.gradle")

idea {
  module {
    testSourceDirs.add(File("src/integration-tests/kotlin"))
  }
}