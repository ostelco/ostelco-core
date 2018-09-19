# Github Hub docker image

- This docker image is pushed manually to Google Container Registry
- It is pushed as: eu.gcr.io/pi-ostelco-dev/github-hub:<hub version>
- It is used for creating -and operating on- PRs.
- Hub requires an environment variable GITHUB_TOKEN to authenticate when creating a PR.
- Alternative authentication ways can be found on https://hub.github.com/hub.1.html
- For instructions on pushing the image, please refer to the [GCR official documentation](https://cloud.google.com/container-registry/docs/pushing-and-pulling)