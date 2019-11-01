An informal TODO list for the sim batch management tool
==

1. Make persistence-model for generated sim profiles.  This database should then be used
   to generate uploads of various kinds to prime.  This essentially means mirroring
   and extending the data model for sim profiles currently in Prime.
1. Rewrite upload-sim-batch-lib-test.go to be part of sim-batch-mgt.go,
   during that process:
   * Persist the batch data.
   * List available batches, show the status.
   * Make a command that can print the input file for that batch.
   * Find some clever way to get legacy batches into the database without typing too much.
1. Add crypto resources so that the program can talk to external parties.
1. Figure out how to handle workflows. Be explicit!
1. Handle both parameterized lists of MSISDNs and list-based input.
1. The interfaces to external parties will be
    - input/output files for profile generation.
    - some kind of file (not yet determind) for msisdn lists.
    - HTTP upload commands, either indirectly via curl (as now), or
      directly from the script later.   In either case 
      it will be assumed that tunnels are set up out of band, and
      tunnel setup is not part of this program.
