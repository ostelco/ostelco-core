TODO
==


1. Create a very clean PR for future code review.
1. Write up a nice markdown documentation describing common usecases.
1. Add crypto resources so that the program can talk to external parties.
1. Add config for crypto parameters  for HSSes, profile-vendors and operators (sftp in particular)
1. Add misc. parameters about sim vendors, HSSes, Prime instances etc., so that
   batches can be properly constrained, defaults set the right way and external
   components accessed from gocode.
1. Figure out how to handle workflows. Be explicit!
1. The interfaces to external parties will be
    - input/output files for profile generation.
    - some kind of file (not yet determined) for msisdn lists.
    - HTTP upload commands, either indirectly via curl (as now), or
      directly from the script later.   In either case 
      it will be assumed that tunnels are set up out of band, and
      tunnel setup is not part of this program.
1. Declare legal hss/dpv combinations, batches must use legal combos.
1. Declare prime instances (should make sense to have both prod and dev defined
   with different constraints on them).


