#!/bin/bash


DEPENDENCIES="gradle docker-compose"


#
# Do we have the dependencies (in this case only gradle, but copy/paste
# made the test more generic .-)
#
for dep in $DEPENDENCIES ; do 
 if [[ -z "$(which $dep)" ]] ; then
   echo "Couldn't find dependency $dep"
   exit 1
 fi
done




#
# Do we have the necessary environment variables set
# to run payment tests?
#

if [[ -z "$STRIPE_API_KEY" ]] ; then
    echo "$0 ERROR: STRIPE_API_KEY is not set.  Se instructions in docs/TEST.md for how to get one."
    exit 1
fi

if [[ -z "$STRIPE_ENDPOINT_SECRET" ]] ; then
    export  STRIPE_ENDPOINT_SECRET=thisIsARandomString
    echo "$0 INFO: Couldn't find variable STRIPE_ENDPOINT_SECRET, setting it to dummy value '$STRIPE_ENDPOINT_SECRET'"
fi


#
# Cd to script directory
#

cd $(dirname $0)

#
# Then start running the build
#

gradle clean build

#
# If that didn't go too well, then bail out.
#

if [[ $? -ne 0 ]] ; then echo 
   echo "Compilation failed, aborting"
   exit 1
else
   echo "$0 INFO: Building/unit tests went well, Proceeding to acceptance tests."
fi


docker-compose up --build --abort-on-container-exit
