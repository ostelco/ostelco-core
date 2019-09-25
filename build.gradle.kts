
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

// these are needed only in top-level module
plugins {
  base
  java
  id("project-report")
  id("com.github.ben-manes.versions") version "0.25.0"
  jacoco
  kotlin("jvm") version "1.3.50" apply false
  id("com.google.protobuf") version "0.8.10" apply false
  id("com.github.johnrengelman.shadow") version "5.1.0" apply false
  idea
}

allprojects {
  apply(plugin = "jacoco")

  group = "org.ostelco"
  version = "1.0.0-SNAPSHOT"

  repositories {
    mavenCentral()
    maven { url = URI("https://dl.bintray.com/palantir/releases") } // docker-compose-rule is published on bintray
    jcenter()
    maven { url = URI("http://repository.jboss.org/nexus/content/repositories/releases/") }
    maven { url = URI("https://maven.repository.redhat.com/ga/") }
    maven { url = URI("http://clojars.org/repo/") }
    maven { url = URI("https://jitpack.io") }
  }

  jacoco {
    toolVersion = "0.8.4"
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_12
  targetCompatibility = JavaVersion.VERSION_12
}

subprojects {
  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_12.majorVersion
    }
  }
}

fun isNonStable(version: String): Boolean {
  val regex = "^[0-9,.v-]+$".toRegex()
  val isStable = regex.matches(version)
  return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
  // Example 1: reject all non stable versions
  rejectVersionIf {
    isNonStable(candidate.version)
  }

  // Example 2: disallow release candidates as upgradable versions from stable versions
  rejectVersionIf {
    isNonStable(candidate.version) && !isNonStable(currentVersion)
  }

  // Example 3: using the full syntax
  resolutionStrategy {
    componentSelection {
      all {
        if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
          reject("Release candidate")
        }
      }
    }
  }
}