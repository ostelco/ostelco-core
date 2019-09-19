
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

// these are needed only in top-level module
plugins {
  java
  id("project-report")
  id("com.github.ben-manes.versions") version "0.25.0"
  jacoco
  kotlin("jvm") version "1.3.50" apply false
  id("com.google.protobuf") version "0.8.10" apply false
  id("com.github.johnrengelman.shadow") version "5.1.0" apply false
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
    toolVersion = "0.8.2"
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_12
  targetCompatibility = JavaVersion.VERSION_12
}

val assertJVersion by extra("3.13.2")
val arrowVersion by extra("0.8.2")
val beamVersion by extra("2.15.0")
val csvVersion by extra("1.7")
val cxfVersion by extra("3.3.3")
val dockerComposeJunitRuleVersion by extra("1.3.0")
val dropwizardVersion by extra("1.3.14")
val metricsVersion by extra("4.1.0")
val firebaseVersion by extra("6.10.0")
val googleCloudVersion by extra("1.90.0")
val grpcVersion by extra("1.23.0")
val guavaVersion by extra("28.1-jre")
val jacksonVersion by extra("2.9.9")
val javaxActivationVersion by extra("1.1.1")
val javaxActivationApiVersion by extra("1.2.0")
val javaxAnnotationVersion by extra("1.3.2")
// Keeping it version 1.16.1 to be consistent with grpc via PubSub client lib
// Keeping it version 1.16.1 to be consistent with netty via Firebase lib
val jaxbVersion by extra("2.3.1")
val jdbi3Version by extra("3.10.0")
val jjwtVersion by extra("0.10.7")
val junit5Version by extra("5.5.2")
val kotlinVersion by extra("1.3.50")
val kotlinXCoroutinesVersion by extra("1.3.1")
val mockitoVersion by extra("3.0.0")
val mockitoKotlinVersion by extra("2.2.0")
val neo4jDriverVersion by extra("1.7.5")
val neo4jVersion by extra("3.5.9")
val opencensusVersion by extra("0.24.0")
val postgresqlVersion by extra("42.2.8")  // See comment in ./sim-administration/simmanager/build.gradle
val prometheusDropwizardVersion by extra("2.2.0")
val protocVersion by extra("3.9.1")
val slf4jVersion by extra("1.7.28")
// IMPORTANT: When Stripe SDK library version is updated, check if the Stripe API version has changed.
// If so, then update API version in Stripe Web Console for callback Webhooks.
val stripeVersion by extra("12.0.0")
val swaggerVersion by extra("2.0.9")
val swaggerCodegenVersion by extra("2.4.8")
val testcontainersVersion by extra("1.12.1")
val tinkVersion by extra("1.2.2")
val zxingVersion by extra("3.4.0")

subprojects {
  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_12.majorVersion
    }
  }
}

// TODO vihang port this from groovy to kts
//tasks.withType<DependencyUpdates> {
//  resolutionStrategy = Action {
//    componentSelection { rules ->
//      rules.all { ComponentSelection selection ->
//        val rejected:Boolean = ["alpha", "beta", "rc", "cr", "m", "redhat"].any { qualifier ->
//          selection.candidate.version ==~ /(?i).*[.-]${qualifier}[.\d-]*/
//        }
//        if (rejected) {
//          selection.reject("Release candidate")
//        }
//      }
//    }
//  }
//}
