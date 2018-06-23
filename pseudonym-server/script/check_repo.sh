#!/usr/bin/env bash

set -e

if [ -z "$1" ] || [ -z "$2" ]; then
  (>&2 echo "Usage: check_repo.sh <module name> <git tag>")
  exit 1
fi

module=$1
tag=$2

echo "Checking if '$tag' exists in master branch"

if [ ! -f "$module/script/deploy.sh" ]; then
    (>&2 echo "Run this script from project root dir (ostelco-core)")
    exit 1
fi

CHECK_REPO="$module/script/check_repo.sh"

if [ ! -f ${CHECK_REPO} ]; then
    (>&2 echo "Missing file - $CHECK_REPO")
    exit 1
fi

command -v git >/dev/null 2>&1 || { echo >&2 "Git not available, Aborting."; exit 1; }

# Find if the commit for the git tag exist in master branch.

# We need a full checkout to search the tag.
git clone https://github.com/ostelco/ostelco-core.git
cd ostelco-core
git checkout master

tag_commit=$(git rev-list -n 1 $tag)
echo "Searching for '$tag_commit'"

if [ -z "$tag_commit" ]; then
  (>&2 echo "Cannot find commit for '$tag'")
  exit 1
fi

# Look if the commit is in master (reachable by first-parent, so not deep)
if git rev-list --first-parent master | grep $tag_commit >/dev/null; then
    (>&2 echo "$tag points to a commit in master")
else
    (>&2 echo "$tag does not point to a commit in master")
    exit 1
fi

# remove the new checkout.
cd ..
rm -rf ostelco-core

exit 0
