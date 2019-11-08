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
	"fmt"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/fieldsyntaxchecks"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/store"
	"strconv"
	"strings"
)

func GeneratePostingCurlscript(url string, payload string) {
	fmt.Printf("#!/bin/bash\n")

	fmt.Printf("curl  -H 'Content-Type: text/plain' -X PUT --data-binary @-  %s <<EOF\n", url)
	fmt.Printf("%s", payload)
	fmt.Print("EOF\n")
}

func GenerateCsvPayload2(batch model.Batch) string {
	var sb strings.Builder
	sb.WriteString("ICCID, IMSI, MSISDN, PIN1, PIN2, PUK1, PUK2, PROFILE\n")

	iccidWithoutLuhnChecksum, err :=  strconv.Atoi(batch.FirstIccid)
	if err != nil {
		panic(err)
	}

	imsi, err :=  strconv.Atoi(batch.FirstImsi)
	if err != nil {
		panic(err)
	}

	var msisdn, err2 =  strconv.Atoi(batch.FirstMsisdn)
	if err2 != nil {
		panic(err)
	}



	// TODO: Replace with a loop over actual entries
	for i := 0; i < batch.Quantity; i++ {
		iccid := fmt.Sprintf("%d%1d", iccidWithoutLuhnChecksum, fieldsyntaxchecks.LuhnChecksum(iccidWithoutLuhnChecksum))
		line := fmt.Sprintf("%s, %d, %d,,,,,%s\n", iccid, imsi, msisdn, batch.ProfileType)
		sb.WriteString(line)

		iccidWithoutLuhnChecksum += batch.IccidIncrement
		imsi += batch.ImsiIncrement
		msisdn += batch.MsisdnIncrement
	}

	return sb.String()
}

func GenerateCsvPayload3(db *store.SimBatchDB, batch model.Batch) string {
	var sb strings.Builder
	sb.WriteString("ICCID, IMSI, MSISDN, PIN1, PIN2, PUK1, PUK2, PROFILE\n")

	entries, err := db.GetAllSimEntriesForBatch(batch.BatchId)
	if err != nil {
		panic(err)
	}

	for  _ , entry:= range entries {
		line := fmt.Sprintf("%s, %s, %s,,,,,%s\n", entry.Iccid, entry.Imsi, entry.Msisdn, batch.ProfileType)
		sb.WriteString(line)
	}

	return sb.String()
}
