tasks {
    named<JacocoReport>("jacocoTestReport") {
        group = "Reporting"
        description = "Generate Jacoco coverage reports after running tests."
        additionalSourceDirs.from(files(project.the<SourceSetContainer>()["main"].allJava.srcDirs))
        reports {
            xml.isEnabled = true
            csv.isEnabled = false
            html.isEnabled = true
            html.destination = file("${buildDir}/jacocoHtml")
        }
    }
}

tasks.named("check").get().dependsOn(tasks.named("jacocoTestReport"))