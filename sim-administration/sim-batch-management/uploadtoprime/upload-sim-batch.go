//usr/bin/env go run "$0" "$@"; exit "$?"

// XXX This is an utility script to feed the prime with sim profiles.
//     it  is actually a much better idea to extend the import functionality of
//     prime to generate sequences and checksums, but that will require a major
//     extension of a program that is soon going into production, so I'm keeping this
//     complexity external for now. However, the existance of this program should be
//     considered technical debt, and the debt can be paid back e.g. by
//     internalizing the logic into prime.

package uploadtoprime

import (
	"flag"
	"fmt"
	"log"
	"net/url"
	"regexp"
	"strings"
	"github.com/ostelco/ostelco-core/loltelutils"
	)

import (
	. "strconv"
)

func main() {
	batch := parseCommandLine()
	var csvPayload string = generateCsvPayload(batch)

	generatePostingCurlscript(batch.url, csvPayload)
}

func generatePostingCurlscript(url string, payload string) {
	fmt.Printf("#!/bin/bash\n")

	fmt.Printf("curl  -H 'Content-Type: text/plain' -X PUT --data-binary @-  %s <<EOF\n", url)
	fmt.Printf("%s", payload)
	fmt.Print(("EOF\n"))
}

func generateControlDigit(luhnString string) int {
	controlDigit := calculateChecksum(luhnString, true) % 10

	if controlDigit != 0 {
		controlDigit = 10 - controlDigit
	}

	return controlDigit
}

func calculateChecksum(luhnString string, double bool) int {
	source := strings.Split(luhnString, "")
	checksum := 0

	for i := len(source) - 1; i > -1; i-- {
		t, _ := ParseInt(source[i], 10, 8)
		n := int(t)

		if double {
			n = n * 2
		}

		double = !double

		if n >= 10 {
			n = n - 9
		}

		checksum += n
	}
	return checksum
}

func LuhnChecksum(number int) int {
	return generateControlDigit(Itoa(number))
}

func generateCsvPayload(batch Batch) string {
	var sb strings.Builder
	sb.WriteString("ICCID, IMSI, MSISDN, PIN1, PIN2, PUK1, PUK2, PROFILE\n")

	var iccidWithoutLuhnChecksum = batch.firstIccid

	var imsi = batch.firstImsi
	var msisdn = batch.firstMsisdn
	for i := 0; i <= batch.length; i++ {

		iccid := fmt.Sprintf("%d%1d", iccidWithoutLuhnChecksum, LuhnChecksum(iccidWithoutLuhnChecksum))
		line := fmt.Sprintf("%s, %d, %d,,,,,%s\n", iccid, imsi, msisdn, batch.profileType)
		sb.WriteString(line)

		iccidWithoutLuhnChecksum += batch.iccidIncrement
		imsi += batch.imsiIncrement
		msisdn += batch.msisdnIncrement
	}

	return sb.String()
}

func isICCID(s string) bool {
	match, _ := regexp.MatchString("^\\d{18}\\d?\\d?$", s)
	return match
}

func checkICCIDSyntax(name string, potentialIccid string) {
	if !isICCID(potentialIccid) {
		log.Fatalf("Not a valid %s ICCID: '%s'.  Must be 18 or 19 (or 20) digits (_including_ luhn checksum).", name, potentialIccid)
	}

	stringWithoutLuhnChecksum := IccidWithoutLuhnChecksum(potentialIccid)
	controlDigit := generateControlDigit(stringWithoutLuhnChecksum)
	checksummedCandidate := fmt.Sprintf("%s%d", stringWithoutLuhnChecksum, controlDigit)
	if checksummedCandidate != potentialIccid {
		log.Fatalf("Not a valid  ICCID: '%s'. Expected luhn checksom '%d'", potentialIccid, controlDigit)
	}
}

func isIMSI(s string) bool {
	match, _ := regexp.MatchString("^\\d{15}$", s)
	return match
}

func checkIMSISyntax(name string, potentialIMSI string) {
	if !isIMSI(potentialIMSI) {
		log.Fatalf("Not a valid %s IMSI: '%s'.  Must be 15 digits.", name, potentialIMSI)
	}
}

func isMSISDN(s string) bool {
	match, _ := regexp.MatchString("^\\d+$", s)
	return match
}

func checkMSISDNSyntax(name string, potentialMSISDN string) {
	if !isMSISDN(potentialMSISDN) {
		log.Fatalf("Not a valid %s MSISDN: '%s'.  Must be non-empty sequence of digits.", name, potentialMSISDN)
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
		log.Fatalf("Not a valid %s MSISDN: '%s'. Must be uppercase characters, numbers and underscores. ", name, potentialProfileName)
	}
}

type Batch struct {
	profileType     string
	url             string
	length          int
	firstMsisdn     int
	msisdnIncrement int
	firstIccid      int
	iccidIncrement  int
	firstImsi       int
	imsiIncrement   int
}

func IccidWithoutLuhnChecksum(s string) string {
	return TrimSuffix(s, 1)
}


func parseCommandLine() Batch {

	//
	// Set up command line parsing
	//
	firstIccid := flag.String("first-rawIccid",
		"not  a valid rawIccid",
		"An 18 or 19 digit long string.  The 19-th digit being a luhn luhnChecksum digit, if present")
	lastIccid := flag.String("last-rawIccid",
		"not  a valid rawIccid",
		"An 18 or 19 digit long string.  The 19-th digit being a luhn luhnChecksum digit, if present")
	firstIMSI := flag.String("first-imsi", "Not a valid IMSI", "First IMSI in batch")
	lastIMSI := flag.String("last-imsi", "Not a valid IMSI", "Last IMSI in batch")
	firstMsisdn := flag.String("first-msisdn", "Not a valid MSISDN", "First MSISDN in batch")
	lastMsisdn := flag.String("last-msisdn", "Not a valid MSISDN", "Last MSISDN in batch")
	profileType := flag.String("profile-type", "Not a valid sim profile type", "SIM profile type")
	batchLengthString := flag.String(
		"batch-length",
		"Not a valid batch-length, must be an integer",
		"Number of sim cards in batch")

	// XXX Legal values are Loltel and M1 at this time, how to configure that
	//     flexibly?  Eventually by puttig them in a database and consulting it during
	//     command execution, but for now, just by squinting.

	hssVendor := flag.String("hss-vendor", "M1", "The HSS vendor")
	uploadHostname :=
		flag.String("upload-hostname", "localhost", "host to upload batch to")
	uploadPortnumber :=
		flag.String("upload-portnumber", "8080", "port to upload to")

	profileVendor :=
		flag.String("profile-vendor", "Idemia", "Vendor of SIM profiles")

	initialHlrActivationStatusOfProfiles :=
		flag.String(
			"initial-hlr-activation-status-of-profiles",
			"ACTIVATED",
			"Initial hss activation state.  Legal values are ACTIVATED and NOT_ACTIVATED.")

	//
	// Parse input according to spec above
	//
	flag.Parse()

	//
	// Check parameters for syntactic correctness and
	// semantic sanity.
	//

	checkICCIDSyntax("first-rawIccid", *firstIccid)
	checkICCIDSyntax("last-rawIccid", *lastIccid)
	checkIMSISyntax("last-imsi", *lastIMSI)
	checkIMSISyntax("first-imsi", *firstIMSI)
	checkMSISDNSyntax("last-msisdn", *lastMsisdn)
	checkMSISDNSyntax("first-msisdn", *firstMsisdn)

	batchLength, err := Atoi(*batchLengthString)
	if err != nil {
		log.Fatalf("Not a valid batch length string '%s'.\n", *batchLengthString)
	}

	if batchLength <= 0 {
		log.Fatalf("Batch length must be positive, but was '%d'", batchLength)
	}

	uploadUrl := fmt.Sprintf("http://%s:%s/ostelco/sim-inventory/%s/import-batch/profilevendor/%s?initialHssState=%s",
		*uploadHostname, *uploadPortnumber, *hssVendor, *profileVendor, *initialHlrActivationStatusOfProfiles)

	checkURLSyntax("uploadUrl", uploadUrl)
	checkProfileType("profile-type", *profileType)

	// Convert to integers, and get lengths
	msisdnIncrement := -1
	if *firstMsisdn <= *lastMsisdn {
		msisdnIncrement = 1
	}

	log.Println("firstmsisdn     = ", *firstMsisdn)
	log.Println("lastmsisdn      = ", *lastMsisdn)
	log.Println("msisdnIncrement = ", msisdnIncrement)

	var firstMsisdnInt, _ = Atoi(*firstMsisdn)
	var lastMsisdnInt, _ = Atoi(*lastMsisdn)
	var msisdnLen = lastMsisdnInt - firstMsisdnInt + 1
	if msisdnLen < 0 {
		msisdnLen = -msisdnLen
	}

	var firstImsiInt, _ = Atoi(*firstIMSI)
	var lastImsiInt, _ = Atoi(*lastIMSI)
	var imsiLen = lastImsiInt - firstImsiInt + 1

	var firstIccidInt, _ = Atoi(IccidWithoutLuhnChecksum(*firstIccid))
	var lastIccidInt, _ = Atoi(IccidWithoutLuhnChecksum(*lastIccid))
	var iccidlen = lastIccidInt - firstIccidInt + 1

	// Validate that lengths of sequences are equal in absolute
	// values.
	if Abs(msisdnLen) != Abs(iccidlen) || Abs(msisdnLen) != Abs(imsiLen) || batchLength != Abs(imsiLen) {
		log.Printf("msisdnLen   = %10d\n", msisdnLen)
		log.Printf("iccidLen    = %10d\n", iccidlen)
		log.Printf("imsiLen     = %10d\n", imsiLen)
		log.Fatal("FATAL: msisdnLen, iccidLen and imsiLen are not identical.")
	}

	tail := flag.Args()
	if len(tail) != 0 {
		log.Printf("Unknown parameters:  %s", flag.Args())
	}


	// Return a correctly parsed batch
	return Batch{
		profileType:     *profileType,
		url:             uploadUrl,
		length:          Abs(iccidlen),
		firstIccid:      firstIccidInt,
		iccidIncrement:  Sign(iccidlen),
		firstImsi:       firstImsiInt,
		imsiIncrement:   Sign(imsiLen),
		firstMsisdn:     firstMsisdnInt,
		msisdnIncrement: msisdnIncrement,
	}
}
