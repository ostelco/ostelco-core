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

func LuhnChecksum(number int) int {
	return generateControlDigit(strconv.Itoa(number))
}

func IsICCID(s string) bool {
	match, _ := regexp.MatchString("^\\d{18}\\d?\\d?$", s)
	return match
}

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

func IsIMSI(s string) bool {
	match, _ := regexp.MatchString("^\\d{15}$", s)
	return match
}

func CheckIMSISyntax(name string, potentialIMSI string) {
	if !IsIMSI(potentialIMSI) {
		log.Fatalf("Not a valid %s IMSI: '%s'.  Must be 15 digits.", name, potentialIMSI)
	}
}

func IsMSISDN(s string) bool {
	match, _ := regexp.MatchString("^\\d+$", s)
	return match
}

func CheckMSISDNSyntax(name string, potentialMSISDN string) {
	if !IsMSISDN(potentialMSISDN) {
		log.Fatalf("Not a valid %s MSISDN: '%s'.  Must be non-empty sequence of digits.", name, potentialMSISDN)
	}
}

func CheckURLSyntax(name string, theUrl string) {
	_, err := url.ParseRequestURI(theUrl)
	if err != nil {
		log.Fatalf("Not a valid %s URL: '%s'.", name, theUrl)
	}
}

func IsProfileName(s string) bool {
	match, _ := regexp.MatchString("^[A-Z][A-Z0-9_]*$", s)
	return match
}

func CheckProfileType(name string, potentialProfileName string) {
	if !IsProfileName(potentialProfileName) {
		log.Fatalf("Not a valid %s MSISDN: '%s'. Must be uppercase characters, numbers and underscores. ", name, potentialProfileName)
	}
}

func IccidWithoutLuhnChecksum(s string) string {
	return loltelutils.TrimSuffix(s, 1)
}
