#!/bin/bash




# First we want the thing to compile
go build

if [ "$?" -ne "0" ]; then
  echo "Sorry compilation failed aborting build."
  exit 1
fi



# THen to pass tests
go test ./...

if [ "$?" -ne "0" ]; then
  echo "Sorry, one or more tests failed, aborting build."
  exit 1
fi

# Then... 
# somewhat  nonportably ... run static analysis of the
# go code.

 ~/go/bin/staticcheck ./...




# If sourcing this script, then the line below
# will modify command line compesion in bash

if [[ $_ != $0 ]] ; then
    rm -f /tmp/tmp.db
    eval "$(SIM_BATCH_DATABASE=/tmp/tmp.db ./sbm  --completion-script-bash)"
fi
