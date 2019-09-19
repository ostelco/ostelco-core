plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {

  val kotlinVersion:String by rootProject.extra

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  implementation ("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion") {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
  }
  api(project(":diameter-stack"))
}