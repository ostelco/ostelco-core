package fieldsyntaxchecks

import (
	"fmt"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/loltelutils"
	"log"
	"net/url"
	"regexp"
	"strconv"
	"strings"
)

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
		t, _ := strconv.ParseInt(source[i], 10, 8)
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

// LuhnChecksum generates a Luhn checksum control digit for the parameter number.
func LuhnChecksum(number int) int {
	return generateControlDigit(strconv.Itoa(number))
}

// AddLuhnChecksum interprets the parameter as a number, then
// generates and appends an one-digit Luhn checksum.
func AddLuhnChecksum(parameterNumber string) string {
	checksum := generateControlDigit(parameterNumber)
	return fmt.Sprintf("%s%1d", parameterNumber, checksum)
}

// IsICCID if the string is an 18 or 19 digit positive integer.
// Does not check luhn checksum.
func IsICCID(s string) bool {
	match, _ := regexp.MatchString("^\\d{18}\\d?\\d?$", s)
	return match
}

// IsICCID if the string is an 18 or 19 digit positive integer.
// Does  check luhn checksum.
func CheckICCIDSyntax(name string, potentialIccid string) {
	if !IsICCID(potentialIccid) {
		log.Fatalf("Not a valid '%s' ICCID: '%s'.  Must be 18 or 19 (or 20) digits (_including_ luhn checksum).", name, potentialIccid)
	}

	stringWithoutLuhnChecksum := IccidWithoutLuhnChecksum(potentialIccid)
	controlDigit := generateControlDigit(stringWithoutLuhnChecksum)
	checksummedCandidate := fmt.Sprintf("%s%d", stringWithoutLuhnChecksum, controlDigit)
	if checksummedCandidate != potentialIccid {
		log.Fatalf("Not a valid '%s'  ICCID: '%s'. Expected luhn checksom '%d'", name, potentialIccid, controlDigit)
	}
}

// IsIMSI is true iff the parameter string is a 15 digit number,
// indicating that it could be a valid IMSI.
func IsIMSI(s string) bool {
	match, _ := regexp.MatchString("^\\d{15}$", s)
	return match
}

// CheckIMSISyntax is a convenience function that checks
// if the potentialIMSI string is a syntactically correct
// IMSI. IF it isn't a log message is written and the program
// terminates.
func CheckIMSISyntax(name string, potentialIMSI string) {
	if !IsIMSI(potentialIMSI) {
		// TODO: Modify.  Return error value instead.
		log.Fatalf("Not a valid %s IMSI: '%s'.  Must be 15 digits.", name, potentialIMSI)
	}
}

// IsMSISDN bis true if the parameter string is a positive number.
func IsMSISDN(s string) bool {
	match, _ := regexp.MatchString("^\\d+$", s)
	return match
}

// CheckMSISDNSyntax is a convenience function that checks if
// the potential msisdn is an actual msisdn or not. If it isn't
// then an error message is written and the program
// terminates.
func CheckMSISDNSyntax(name string, potentialMSISDN string) {
	if !IsMSISDN(potentialMSISDN) {
		// TODO: Fix this to return error value instead.
		log.Fatalf("Not a valid %s MSISDN: '%s'.  Must be non-empty sequence of digits.", name, potentialMSISDN)
	}
}


// CheckURLSyntax is a convenience function that checks if
// the potential url is an actual url or not. If it isn't
// then an error message is written and the program
// terminates.
func CheckURLSyntax(name string, theUrl string) {
	if _, err := url.ParseRequestURI(theUrl);  err != nil {
		// TODO: Fix this to return error value instead.
		log.Fatalf("Not a valid %s URL: '%s'.", name, theUrl)
	}
}

func IsProfileName(s string) bool {
	match, _ := regexp.MatchString("^[A-Z][A-Z0-9_]*$", s)
	return match
}


// CheckProfileType is a convenience function that checks if
// the potential profile name is a syntacticaly correct
// profile name or not.. If it isn't
// then an error message is written and the program
// terminates.
func CheckProfileType(name string, potentialProfileName string) {
	if !IsProfileName(potentialProfileName) {
		// TODO: Fix this to return error value instead.
		log.Fatalf("Not a valid %s MSISDN: '%s'. Must be uppercase characters, numbers and underscores. ", name, potentialProfileName)
	}
}


// IccidWithoutLuhnChecksum takes an ICCID with a trailing Luhn
// checksum, and returns the value without the trailing checksum.
func IccidWithoutLuhnChecksum(s string) string {
	return loltelutils.TrimSuffix(s, 1)
}
