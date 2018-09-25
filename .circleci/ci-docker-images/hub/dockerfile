# This docker image is pushed manually to Google Container Registry
# It is pushed as: eu.gcr.io/pi-ostelco-dev/github-hub:<hub version>
# It is used for creating -and operating on- PRs.
# Hub requires an environment variable GITHUB_TOKEN to authenticate when creating a PR.
# Alternative authentication ways can be found on https://hub.github.com/hub.1.html

FROM fedora

RUN dnf -yq install hub

