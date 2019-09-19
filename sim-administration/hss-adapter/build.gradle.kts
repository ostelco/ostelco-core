import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
  idea
  id("com.google.protobuf")
}

val grpcVersion:String by rootProject.extra
val protocVersion:String by rootProject.extra
val javaxAnnotationVersion:String by rootProject.extra

dependencies {

  val arrowVersion:String by rootProject.extra
  val kotlinVersion:String by rootProject.extra
  val dropwizardVersion:String by rootProject.extra
  val metricsVersion:String by rootProject.extra
  val guavaVersion:String by rootProject.extra
  val jacksonVersion:String by rootProject.extra
  val mockitoKotlinVersion:String by rootProject.extra
  val testcontainersVersion:String by rootProject.extra

  implementation(project(":prime-modules"))
  implementation(project(":sim-administration:simmanager"))

  api("io.grpc:grpc-netty-shaded:$grpcVersion")
  api("io.grpc:grpc-protobuf:$grpcVersion")
  api("io.grpc:grpc-stub:$grpcVersion")
  api("io.grpc:grpc-core:$grpcVersion")
  implementation("javax.annotation:javax.annotation-api:$javaxAnnotationVersion")

  api("io.arrow-kt:arrow-core:$arrowVersion")
  api("io.arrow-kt:arrow-typeclasses:$arrowVersion")
  api("io.arrow-kt:arrow-instances-core:$arrowVersion")
  api("io.arrow-kt:arrow-effects:$arrowVersion")

  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation("io.dropwizard:dropwizard-core:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-client:$dropwizardVersion")
  implementation("io.dropwizard:dropwizard-jdbi3:$dropwizardVersion")
  implementation("io.dropwizard.metrics:metrics-core:$metricsVersion")
  implementation("com.google.guava:guava:$guavaVersion")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

  testImplementation("io.dropwizard:dropwizard-testing:$dropwizardVersion")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:$mockitoKotlinVersion")

  testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
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

application {
  mainClassName = "org.ostelco.simcards.hss.HssAdapterApplicationKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
  isZip64 = true
}

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
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/java"))
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/grpc"))
  }
}

tasks.clean {
  delete(protobufGeneratedFilesBaseDir)
}
