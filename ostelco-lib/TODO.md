
* Make maven repo generate a locally installed library
  https://docs.gradle.org/current/userguide/maven_plugin.html#sub:installing_to_the_local_repository

* Verify that the local library actually works, by start
  using it in the prime subproject.

* Write a gradle rule to start committing to maven central, preferably using SSH.

  - http://www.sonatype.org/nexus/2015/01/08/deploy-to-maven-central-repository/
  - https://issues.sonatype.org/browse/OSSRH-37143
  - Get someone else to register an user and be able to upload.

* Set up the gradle rule.


* Register ostelco.org or something like that. [done]

* Start using ostelco.org

* Use this to debug the gradle script: https://georgik.rocks/how-to-debug-gradle-script/
