#!/usr/bin/env bash

# This script checks if current branch is master

if [ master != $1 ];
then
    echo "Aborting. '$1' is not 'master' branch."
    exit 1;
fi
exit 0;