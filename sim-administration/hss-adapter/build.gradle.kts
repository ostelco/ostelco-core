import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
  idea
  id("com.google.protobuf")
}

dependencies {
  implementation(project(":prime-modules"))
  implementation(project(":sim-administration:simmanager"))

  api("io.grpc:grpc-netty-shaded:${Version.grpc}")
  api("io.grpc:grpc-protobuf:${Version.grpc}")
  api("io.grpc:grpc-stub:${Version.grpc}")
  api("io.grpc:grpc-core:${Version.grpc}")
  implementation("javax.annotation:javax.annotation-api:${Version.javaxAnnotation}")

  api("io.arrow-kt:arrow-core:${Version.arrow}")
  api("io.arrow-kt:arrow-syntax:${Version.arrow}")

  implementation(kotlin("reflect"))
  implementation(kotlin("stdlib-jdk8"))

  implementation("io.dropwizard:dropwizard-core:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-jdbi3:${Version.dropwizard}")
  implementation("io.dropwizard.metrics:metrics-core:${Version.metrics}")
  implementation("com.google.guava:guava:${Version.guava}")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:${Version.mockitoKotlin}")

  testImplementation("org.testcontainers:postgresql:${Version.testcontainers}")
}

sourceSets.create("integration") {
  java.srcDirs("src/integration-test/kotlin")
  resources.srcDirs("src/integration-test/resources")
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
  protoc { artifact = "com.google.protobuf:protoc:${Version.protoc}" }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:${Version.grpc}"
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

apply(from = "../../gradle/jacoco.gradle.kts")

idea {
  module {
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/java"))
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/grpc"))
    testSourceDirs = testSourceDirs + file("src/integration-test/kotlin")
  }
}

tasks.clean {
  delete(protobufGeneratedFilesBaseDir)
}
