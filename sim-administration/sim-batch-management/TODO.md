TODO
==

1. Use a different database than hardcoded "foobar.db" :-) Read it from an
    environment variable or something at least a bit portable.
1. Write up a nice markdown documentation describing common usecases.
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
      
 	// TODO: Some command to list all profile-vendors, hsses, etc. , e.g. lspv, lshss, ...
 	// TODO: Add sftp coordinates to be used when fetching/uploding input/utput-files
 	// TODO: Declare hss-es, that can be refered to in profiles.
 	// TODO: Declare legal hss/dpv combinations, batches must use legal combos.
 	// TODO: Declare contact methods for primes.  It might be a good idea to
 	//        impose referential integrity constraint on this too, so that
 	//        profile/vendor/hss/prime combos are constrained.  It should be possible
 	//        to specify prod/dev primes.

