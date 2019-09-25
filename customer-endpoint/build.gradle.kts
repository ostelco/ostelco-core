import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))

  testImplementation("io.dropwizard:dropwizard-testing:${Version.dropwizard}")
  testImplementation("org.mockito:mockito-core:${Version.mockito}")
  testImplementation("org.assertj:assertj-core:${Version.assertJ}")

  testImplementation("io.jsonwebtoken:jjwt-api:${Version.jjwt}")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:${Version.jjwt}")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:${Version.jjwt}")

  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:${Version.mockitoKotlin}")
  testImplementation("net.bytebuddy:byte-buddy:${Version.byteBuddy}") {
    because("mockito-kotlin:2.2.0 has byte-buddy:1.9.0 which does not work for java13")
  }
  testImplementation("net.bytebuddy:byte-buddy-agent:${Version.byteBuddy}") {
    because("mockito-kotlin:2.2.0 has byte-buddy:1.9.0 which does not work for java13")
  }
}

apply(from = "../gradle/jacoco.gradle.kts")