//usr/bin/env go run "$0" "$@"; exit "$?"

package main

import (
	"flag"
	"fmt"
)

func main() {

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

	var svar string
	flag.StringVar(&svar, "svar", "bar", "a string var")

	flag.Parse()

	fmt.Println("first-iccid:", *firstIccid)
	fmt.Println("last-iccid:", *lastIccid)
	fmt.Println("first-msisdn:", *firstMsisdn)
	fmt.Println("last-msisdn:", *lastMsisdn)
	fmt.Println("first-imsi:", *firstIMSI)
	fmt.Println("last-imsi:", *lastIMSI)
	fmt.Println("profile-type:", *profileType)
	fmt.Println("svar:", svar)
	fmt.Println("tail:", flag.Args())
}
