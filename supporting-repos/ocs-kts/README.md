# ocs-kts
Kotlin Scripts for ocs-ktc project in ostelco-core repo.

For the repo [ostelco-core](https://github.com/ostelco/ostelco-core), since we do not want to keep our business logic in
an open source project, that logic is moved to Kotlin script files.
  
Those kotlin scripts for the project [ocs-ktc](https://github.com/ostelco/ostelco-core/tree/develop/ocs-ktc)
in `ostelco/ostelco-core` repo are kept in this repo.

Files in this repo are optional for the working of `ostelco/ostelco-core` project.
 
If you want to work on these files, you need to checkout this repo in `src/main/resources` of `ocs-ktc`
project of `ostelco/ostelco-core`.

This repo is dependent on _interfaces_ defined in `ostelco/ostelco-core` project.  So, you will get compile errors if
this repo is not checkout as mentioned in previous step.
