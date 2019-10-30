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


# TODO
* Make a build command that runs tests and reports test coverage (etc),
  make it part of the "build-all.go" script.
* Write some code that allows sim-batches to be kept in database.
  - Pick up the location of the database and also other
    secrets via an environment variable.
  - Figure out if we can make multiple command-line programs that have equal value




