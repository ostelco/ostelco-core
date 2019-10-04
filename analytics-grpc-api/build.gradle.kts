import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.ostelco.prime.gradle.Version

plugins {
  `java-library`
  id("com.google.protobuf")
  idea
}

dependencies {
  api("io.grpc:grpc-netty-shaded:${Version.grpc}")
  api("io.grpc:grpc-protobuf:${Version.grpc}")
  api("io.grpc:grpc-stub:${Version.grpc}")
  api("io.grpc:grpc-core:${Version.grpc}")
  implementation("com.google.protobuf:protobuf-java:${Version.protoc}")
  implementation("com.google.protobuf:protobuf-java-util:${Version.protoc}")
  implementation("javax.annotation:javax.annotation-api:${Version.javaxAnnotation}")
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

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

idea {
  module {
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/java"))
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/grpc"))
  }
}

tasks.clean {
  delete(protobufGeneratedFilesBaseDir)
}
