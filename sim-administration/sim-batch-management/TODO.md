TODO
==
1. Make the build-all script run without errors (including linter errors)
1. Clean up the code a lot
1. Take pending code review comments into account.
1. Create a very clean PR for future code review.
1. Compress the utility scripts into very close to nothing, by adding their functionality to the .go code.
1. Add crypto resources so that the program can talk to external parties.
1. Add misc. parameters about sim vendors, HSSes, Prime instances etc., so that
   batches can be properly constrained, defaults set the right way and external
   components accessed from gocode.
1. Figure out how to handle workflows. Be explicit!
1. Handle both parameterized lists of MSISDNs and list-based input.
1. The interfaces to external parties will be
    - input/output files for profile generation.
    - some kind of file (not yet determined) for msisdn lists.
    - HTTP upload commands, either indirectly via curl (as now), or
      directly from the script later.   In either case 
      it will be assumed that tunnels are set up out of band, and
      tunnel setup is not part of this program.

