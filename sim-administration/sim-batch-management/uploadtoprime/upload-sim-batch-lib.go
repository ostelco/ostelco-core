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
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/fieldsyntaxchecks"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/loltelutils"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"
	"log"
	"strconv"
	"strings"
)

func GeneratePostingCurlscript(url string, payload string) {
	fmt.Printf("#!/bin/bash\n")

	fmt.Printf("curl  -H 'Content-Type: text/plain' -X PUT --data-binary @-  %s <<EOF\n", url)
	fmt.Printf("%s", payload)
	fmt.Print("EOF\n")
}

func GenerateCsvPayload(batch model.OutputBatch) string {
	var sb strings.Builder
	sb.WriteString("ICCID, IMSI, MSISDN, PIN1, PIN2, PUK1, PUK2, PROFILE\n")

	var iccidWithoutLuhnChecksum = batch.FirstIccid

	var imsi = batch.FirstImsi
	var msisdn = batch.FirstMsisdn
	for i := 0; i < batch.Length; i++ {

		iccid := fmt.Sprintf("%d%1d", iccidWithoutLuhnChecksum, fieldsyntaxchecks.LuhnChecksum(iccidWithoutLuhnChecksum))
		line := fmt.Sprintf("%s, %d, %d,,,,,%s\n", iccid, imsi, msisdn, batch.ProfileType)
		sb.WriteString(line)

		iccidWithoutLuhnChecksum += batch.IccidIncrement
		imsi += batch.ImsiIncrement
		msisdn += batch.MsisdnIncrement
	}

	return sb.String()
}

func ParseUploadFileGeneratorCommmandline() model.OutputBatch {

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
		"batch-Quantity",
		"Not a valid batch-Quantity, must be an integer",
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

	return OutputBatchFromCommandLineParameters(
		firstIccid,
		lastIccid,
		lastIMSI,
		firstIMSI,
		lastMsisdn,
		firstMsisdn,
		uploadHostname,
		uploadPortnumber,
		hssVendor,
		initialHlrActivationStatusOfProfiles,
		profileType,
		profileVendor,
		batchLengthString)
}

func OutputBatchFromCommandLineParameters(firstIccid *string,
	lastIccid *string,
	lastIMSI *string,
	firstIMSI *string,
	lastMsisdn *string,
	firstMsisdn *string,
	uploadHostname *string,
	uploadPortnumber *string,
	hssVendor *string,
	initialHlrActivationStatusOfProfiles *string,
	profileType *string,
	profileVendor *string,
	batchLengthString *string) model.OutputBatch {

	//
	// Check parameters for syntactic correctness and
	// semantic sanity.
	//

	fieldsyntaxchecks.CheckICCIDSyntax("first-rawIccid", *firstIccid)
	fieldsyntaxchecks.CheckICCIDSyntax("last-rawIccid", *lastIccid)
	fieldsyntaxchecks.CheckIMSISyntax("last-imsi", *lastIMSI)
	fieldsyntaxchecks.CheckIMSISyntax("first-imsi", *firstIMSI)
	fieldsyntaxchecks.CheckMSISDNSyntax("last-msisdn", *lastMsisdn)
	fieldsyntaxchecks.CheckMSISDNSyntax("first-msisdn", *firstMsisdn)

	batchLength, err := strconv.Atoi(*batchLengthString)
	if err != nil {
		log.Fatalf("Not a valid batch Quantity string '%s'.\n", *batchLengthString)
	}

	if batchLength <= 0 {
		log.Fatalf("OutputBatch Quantity must be positive, but was '%d'", batchLength)
	}

	uploadUrl := fmt.Sprintf("http://%s:%s/ostelco/sim-inventory/%s/import-batch/profilevendor/%s?initialHssState=%s",
		*uploadHostname, *uploadPortnumber, *hssVendor, *profileVendor, *initialHlrActivationStatusOfProfiles)

	fieldsyntaxchecks.CheckURLSyntax("uploadUrl", uploadUrl)
	fieldsyntaxchecks.CheckProfileType("profile-type", *profileType)

	// Convert to integers, and get lengths
	msisdnIncrement := -1
	if *firstMsisdn <= *lastMsisdn {
		msisdnIncrement = 1
	}

	log.Println("firstmsisdn     = ", *firstMsisdn)
	log.Println("lastmsisdn      = ", *lastMsisdn)
	log.Println("MsisdnIncrement = ", msisdnIncrement)

	var firstMsisdnInt, _ = strconv.Atoi(*firstMsisdn)
	var lastMsisdnInt, _ = strconv.Atoi(*lastMsisdn)
	var msisdnLen = lastMsisdnInt - firstMsisdnInt + 1
	if msisdnLen < 0 {
		msisdnLen = -msisdnLen
	}

	var firstImsiInt, _ = strconv.Atoi(*firstIMSI)
	var lastImsiInt, _ = strconv.Atoi(*lastIMSI)
	var imsiLen = lastImsiInt - firstImsiInt + 1

	var firstIccidInt, _ = strconv.Atoi(fieldsyntaxchecks.IccidWithoutLuhnChecksum(*firstIccid))
	var lastIccidInt, _ = strconv.Atoi(fieldsyntaxchecks.IccidWithoutLuhnChecksum(*lastIccid))
	var iccidlen = lastIccidInt - firstIccidInt + 1

	// Validate that lengths of sequences are equal in absolute
	// values.
	// TODO: Perhaps use some varargs trick of some sort here?
	if loltelutils.Abs(msisdnLen) != loltelutils.Abs(iccidlen) || loltelutils.Abs(msisdnLen) != loltelutils.Abs(imsiLen) || batchLength != loltelutils.Abs(imsiLen) {
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
	return model.OutputBatch{
		ProfileType:     *profileType,
		Url:             uploadUrl,
		Length:          loltelutils.Abs(iccidlen),
		FirstIccid:      firstIccidInt,
		IccidIncrement:  loltelutils.Sign(iccidlen),
		FirstImsi:       firstImsiInt,
		ImsiIncrement:   loltelutils.Sign(imsiLen),
		FirstMsisdn:     firstMsisdnInt,
		MsisdnIncrement: msisdnIncrement,
	}
}

///
///    Input batch management
///

func ParseInputFileGeneratorCommmandline() model.InputBatch {

	// TODO: This function should be rewritten to parse a string array and send it to flags.
	//       we need to up our Go-Fu before we can make flag.Parse(arguments) work

	return model.InputBatch{
		Customer:    "Footel",
		ProfileType: "BAR_FOOTEL_STD",
		OrderDate:   "20191007",
		BatchNo:     "2019100701",
		Quantity:    10,
		FirstIccid:  "894700000000002214",
		FirstImsi:   "242017100012213"}
}

func GenerateInputFile(batch model.InputBatch) string {
	result := "*HEADER DESCRIPTION\n" +
		"***************************************\n" +
		fmt.Sprintf("Customer        :%s\n", batch.Customer) +
		fmt.Sprintf("ProfileType     : %s\n", batch.ProfileType) +
		fmt.Sprintf("Order Date      : %s\n", batch.OrderDate) +
		fmt.Sprintf("Batch No        : %s\n", batch.BatchNo) +
		fmt.Sprintf("Quantity        : %d\n", batch.Quantity) +
		"***************************************\n" +
		"*INPUT VARIABLES\n" +
		"***************************************\n" +
		"var_In:\n" +
		fmt.Sprintf(" ICCID: %s\n", batch.FirstIccid) +
		fmt.Sprintf("IMSI: %s\n", batch.FirstImsi) +
		"***************************************\n" +
		"*OUTPUT VARIABLES\n" +
		"***************************************\n" +
		"var_Out: ICCID/IMSI/KI\n"
	return result
}
