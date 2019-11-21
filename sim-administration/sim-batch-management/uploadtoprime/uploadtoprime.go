package uploadtoprime

import (
	"fmt"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/store"
	"strings"
)

func GeneratePostingCurlscript(url string, payload string) {
	fmt.Printf("#!/bin/bash\n")

	fmt.Printf("curl  -H 'Content-Type: text/plain' -X PUT --data-binary @-  %s <<EOF\n", url)
	fmt.Printf("%s", payload)
	fmt.Print("EOF\n")
}

func GenerateCsvPayload(db *store.SimBatchDB, batch model.Batch) string {
	var sb strings.Builder
	sb.WriteString("ICCID, IMSI, MSISDN, PIN1, PIN2, PUK1, PUK2, PROFILE\n")

	entries, err := db.GetAllSimEntriesForBatch(batch.BatchID)
	if err != nil {
		panic(err)
	}

	for  _ , entry:= range entries {
		line := fmt.Sprintf("%s, %s, %s,,,,,%s\n", entry.Iccid, entry.Imsi, entry.Msisdn, batch.ProfileType)
		sb.WriteString(line)
	}

	return sb.String()
}
