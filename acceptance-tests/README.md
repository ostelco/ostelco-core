# Acceptance tests

_PRELIMINARY, EXTREMELY INCOMPLETE DOCUMENTATION, MUST BE IMPROVED!_

The ostelco system is complex in that it consists of multiple components that
communicate both with internal components and external components.

We attempt to test all contingencies in unit and acceptance tests, but
since both of these types of tests are designed to be quick, and to
test a single component rather than the whole system, we have also
built an "acceptance test" that will test all parts of the system, in
a context where all the external components are also present either as
externally hosted test instances, or as mocked out-components running
locally.

The acceptance tests themselves are run using `docker compose`, and are
implemented as a separate docker container that is run in the docker
compose environment.

## Docker compose files

### The various docker compose files

There are in fact multiple docker compose  files present in the
top level directory, and they have different usecases:

 *  `docker-compose.yaml`: The main file.  Will run a set of tests exercising
    most of the components.

 *  `docker-compose.esp.yaml`: A test that is similar to `docker-compose.yaml`,
    but also includes the google ESP components. We usually don't run this test
    since the complications of running ESP for the most part don't outweigh
    its utility.

 *  `docker-compose.ocs.yaml`:  tbd

 *  `docker-compose.seagull.yaml`: tbd

### Structure of the docker compose files

### Components started by docker compose

... tbd (also a PUML diagram showing call relationships)

## Prerequisites for running the acceptance tests

... tbd

## How to run the acceptance tests

For the main Docker compose file:

    $ docker-compose up --build --abort-on-container-exit

To run one of the other Docker compose files, use the `-f` option. Example:

    $ docker-compose -f docker-compose.esp.yaml up --build --abort-on-container-exit

... tbd, how to run them while developing new tests, how to attach  a test
being developed to an IDE's debugger.

## Running tests that depends on webhooks configured at Stripe

Currently the "recurring payment" tests depends on webhooks being enabled at Stripe
and the events being proxy forwarded the Prime backend. For enabling proxy forward
of the events the
[stripe/stripe-cli](https://hub.docker.com/r/stripe/stripe-cli) Docker image is used.

For the tests to work the following two evnironment variables must be set to their
correct value.

 - `STRIPE_API_KEY`
 - `STRIPE_ENDPOINT_SECRET`

For the `STRIPE_API_KEY` variable go to the Stripe console and list the value at
Developer -> API keys -> Secret key.

To get the correct `STRIPE_ENDPOINT_SECRET` value do as follows:

    $ export STRIPE_API_KEY=<secret value obtained earlier>
    $ docker run --rm -e STRIPE_API_KEY=$STRIPE_API_KEY stripe/stripe-cli listen
    Checking for new versions...

    Getting ready...
    Ready! Your webhook signing secret is whsec_secretvaluesecretvalue0123456789 (^C to quit)

Alternatively download the `stripe` command line program from
[https://stripe.com/docs/stripe-cli](https://stripe.com/docs/stripe-cli) and run
the command:

    $ stripe listen

(with the `STRIPE_API_KEY` environment variable set).

Set the `STRIPE_ENDPOINT_SECRET` environment variable to the string starting with
"`whsec_`".

    $ export STRIPE_ENDPOINT_SECRET=whsec_secretvaluesecretvalue0123456789

This will cause the tests that depends upon Stripe events to run.

To disiable the tests, set the `STRIPE_ENDPOINT_SECRET` to some dummy value that
don't start with the "`whsec_`" string.
