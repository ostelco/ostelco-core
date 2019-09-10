# How to upload batch information to prime using the 

## Introuduction
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

##Prerequisites

* Go has to be installed on the system  being run.
* Prime needs to be accessible via ssh tunnel or otherwise from the host
  where the script is being run.


##A typical invocation looks like this:


(The parameters below have correct lengths, but are otherwise bogus,
and will cause error messages.)

```
 ./upload-sim-batch.go \
      -first-iccid 1234567678901234567689 \
      -last-iccid 1234567678901234567689 \
      -first-imsi 12345676789012345 \
      -last-imsi 12345676789012345 \
      -first-msisdn 12345676789012345 \
      -last-msisdn 12345676789012345 \
      -profile-type gargle-blaster-zot \
      -profile-vendor idemalto \
      -upload-hostname localhost \
      -upload-portnumber 8080
```

##The full set of  command line options

```

  -first-iccid string
    	      An 18 or 19 digit long string.  The 19-th digit being a luhn luhnChecksum digit, if present (default "not  a valid iccid")
  -first-imsi string
    	      First IMSI in batch (default "Not a valid IMSI")
  -first-msisdn string
    		First MSISDN in batch (default "Not a valid MSISDN")
  -hss-vendor string
    	      The HSS vendor (default "M1")
  -initial-hlr-activation-status-of-profiles string
    					     Initial hss activation state.  Legal values are ACTIVATED and NOT_ACTIVATED. (default "ACTIVATED")
  -last-iccid string
    	      An 18 or 19 digit long string.  The 19-th digit being a luhn luhnChecksum digit, if present (default "not  a valid iccid")
  -last-imsi string
    	     Last IMSI in batch (default "Not a valid IMSI")
  -last-msisdn string
    	       Last MSISDN in batch (default "Not a valid MSISDN")
  -profile-type string
    		SIM profile type (default "Not a valid sim profile type")
  -profile-vendor string
    		  Vendor of SIM profiles (default "Idemia")
  -upload-hostname string
    		   host to upload batch to (default "localhost")
  -upload-portnumber string
    		     port to upload to (default "8080")

```



