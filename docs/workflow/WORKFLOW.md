#  Describe the workflow to generate both artefacts for distribution through maven central

![workflow](workflow.png)

We use a variation of the ["gitflow"](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow) workflow, where all work is done in feature branches,  which are merged into the "develop" branch.   Snapshot releases are done from the "develop" branch.  The master branch only ever gets merges from the develop branch, and major releases (non snapshot) are done from the master branch.