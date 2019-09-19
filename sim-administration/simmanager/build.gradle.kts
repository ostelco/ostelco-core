
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
  kotlin("jvm")
  id("com.github.johnrengelman.shadow")
  idea
  id("com.google.protobuf")
}

val grpcVersion:String by rootProject.extra
val protocVersion:String by rootProject.extra
val javaxAnnotationVersion:String by rootProject.extra

dependencies {

  val arrowVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val metricsVersion:String by rootProject.extra
  val guavaVersion:String by rootProject.extra
  val csvVersion:String by rootProject.extra
  val jdbi3Version:String by rootProject.extra
  val postgresqlVersion:String by rootProject.extra
  val mockitoKotlinVersion:String by rootProject.extra
  val testcontainersVersion:String by rootProject.extra

  implementation(project(":prime-modules"))

  implementation(project(":sim-administration:jersey-json-schema-validator"))
  implementation(project(":sim-administration:simcard-utils"))
  implementation(project(":sim-administration:es2plus4dropwizard"))
  implementation(project(":sim-administration:ostelco-dropwizard-utils"))

  // Arrow
  api("io.arrow-kt:arrow-core:$arrowVersion")
  api("io.arrow-kt:arrow-typeclasses:$arrowVersion")
  api("io.arrow-kt:arrow-instances-core:$arrowVersion")
  api("io.arrow-kt:arrow-effects:$arrowVersion")

  // Grpc
  api("io.grpc:grpc-netty-shaded:$grpcVersion")
  api("io.grpc:grpc-protobuf:$grpcVersion")
  api("io.grpc:grpc-stub:$grpcVersion")
  api("io.grpc:grpc-core:$grpcVersion")
  implementation("com.google.protobuf:protobuf-java:$protocVersion")
  implementation("com.google.protobuf:protobuf-java-util:$protocVersion")
  implementation("javax.annotation:javax.annotation-api:$javaxAnnotationVersion")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

  // Dropwizard
  implementation("io.dropwizard:dropwizard-core:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-jdbi3:$dropwizardVersion")
  implementation("io.dropwizard.metrics:metrics-core:$metricsVersion")
  implementation("com.google.guava:guava:$guavaVersion") {
    isForce = true
  }

  // CSV
  implementation("org.apache.commons:commons-csv:$csvVersion")

  // Jdbi3
  //
  // For dropwizard ver. 1.3.9:
  //   dropwizard-jdbi3 ver. 1.3.9 pulls in jdbi3-core ver. 3.5.1
  //   jdbi3-postgres ver. 3.5.1 pulls in org.postgresql ver. 42.2.2
  // But latest ver. of jdbi3-core is 3.8.2, and for that version
  // jdbi3-postgres no longer pulls in org.postgresql
  //
  // Dropwizard matching version:
  //    jdbi3Version = "3.5.1"
  //    postgresqlVersion = "42.2.2"
  // With latest:
  //    jdbi3Version = "3.8.2"
  //    postgresqlVersion = "42.2.5"
  //
  implementation("org.jdbi:jdbi3-kotlin:$jdbi3Version")
  implementation("org.jdbi:jdbi3-kotlin-sqlobject:$jdbi3Version")
  implementation("org.jdbi:jdbi3-postgres:$jdbi3Version")
  implementation("org.postgresql:postgresql:$postgresqlVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:$mockitoKotlinVersion")
  testImplementation("org.testcontainers:postgresql:$testcontainersVersion")

  testImplementation(project(":sim-administration:sm-dp-plus-emulator"))
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

var protobufGeneratedFilesBaseDir: String = ""

protobuf {
  protoc { artifact = "com.google.protobuf:protoc:$protocVersion" }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
    }
  }
  generateProtoTasks {
    ofSourceSet("main").forEach {
      it.plugins {
        id("grpc")
      }
    }
  }
  protobufGeneratedFilesBaseDir = generatedFilesBaseDir
}

apply(from = "../../gradle/jacoco.gradle")

idea {
  module {
    testSourceDirs.add(file("src/integration-test/kotlin"))
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/java"))
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/grpc"))
  }
}

tasks.clean {
  delete(protobufGeneratedFilesBaseDir)
}
