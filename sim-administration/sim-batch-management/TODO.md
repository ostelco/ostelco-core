An informal TODO list for the sim batch management tool
==

1. Make an RDBMS that handles sim card workflows.
2. It must by necessity be able to handle free lists of 
   imsis, msisdns etc.
3. As today, it should be possible to -generate- those lists
   from parameters, where that makes sense. In general howeve,r
   in particular for production use, this will not be the case
   and we need to cater for that.
4. The programme should -initially- be wholly command line
   oriented, with a database using sqlite.
5. At some (much) later stage, it may make sense to put it
   in some cloud, somewhere.
6. The interfaces to external parties will be
    - input/output files for profile generation.
    - some kind of file (not yet determind) for msisdn lists.
    - HTTP upload commands, either indirectly via curl (as now), or
      directly from the script later.   In either case 
      it will be assumed that tunnels are set up out of band, and
      tunnel setup is not part of this program.
