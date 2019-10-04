plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect").toString()) {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
  }
  api(project(":diameter-stack"))
}