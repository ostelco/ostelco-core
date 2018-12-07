# Exploratory code for understanding the eSIM protocols

Based on information from
https://www.gsma.com/newsroom/wp-content/uploads/SGP.22-v2.0.pdf

... Can the JSON files be interpreted as swagger spec, and be
converted into a server/client implementation?  If so can we use it to
flesh out the interaction between the BSS and the SM-DP+?

Use json schema files (stored in the resources directory of the
source code) to verify that the input / output is conformant to the
GSMA spec.  Use jackson's ordinary methods for handling incoming
requests and replies.

TODO (Prioritized)
---
* Make class comments for every high level class.

* Make junit-tests that runs the dropwizard application in a mode to permit it
  to accept TSL connections that are signed by a self-signed certificate,
  using a certificate chain where the signing authority is included.  The tests
  should as far as possible use standard configurations of both 
  dropwizard and the jersey client to make this happen.  A script describing
  how to generate new signed certificates should also be included.

  Look here for description on how to make this happen
  https://stackoverflow.com/questions/34908947/dropwizard-client-certificate-authentication-via-httpclient-key-trust-store


TODO (not prioritized)
---

* Think _very_hard_  about how to make an acceptance test for this thing.   It should
   * Have a minimum of different executables.
   * Preferably run in Docker Compose, but kubernetes is also an option.
   * Generate a clear-cut test result, preferably in junit, preferable with coverage.
   * Should be simple to run from the command line, no magic necessary.

* General refactoring, weed out TODOs, make constants, set protocol versions
  correctly etc.

* Authentication:
   * Design certificate structure (SM-DP+, CSR  created by whom, signed by whom
     etc.)   Figure out how and where these certs should be stored.
   * Same thing for HSS.
   * Make test certificates that are self-signed and are included in the
     acceptance tests.
   * Document the whole procedure using markdown and plant uml.

* Make a docker compose testable ensemble of executables for acceptance test. 

 *  Must contain:

    * A sim inventory service, the test article, with a config file.
    * SIM admin database, running in postgres, and sufficient script support to
      start it up with the appropriate database schema installed.
    * SM-DP+ server. Very simplified, serving
      a highly restricted set of usecases.   Must be capable of reporting
      back to client about what happens.   Must therefore use a valid
      certificate structure.
    * An HSS emulator, that the sim inventory can talk to to enable
      sim profiles.
    * An .out file that can be injected into the Sim admin database
      as the first step of the acceptance test.
    * A script emulating the behavior of Prime talking to the
      the SIM inventory.  Not necessarily Prime, but should use
      the same libraries that we intend to be used from Prime.
    * Some mechanism that will deliver the results of running the acceptance
      tests to a surrounding environment that can determine if the test
      ran to  completion or not.

 *  Nice to have:

    * Prometheus integration

* Return channel for ES2+

   * Figure out where we want to place the return channel for ES2+
   * Set up the certificates, CSRs etc.
   * Set up a first version that does nothing but receiving the information
     and write it to a log (that is then picked up by the standard
     logging interfaces).  This should be a version of the sim inventory server,
     but it should run in a mode that is made just for this usecase.
   * Later (possibly much later), write code that mutates the common
     data model, or publishes updates to an internal bus of some sort.


* Make code that generates QR Code as picture (png?) that can be embedded
  in an user interface.

* Code walkthrough of usecases, all the way down into database structures.
   * Must be done asap by the dev team.

* Documentation
  * The documentation should by and large be written in markdown and 
    plant UML.
  * A domain model for the SIM cards and their life cycle.
  * API documentation with all parameters throroughly described, and 
    examples of use.

* Instrumentation
  * Expose metrics to Prometheus via standard techniques

* Integration to live test-SM-DP+
  * Complete an SM-DP+ communications class that contains both health
    check and metrics for communications overhead. Use standard 
    dropwizard mechanisms for this.
  
  * Figure out how to do heartbeat. Must ask SM-DP+ vendor about this.  Must be 
    done over ES2+ protocol.

  * First goal to aim for is to provision a test profile to a live commercial
    handset.

* Integration to live HSS
  * Authentication
  * Heartvbeat
  * Instrumentation
  * Synchronization with datamodel (misc. states for SIM).
   

* Sync design of internal API with requirements from user interface flow
   => ... then ruthlessly prune elements that are not required for
   UX or testing.


Ideas
---

* Think this through: Would it make sense to have emulators for
  the HSS and SM-DP+es we will integrate to, just to see that the 
  system is capable of connecting to them using the appropriate
  set of certificates etc.?

* Follow through on https://rohannagar.github.io/2018-04-11/dropwizard-on-kubernetes

* docker build -t ostelco/simadmin . 
  ... to build a new version of the simadmin thingy that is
  also reachable by kubernetes locally.

* Re-enable json schema validation on  everything, fixing the broken 
  schemas in the process.

* Make some refactoring in the data structures. 

* Use the generated openapi spec to generate clients.

* Add  and test two-way SSL authentication.

* Make test-runs of some typical client/SM-DP+ interactions,
  possibly logging them using plant uml autogenerated
  diagrams.

* Connect to proper SM-DP+, watch everything explode
  ... observe fix repeat until it's working.

* Integrate into a proper workflow for subscriber signup,
  possibly involving generation of QR-codes :-)

* Figure out how to integrate with prime, possibly making the whole thing
  a part of prime.

* Figure out how to expose endpoints using google cloud endpoints, and
  preserving authentication for incoming ES2+ callbacks.



How to start the es2plus application
---

1. Run `mvn clean install` to build your application
1. Start application with `java -jar target/esim-1.0-SNAPSHOT.jar server config.yml`
1. To check that your application is running enter url `http://localhost:8080`

Health Check
---

To see your applications health enter url `http://localhost:8081/healthcheck`


Secret password for keystore used in testing is "secret"