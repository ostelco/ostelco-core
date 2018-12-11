#!/bin/bash


DEPENDENCIES="mvn docker"

for dep in $DEPENDENCIES ; do 
 if [[ -z "$(which $dep)" ]] ; then
   echo "Couldn't find dependency $dep"
   exit 0
 fi
done


#
# Build all the maven artefacts
#

mvn clean install

#
# If that didn't go too well, then bail out.
#

if [[ $? -ne 0 ]] ; then echo 
   echo "Compilation failed, aborting"
   exit 1
fi

#
# Build the dockerfile for the simadmin
#
docker build --file dockerfiles/Dockerfile.simadmin --tag ostelco/simadmin .