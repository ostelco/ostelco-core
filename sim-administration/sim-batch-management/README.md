# Sim logistics management

This directory contains code for managing orders of SIM cards, 
sending these orders to various actors, and injecting them into
various subsystems.

It is currently in  a state of flux, but will evolve in a bottom
up fashion driven by daily demands to perform the tasks the code
in this directory assists in.

The code in this directory is written in golang.  Currently there are two
go programmes here, both designed to be run from the command line.

 *  outfile_to_hss_input_converter.go:  Will convert output from 
    a SIM card vendor into  input for a HSS vendor.
    
 *  upload-sim-batch.go: Will from command line parameters generate a 
    bash script that will use curl to upload sim card parameters
    to a "prime" instance.
    
For both of these programmes, see the source code, in particular the
comments near the top of the files for instructions on how to use them.

##To build everything

Before we build, some things neds to be in order.

### Prerequisites

 * Go has to be installed on the system  being run.
  
 * Prime needs to be accessible via ssh tunnel or otherwise from the host
    where the script is being run.

### Building

   ./build-all.sh

... will compile and test the program and leave an executable called
"sbm" in the current directory

   . ./build-all.sh

... will compile and test the program, then if you're running bash
extend your shell with command line extensions for the sbm program.

## Some common usecases

### How to upload batch information to prime

#### Introduction

Prime has  REST endpoint for uploading sim batches.   This is an 
interface with little error checking (beyond the bare miniumums)
and a low abstraction layer: It requires a CSV file of ICCID/IMSI/MSISDN/PROFILE tuples.

This is convenient as a starting point, but in practice it has turned
out to be a little too simple, hence the script upload-sim-batch.go.

This script takes assumes that there is already a way to talk HTTP 
(no encryption) to the upload profiles.  The default assumption is that
a tunnel has been set up from localhost:8080 to somewhere more
appropriate, but these coordinaters can be tweaked using command line
parameters.

The basic REST interface assumes an incoming .csv file, but that is
bulky, and the information content is low.  In practice we will 
more often than not know the ranges of IMSI, ICCID and MSISDN numbers
involved, and obviously also the profile type names.  The script can
take these values as parameters and generate a valid CSV file, and
upload it automatically.

The parameters are checked for consistency, so that if there are 
more MSISDNs than ICCIDs in the ranges given as parameters, for instance,
then the script will terminate with an error message.

(these are reasonable things to check btw, errors have been made
that justifies adding these checks).

###A typical invocation looks like this:

  TBD

##TODO

1. Create a very clean PR for future code review.

2. Write up a nice markdown documentation describing common usecases.
 
3. Add crypto resources so that the program can talk to external parties.
 
4. Add code to activate profiles in HSS (if API is known)
 
5. Add config for crypto parameters  for HSSes, profile-vendors and operators (sftp in particular)
 
6. Add misc. parameters about sim vendors, HSSes, Prime instances etc., so that
   batches can be properly constrained, defaults set the right way and external
   components accessed from gocode.
 
7. Figure out how to handle workflows. Be explicit!
 
8. The interfaces to external parties will be
    - input/output files for profile generation.
    - some kind of file (not yet determined) for msisdn lists.
    - HTTP upload commands, either indirectly via curl (as now), or
      directly from the script later.   In either case 
      it will be assumed that tunnels are set up out of band, and
      tunnel setup is not part of this program.
 
9. Declare legal hss/dpv combinations, batches must use legal combos.
 
10. Declare prime instances (should make sense to have both prod and dev defined
   with different constraints on them).

11. Read througn https://github.com/alecthomas/kingpin/blob/master/parsers.go and
    amend the command line reader to read existing files, proper DNS etc.
    as appropriate.

12. Refactor the main program. It's way to big now.
