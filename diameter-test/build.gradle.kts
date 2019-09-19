plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra

  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect").toString()) {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
  }
  api(project(":diameter-stack"))
}