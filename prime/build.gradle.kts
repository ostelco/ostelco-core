import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  application
  id("com.github.johnrengelman.shadow")
  idea
}

// Update version in [script/start.sh] too.
version = "1.66.0"

dependencies {
  // interface module between prime and prime-modules
  api(project(":prime-modules"))

  // prime-modules
  runtimeOnly(project(":appleid-auth-service"))
  runtimeOnly(project(":ocs-ktc"))
  runtimeOnly(project(":document-data-store"))
  runtimeOnly(project(":neo4j-store"))
  runtimeOnly(project(":customer-endpoint"))
  runtimeOnly(project(":ekyc"))
  runtimeOnly(project(":graphql"))
  runtimeOnly(project(":admin-endpoint"))
  runtimeOnly(project(":app-notifier"))
  runtimeOnly(project(":customer-support-endpoint"))
  runtimeOnly(project(":email-notifier"))
  runtimeOnly(project(":payment-processor"))
  runtimeOnly(project(":analytics-module"))
  runtimeOnly(project(":slack"))
  runtimeOnly(project(":imei-lookup"))
  runtimeOnly(project(":jersey"))
  runtimeOnly(project(":secure-archive"))
  runtimeOnly(project(":scaninfo-datastore"))
  runtimeOnly(project(":sim-administration:simmanager"))
  runtimeOnly(project(":tracing"))

  runtimeOnly(project(":logging"))

  implementation("io.dropwizard:dropwizard-http2:${Version.dropwizard}")
  runtimeOnly("io.dropwizard:dropwizard-json-logging:${Version.dropwizard}")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}")
  implementation("com.google.guava:guava:${Version.guava}")
  implementation("org.dhatim:dropwizard-prometheus:${Version.prometheusDropwizard}")

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation("org.mockito:mockito-core:${Version.mockito}")
  testImplementation("com.lmax:disruptor:3.4.2")
  testImplementation("com.palantir.docker.compose:docker-compose-rule-junit4:${Version.dockerComposeJunitRule}")
  testImplementation("org.dhatim:dropwizard-prometheus:${Version.prometheusDropwizard}")
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
  environment("GOOGLE_APPLICATION_CREDENTIALS", "config/prime-service-account.json")
  testClassesDirs = sourceSets.getByName("integration").output.classesDirs
  classpath = sourceSets.getByName("integration").runtimeClasspath
}

tasks.build.get().dependsOn(integration)

application {
  mainClassName = "org.ostelco.prime.PrimeApplicationKt"
}

tasks.withType<ShadowJar> {
  mergeServiceFiles()
  archiveClassifier.set("uber")
  archiveVersion.set("")
  isZip64 = true
}

tasks.register("version") {
  doLast {
    println(version)
  }
}

tasks.test {
  testLogging {
    exceptionFormat = FULL
    events("PASSED", "FAILED", "SKIPPED")
  }
}

idea {
  module {
    testSourceDirs = testSourceDirs + file("src/integration-test/kotlin")
  }
}