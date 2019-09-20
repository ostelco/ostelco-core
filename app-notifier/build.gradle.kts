plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))
  implementation(project(":firebase-extensions"))
}