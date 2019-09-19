import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
  `java-library`
  id("com.google.protobuf")
  idea
}

val grpcVersion:String by rootProject.extra
val protocVersion:String by rootProject.extra
val javaxAnnotationVersion:String by rootProject.extra

dependencies {
  api("io.grpc:grpc-netty-shaded:$grpcVersion")
  api("io.grpc:grpc-protobuf:$grpcVersion")
  api("io.grpc:grpc-stub:$grpcVersion")
  api("io.grpc:grpc-core:$grpcVersion")
  implementation("com.google.protobuf:protobuf-java:$protocVersion")
  implementation("com.google.protobuf:protobuf-java-util:$protocVersion")
  implementation("javax.annotation:javax.annotation-api:$javaxAnnotationVersion")
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

idea {
  module {
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/java"))
    sourceDirs.addAll(files("${protobufGeneratedFilesBaseDir}/main/grpc"))
  }
}

tasks.clean {
  delete(protobufGeneratedFilesBaseDir)
}
