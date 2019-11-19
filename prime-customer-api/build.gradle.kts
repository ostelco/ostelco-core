import org.hidetake.gradle.swagger.generator.GenerateSwaggerCode
import org.ostelco.prime.gradle.Version

plugins {
  `java-library`
  id("org.hidetake.swagger.generator") version "2.18.1"
  idea
}

var generateSwaggerCode: GenerateSwaggerCode? = null

// gradle generateSwaggerCode
swaggerSources.create("java-client") {
  setInputFile(file("${projectDir}/../prime/infra/dev/prime-customer-api.yaml"))
  generateSwaggerCode = code(closureOf<GenerateSwaggerCode> {
    language = "java"
    configFile = file("${projectDir}/config.json")
  })
}

generateSwaggerCode?.also { generatedCode ->
  tasks.compileJava.get().dependsOn(generatedCode)
  sourceSets.main.get().java.srcDir("${generatedCode.outputDir}/src/main/java")
  sourceSets.main.get().resources.srcDir("${generatedCode.outputDir}/src/main/resources")
}

dependencies {
  swaggerCodegen("io.swagger:swagger-codegen-cli:${Version.swaggerCodegen}")

  implementation("javax.annotation:javax.annotation-api:${Version.javaxAnnotation}")

  // taken from build/swagger-code-java-client/build.gradle
  implementation("io.swagger:swagger-annotations:1.6.0")
  implementation("com.google.code.gson:gson:${Version.gson}")
  implementation("com.squareup.okhttp:okhttp:2.7.5")
  implementation("com.squareup.okhttp:logging-interceptor:2.7.5")
  implementation("io.gsonfire:gson-fire:1.8.3")
  implementation("org.threeten:threetenbp:1.4.0")
  testImplementation("junit:junit:4.12")
}

idea {
  module {
    // generatedSourceDirs
    sourceDirs.addAll(files("${project.buildDir.path}/swagger-code-java-client/src/main/java"))
  }
}