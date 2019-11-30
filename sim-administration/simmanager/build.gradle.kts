import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  id("com.github.johnrengelman.shadow")
  idea
  id("com.google.protobuf")
}

dependencies {
  implementation(project(":prime-modules"))

  implementation(project(":sim-administration:jersey-json-schema-validator"))
  implementation(project(":sim-administration:simcard-utils"))
  implementation(project(":sim-administration:es2plus4dropwizard"))
  implementation(project(":sim-administration:ostelco-dropwizard-utils"))

  // Arrow
  api("io.arrow-kt:arrow-core-data:${Version.arrow}")
  api("io.arrow-kt:arrow-core-extensions:${Version.arrow}")
  api("io.arrow-kt:arrow-typeclasses:${Version.arrow}")
  api("io.arrow-kt:arrow-extras-data:${Version.arrow}")
  api("io.arrow-kt:arrow-extras-extensions:${Version.arrow}")
  api("io.arrow-kt:arrow-effects-data:${Version.arrow}")
  api("io.arrow-kt:arrow-effects-extensions:${Version.arrow}")
  api("io.arrow-kt:arrow-effects-io-extensions:${Version.arrow}")

  // Grpc
  api("io.grpc:grpc-netty-shaded:${Version.grpc}")
  api("io.grpc:grpc-protobuf:${Version.grpc}")
  api("io.grpc:grpc-stub:${Version.grpc}")
  api("io.grpc:grpc-core:${Version.grpc}")
  implementation("com.google.protobuf:protobuf-java:${Version.protoc}")
  implementation("com.google.protobuf:protobuf-java-util:${Version.protoc}")
  implementation("javax.annotation:javax.annotation-api:${Version.javaxAnnotation}")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}")

  // Dropwizard
  implementation("io.dropwizard:dropwizard-core:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")
  implementation("io.dropwizard:dropwizard-jdbi3:${Version.dropwizard}")
  implementation("io.dropwizard.metrics:metrics-core:${Version.metrics}")
  implementation("com.google.guava:guava:${Version.guava}") {
    isForce = true
  }

  // CSV
  implementation("org.apache.commons:commons-csv:${Version.csv}")

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
  implementation("org.jdbi:jdbi3-kotlin:${Version.jdbi3}")
  implementation("org.jdbi:jdbi3-kotlin-sqlobject:${Version.jdbi3}")
  implementation("org.jdbi:jdbi3-postgres:${Version.jdbi3}")
  implementation("org.postgresql:postgresql:${Version.postgresql}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:${Version.mockitoKotlin}")
  testImplementation("net.bytebuddy:byte-buddy:${Version.byteBuddy}") {
      because("mockito-kotlin:2.2.0 has byte-buddy:1.9.0 which does not work for java13")
  }
  testImplementation("net.bytebuddy:byte-buddy-agent:${Version.byteBuddy}") {
      because("mockito-kotlin:2.2.0 has byte-buddy:1.9.0 which does not work for java13")
  }
  testImplementation("org.testcontainers:postgresql:${Version.testcontainers}")

  testImplementation(project(":sim-administration:sm-dp-plus-emulator"))
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

jacoco {
  toolVersion = "0.8.3" // because 0.8.4 has a issue - https://github.com/mockito/mockito/issues/1717
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
    testSourceDirs = testSourceDirs + file("src/integration-test/kotlin")
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/java"))
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/grpc"))
  }
}

tasks.clean {
  delete(protobufGeneratedFilesBaseDir)
}