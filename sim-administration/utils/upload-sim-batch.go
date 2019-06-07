//usr/bin/env go run "$0" "$@"; exit "$?"

package main

import (
	"flag"
	"fmt"
)

func main() {
	parseCommandLine()
	// tbd:
	//   *  Get batch data from parsing of command line.
	//   *  Translate batch into CSV entry (either in memory, or as a function
	//      that can provide every line on demand).
	//   *  Upload batch to backend server, give copious feedback
	//      if it fails for whatever reason.
}

func parseCommandLine() {

	//
	// Set up command line parsing
	//
	firstIccid := flag.String("first-iccid",
		"not  a valid iccid",
		"An 18 or 19 digit long string.  The 19-th digit being a luhn checksum digit, if present")
	lastIccid := flag.String("last-iccid",
		"not  a valid iccid",
		"An 18 or 19 digit long string.  The 19-th digit being a luhn checksum digit, if present")
	firstIMSI := flag.String("first-imsi", "Not a valid IMSI", "First IMSI in batch")
	lastIMSI := flag.String("last-imsi", "Not a valid IMSI", "Last IMSI in batch")
	firstMsisdn := flag.String("first-msisdn", "Not a valid MSISDN", "First MSISDN in batch")
	lastMsisdn := flag.String("last-msisdn", "Not a valid MSISDN", "Last MSISDN in batch")
	profileType := flag.String("profile-type", "Not a valid sim profile type", "SIM profile type")

	// XXX Missing: URL of endpoint to upload batch to.

	//
	// Parse input according to spec above
	//
	flag.Parse()

	//
	// Check parameters for syntactic correctness and
	// semantic sanity.
	//

	// tbd :(field integrity, if 19 digit ICCID, check and then remove
	// luhn checksum. Check that ranges are ranges, and span the same number
	// of entities, if at all possible, check that ranges are within constraints
	// set by some external database of valid ranges (typically for an operator)
	// check that the profile type can be found in some config file somewhere.

	// tbd: If all of the above is kosher, then create a record representing the
	//      batch, that entry is sent back as a return value.

	fmt.Println("first-iccid:", *firstIccid)
	fmt.Println("last-iccid:", *lastIccid)
	fmt.Println("first-msisdn:", *firstMsisdn)
	fmt.Println("last-msisdn:", *lastMsisdn)
	fmt.Println("first-imsi:", *firstIMSI)
	fmt.Println("last-imsi:", *lastIMSI)
	fmt.Println("profile-type:", *profileType)
	fmt.Println("tail:", flag.Args())
}
