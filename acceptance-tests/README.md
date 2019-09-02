#Acceptance tests

PRELIMINARY, EXTREMELY INCOMPLETE DOCUMENTATION, MUST BE IMPROVED!

The ostelco system is complex in that it consists of multiple components that
communicate both with internal components and external components.

We attempt to test all contingencies in unit and acceptance tests, but
since both of these types of tests are designed to be quick, and to
test a single component rather than the whole system, we have also
built an "acceptance test" that will test all parts of the system, in
a context where all the external components are also present either as
externally hosted test instances, or as mocked out-components running
locally.

The acceptance tests themselves are run using "docker compose", and
are implemented as a separate docker container that is run in the
docker compose environment.

## Docker compose files

## The various docker compose files
There are in fact multiple docker compose  files present in the
top level directory, and they have different usecases:


 *  docker-compose.yaml: The most commonly used file.  Will 
    run a set of tests for most of the components.

 *  docker-compose.esp.yaml: A test that is simplar to docker-compose.yaml,
    but also includes the google ESP components. We usually don't run this test
    since the complications of running ESP for the most part don't outweigh
    its utility.

 *  docker-compose.ocs.yaml:  tbd
 *  docker-compose.seagull.yaml: tbd

## Structure of the docker compose files

## Components started by docker compose

... tbd (also a PUML diagram showing call relationships)


## Prerequisites for running the acceptance tests
 
  ... tbd


## How to run the acceptance tests

  ... tbd, but should include:   Just running them, running them while developing new tests, how to attach  a test being developed to an IDE's debugger.






