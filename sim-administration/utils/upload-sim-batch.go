//usr/bin/env go run "$0" "$@"; exit "$?"

package main

import (
	"flag"
	"fmt"
	"log"
	"net/url"
	"regexp"
	. "strconv"
)

func main() {
	batch := parseCommandLine()
	// tbd:
	//   *  Get batch data from parsing of command line.
	//   *  Translate batch into CSV entry (either in memory, or as a function
	//      that can provide every line on demand).
	//   *  Upload batch to backend server, give copious feedback
	//      if it fails for whatever reason.

	var csvPayload string = generateCsvPayload(batch)
	fmt.Printf("CSV payload = %s", csvPayload)

	// Send it along using a HTTP post to the correct endpoint  URL, and check the return value.
}

func generateCsvPayload(batch Batch) string {
	return "This is not a CSV payload"
}

func isICCID(s string) bool {
	match, _ := regexp.MatchString("^\\d{18}\\d?$", s)
	return match
}

func checkICCIDSyntax(name string, potentialIccid string) {
	if !isICCID(potentialIccid) {
		log.Fatal("Not a valid %s ICCID: '%s'.  Must be 18 or 19 digits.", name, potentialIccid)
	}
}

func isIMSI(s string) bool {
	match, _ := regexp.MatchString("^\\d{15}$", s)
	return match
}

func checkIMSISyntax(name string, potentialIMSI string) {
	if !isIMSI(potentialIMSI) {
		log.Fatal("Not a valid %s IMSI: '%s'.  Must be 15 digits.", name, potentialIMSI)
	}
}

func isMSISDN(s string) bool {
	match, _ := regexp.MatchString("^\\d+$", s)
	return match
}

func checkMSISDNSyntax(name string, potentialMSISDN string) {
	if !isMSISDN(potentialMSISDN) {
		log.Fatal("Not a valid %s MSISDN: '%s'.  Must be non-empty sequence of digits.", name, potentialMSISDN)
	}
}

func checkURLSyntax(name string, theUrl string) {
	_, err := url.ParseRequestURI(theUrl)
	if err != nil {
		log.Fatalf("Not a valid %s URL: '%s'.", name, theUrl)
	}
}

func isProfileName(s string) bool {
	match, _ := regexp.MatchString("^[A-Z][A-Z0-9_]*$", s)
	return match
}

func checkProfileType(name string, potentialProfileName string) {
	if !isProfileName(potentialProfileName) {
		log.Fatal("Not a valid %s MSISDN: '%s'. Must be uppercase characters, numbers and underscores", name, potentialProfileName)
	}
}

type Batch struct {
	profileType     string
	length          int
	firstMsisdn     int
	msisdnIncrement int
	firstIccid      int
	iccidIncrement  int
	firstImsi       int
	imsiIncrement   int
}

func iccidWithoutLuhnChecksum(s string) string {
	if len(s) == 19 {
		return trimSuffix(s, 1)
	} else {
		return s
	}
}

func trimSuffix(s string, suffixLen int) string {
	return s[:len(s)-suffixLen]
}

func parseCommandLine() Batch {

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
	url := flag.String("url", "http://<NotAValidURL>/", "Not a valid url type")

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

	checkICCIDSyntax("first-iccid", *firstIccid)
	checkICCIDSyntax("last-iccid", *lastIccid)
	checkIMSISyntax("last-imsi", *lastIMSI)
	checkIMSISyntax("first-imsi", *firstIMSI)
	checkMSISDNSyntax("last-msisdn", *lastMsisdn)
	checkMSISDNSyntax("first-msisdn", *firstMsisdn)
	checkURLSyntax("url", *url)
	checkProfileType("profile-type", *profileType)

	// Convert to integers, and get lengths

	var firstMsisdnInt, _ = Atoi(*firstMsisdn)
	var lastMsisdnInt, _ = Atoi(*lastMsisdn)
	var msisdnLen = lastMsisdnInt - firstMsisdnInt

	var firstImsiInt, _ = Atoi(*firstIMSI)
	var lastImsiInt, _ = Atoi(*lastIMSI)
	var imsiLen = lastImsiInt - firstImsiInt

	var firstIccidInt, _ = Atoi(iccidWithoutLuhnChecksum(*firstIccid))
	var lastIccidInt, _ = Atoi(iccidWithoutLuhnChecksum(*lastIccid))
	var iccidlen = lastIccidInt - firstIccidInt

	// Validate that lengths of sequences are equal in absolute
	// values.
	if Abs(msisdnLen) != Abs(iccidlen) || Abs(msisdnLen) != Abs(imsiLen) {
		log.Println("msisdnLen =", msisdnLen)
		log.Println("iccidLen=", iccidlen)
		log.Println("imsiLen=", imsiLen)
		log.Fatal("FATAL: msisdnLen, iccidLen and imsiLen are not identical.")
	}

	tail := flag.Args()
	if len(tail) != 0 {
		log.Printf("Unknown parameters:  %s", flag.Args())
	}

	// Return a correctly parsed batch
	return Batch{
		profileType:     *profileType,
		length:          Abs(iccidlen),
		firstIccid:      firstIccidInt,
		iccidIncrement:  Sign(iccidlen),
		firstImsi:       firstImsiInt,
		imsiIncrement:   Sign(imsiLen),
		firstMsisdn:     firstMsisdnInt,
		msisdnIncrement: Sign(msisdnLen),
	}
}

func Sign(x int) int {
	if x < 0 {
		return -1
	} else if x > 0 {
		return 1
	} else {
		return 0
	}
}

// Abs returns the absolute value of x.
func Abs(x int) int {
	if x < 0 {
		return -x
	}
	return x
}
