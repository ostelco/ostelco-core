An informal TODO list for the sim batch management tool
==

1. Ingest input files into a corresponding batch, read all the sim 
   profiles into a sim profile table.  This table can then be enriched
   with access codes, MSISDNs etc, and then written to both HSSes and 
   be transformed into various types of upload files.
1. Rewrite upload-sim-batch-lib-test.go to be part of sim-batch-mgt.go,
   during that process:
   * Persist the batch data [Done]
   * List persisted batches, show the status (use json).
   * Persist access parameters for Prime(s) (prod, and pre-prod) (indirectly).
   * Persist access parameters for sim vendors and HSSes
   * Check referential integrity in data model so that batches don't refer to
     HSSes or other types of entities.
   * From the batch information, generate
       - Input file to SIM vendor.
       - Upload file for Prime instances.
   * From output file, generate input file for HSS without storing Ki values.    
1. Add crypto resources so that the program can talk to external parties.
1. Figure out how to handle workflows. Be explicit!
1. Handle both parameterized lists of MSISDNs and list-based input.
1. The interfaces to external parties will be
    - input/output files for profile generation.
    - some kind of file (not yet determined) for msisdn lists.
    - HTTP upload commands, either indirectly via curl (as now), or
      directly from the script later.   In either case 
      it will be assumed that tunnels are set up out of band, and
      tunnel setup is not part of this program.



Notes
==

      simmgr_inventory=> select count(*) from sim_entries where profile = 'OYA_M1_STANDARD_ACB' and smdpplusstate = 'RELEASED'; 
 