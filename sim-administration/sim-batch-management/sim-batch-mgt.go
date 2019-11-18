//usr/bin/env go run "$0" "$@"; exit "$?"
package main

import (
	"bufio"
	"encoding/csv"
	"encoding/json"
	"fmt"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/es2plus"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/fieldsyntaxchecks"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/outfileparser"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/store"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/uploadtoprime"
	kingpin "gopkg.in/alecthomas/kingpin.v2"
	"io"
	"log"
	"os"
	"path/filepath"
	"strings"
	"sync"
)

//  "gopkg.in/alecthomas/kingpin.v2"
var (
	// TODO: Global flags can be added to Kingpin, but also make it have an effect.
	// debug    = kingpin.Flag("debug", "enable debug mode").Default("false").Bool()



	///
	///   Profile-vendor - centric commands
	///
	// Declare a profile-vendor with an SM-DP+ that can be referred to from
	// batches.  Referential integrity required, so it won't be possible to
	// declare bathes with non-existing profile vendors.

	dpv             = kingpin.Command("profile-vendor-declare", "Declare a profile vendor with an SM-DP+ we can talk to")
	dpvName         = dpv.Flag("name", "Name of profile-vendor").Required().String()
	dpvCertFilePath = dpv.Flag("cert", "Certificate pem file.").Required().String()
	dpvKeyFilePath  = dpv.Flag("key", "Certificate key file.").Required().String()
	dpvHost         = dpv.Flag("host", "Host of ES2+ endpoint.").Required().String()
	dpvPort         = dpv.Flag("port", "Port of ES2+ endpoint").Required().Int()
	dpvRequesterId  = dpv.Flag("requester-id", "ES2+ requester ID.").Required().String()

	///
	///    ICCID - centric commands
	///

	getStatus              = kingpin.Command("iccid-get-status", "Get status for an iccid")
	getStatusProfileVendor = getStatus.Flag("profile-vendor", "Name of profile vendor").Required().String()
	getStatusProfileIccid  = getStatus.Arg("iccid", "Iccid to get status for").Required().String()

	downloadOrder       = kingpin.Command("iccid-download-order", "Execute es2p download-order.")
	downloadOrderVendor = downloadOrder.Flag("profile-vendor", "Name of profile vendor").Required().String()
	downloadOrderIccid  = downloadOrder.Flag("iccid", "Iccid to recover profile  for").Required().String()

	recoverProfile       = kingpin.Command("iccid-recover-profile", "Change state of profile.")
	recoverProfileVendor = recoverProfile.Flag("profile-vendor", "Name of profile vendor").Required().String()
	recoverProfileIccid  = recoverProfile.Flag("iccid", "Iccid to recover profile  for").Required().String()
	recoverProfileTarget = recoverProfile.Flag("target-state", "Desired target state").Required().String()

	cancelIccid       = kingpin.Command("iccid-cancel", "Execute es2p iccid-confirm-order.")
	cancelIccidIccid  = cancelIccid.Flag("iccid", "The iccid to cancel").Required().String()
	cancelIccidVendor = cancelIccid.Flag("profile-vendor", "Name of profile vendor").Required().String()
	cancelIccidTarget = cancelIccid.Flag("target", "Tarfget t set profile to").Required().String()

	activateIccidFile       = kingpin.Command("iccids-activate-from-file", "Execute es2p iccid-confirm-order.")
	activateIccidFileVendor = activateIccidFile.Flag("profile-vendor", "Name of profile vendor").Required().String()
	activateIccidFileFile   = activateIccidFile.Flag("iccid-file", "Iccid to confirm profile  for").Required().String()

	bulkActivateIccids       = kingpin.Command("iccids-bulk-activate", "Execute es2p iccid-confirm-order.")
	bulkActivateIccidsVendor = bulkActivateIccids.Flag("profile-vendor", "Name of profile vendor").Required().String()
	bulkActivateIccidsIccids = bulkActivateIccids.Flag("iccid-file", "Iccid to confirm profile  for").Required().String()

	activateIccid       = kingpin.Command("iccid-activate", "Execute es2p iccid-confirm-order.")
	activateIccidVendor = activateIccid.Flag("profile-vendor", "Name of profile vendor").Required().String()
	activateIccidIccid  = activateIccid.Flag("iccid", "Iccid to confirm profile  for").Required().String()

	confirmOrder       = kingpin.Command("iccid-confirm-order", "Execute es2p iccid-confirm-order.")
	confirmOrderVendor = confirmOrder.Flag("profile-vendor", "Name of profile vendor").Required().String()
	confirmOrderIccid  = confirmOrder.Flag("iccid", "Iccid to confirm profile  for").Required().String()

	///
	///   Batch - centric commands
	///

	setBatchActivationCodes      = kingpin.Command("batch-activate-all-profiles", "Execute activation of all  profiles in batch, get activation codes from SM-DP+ and put these codes into the local database.")
	setBatchActivationCodesBatch = setBatchActivationCodes.Arg("batch-name", "Batch to get activation codes for").Required().String()

	getProfActActStatusesForBatch      = kingpin.Command("batch-get-activation-statuses", "Get current activation statuses from SM-DP+ for named batch.")
	getProfActActStatusesForBatchBatch = getProfActActStatusesForBatch.Arg("batch-name", "The batch to get activation statuses for.").Required().String()

	describeBatch      = kingpin.Command("batch-describe", "Describe a batch with a particular name.")
	describeBatchBatch = describeBatch.Arg("batch-name", "The batch to describe").String()

	generateInputFile          = kingpin.Command("batch-generate-input-file", "Generate input file for a named batch using stored parameters")
	generateInputFileBatchname = generateInputFile.Arg("batch-name", "The batch to generate the input file for.").String()

	addMsisdnFromFile        = kingpin.Command("batch-add-msisdn-from-file", "Add MSISDN from CSV file containing at least ICCID/MSISDN, but also possibly IMSI.")
	addMsisdnFromFileBatch   = addMsisdnFromFile.Flag("batch-name", "The batch to augment").Required().String()
	addMsisdnFromFileCsvfile = addMsisdnFromFile.Flag("csv-file", "The CSV file to read from").Required().ExistingFile()
	addMsisdnFromFileAddLuhn = addMsisdnFromFile.Flag("add-luhn-checksums", "Assume that the checksums for the ICCIDs are not present, and add them").Default("false").Bool()


	bwBatch      = kingpin.Command("batch-write-hss", "Generate a batch upload script")
	bwBatchName  = bwBatch.Arg("batch-name", "The batch to generate upload script from").String()
	bwOutputDirName  = bwBatch.Arg("output-dir-name", "The directory in which to place the output file.").String()


	spUpload                 = kingpin.Command("batch-read-out-file", "Convert an output (.out) file from an sim profile producer into an input file for an HSS.")
	spBatchName              = spUpload.Arg("batch-name", "The batch to augment").Required().String()
	spUploadInputFile        = spUpload.Arg("input-file", "path to .out file used as input file").Required().String()


	generateUploadBatch = kingpin.Command("batch-generate-upload-script", "Write a file that can be used by an HSS to insert profiles.")
	generateUploadBatchBatch = generateUploadBatch.Arg("batch", "The batch to output from").Required().String()


// TODO: Delete this asap!
//	spUploadOutputFilePrefix = spUpload.Flag("output-file-prefix",
//		"prefix to path to .csv file used as input file, filename will be autogenerated").Required().String()


	generateActivationCodeSql      = kingpin.Command("batch-generate-activation-code-updating-sql", "Generate SQL code to update access codes")
	generateActivationCodeSqlBatch = generateActivationCodeSql.Arg("batch-name", "The batch to generate sql coce for").String()

	bd           = kingpin.Command("batch-declare", "Declare a batch to be persisted, and used by other commands")
	dbName       = bd.Flag("name", "Unique name of this batch").Required().String()
	dbAddLuhn    = bd.Flag("add-luhn-checksums", "Assume that the checksums for the ICCIDs are not present, and add them").Default("false").Bool()
	dbCustomer   = bd.Flag("customer", "Name of the customer of this batch (with respect to the sim profile vendor)").Required().String()
	dbBatchNo    = bd.Flag("batch-no", "Unique number of this batch (with respect to the profile vendor)").Required().String()
	dbOrderDate  = bd.Flag("order-date", "Order date in format ddmmyyyy").Required().String()
	dbFirstIccid = bd.Flag("first-rawIccid",
		"An 18 or 19 digit long string.  The 19-th digit being a luhn Checksum digit, if present").Required().String()
	dbLastIccid = bd.Flag("last-rawIccid",
		"An 18 or 19 digit long string.  The 19-th digit being a luhn Checksum digit, if present").Required().String()
	dbFirstIMSI         = bd.Flag("first-imsi", "First IMSI in batch").Required().String()
	dbLastIMSI          = bd.Flag("last-imsi", "Last IMSI in batch").Required().String()
	dbFirstMsisdn       = bd.Flag("first-msisdn", "First MSISDN in batch").Required().String()
	dbLastMsisdn        = bd.Flag("last-msisdn", "Last MSISDN in batch").Required().String()
	dbProfileType       = bd.Flag("profile-type", "SIM profile type").Required().String()
	dbBatchLengthString = bd.Flag(
		"batch-quantity",
		"Number of sim cards in batch").Required().String()

	dbHssVendor        = bd.Flag("hss-vendor", "The HSS vendor").Default("M1").String()
	dbUploadHostname   = bd.Flag("upload-hostname", "host to upload batch to").Default("localhost").String()
	dbUploadPortnumber = bd.Flag("upload-portnumber", "port to upload to").Default("8080").String()
	dbProfileVendor = bd.Flag("profile-vendor", "Vendor of SIM profiles").Default("Idemia").String()

	dbInitialHlrActivationStatusOfProfiles = bd.Flag(
		"initial-hlr-activation-status-of-profiles",
		"Initial hss activation state.  Legal values are ACTIVATED and NOT_ACTIVATED.").Default("ACTIVATED").String()
)

func main() {

	kingpin.Command("batches-list", "List all known batches.")

	if err := parseCommandLine(); err != nil {
		panic(err)
	}
}

func parseCommandLine() error {

	db, err := store.OpenFileSqliteDatabaseFromPathInEnvironmentVariable("SIM_BATCH_DATABASE")

	if err != nil {
		return fmt.Errorf("couldn't open sqlite database.  '%s'", err)
	}

	db.GenerateTables()

	cmd := kingpin.Parse()
	switch cmd {

	case "profile-vendor-declare":

		vendor, err := db.GetProfileVendorByName(*dpvName)
		if err != nil {
			return err
		}

		if vendor != nil {
			return fmt.Errorf("already declared profile vendor '%s'", *dpvName)
		}

		if _, err := os.Stat(*dpvCertFilePath); os.IsNotExist(err) {
			return fmt.Errorf("can't find certificate file '%s'", *dpvCertFilePath)
		}

		if _, err := os.Stat(*dpvKeyFilePath); os.IsNotExist(err) {
			return fmt.Errorf("can't find key file '%s'", *dpvKeyFilePath)
		}

		if *dpvPort <= 0 {
			return fmt.Errorf("port  must be positive was '%d'", *dpvPort)
		}

		if 65534 < *dpvPort {
			return fmt.Errorf("port must be smaller than or equal to 65535, was '%d'", *dpvPort)
		}

		// Modify the paths to absolute  paths.

		absDpvCertFilePath, err := filepath.Abs(*dpvCertFilePath)
		if err != nil {
			return err
		}
		absDpvKeyFilePath, err := filepath.Abs(*dpvKeyFilePath)
		if err != nil {
			return err
		}
		v := &model.ProfileVendor{
			Name:               *dpvName,
			Es2PlusCert:        absDpvCertFilePath,
			Es2PlusKey:         absDpvKeyFilePath,
			Es2PlusHost:        *dpvHost,
			Es2PlusPort:        *dpvPort,
			Es2PlusRequesterId: *dpvRequesterId,
		}

		if err := db.CreateProfileVendor(v); err != nil {
			return err
		}

		fmt.Println("Declared a new vendor named ", *dpvName)

	case "batch-get-activation-statuses":
		batchName := *getProfActActStatusesForBatchBatch

		log.Printf("Getting statuses for all profiles in batch  named %s\n", batchName)

		batch, err := db.GetBatchByName(batchName)
		if err != nil {
			return fmt.Errorf("unknown batch '%s'", batchName)
		}

		client, err := ClientForVendor(db, batch.ProfileVendor)
		if err != nil {
			return err
		}

		entries, err := db.GetAllSimEntriesForBatch(batch.BatchId)
		if err != nil {
			return err
		}

		if len(entries) != batch.Quantity {
			return fmt.Errorf("batch quantity retrieved from database (%d) different from batch quantity (%d)", len(entries), batch.Quantity)
		}

		log.Printf("Found %d profiles\n", len(entries))

		// XXX Is this really necessary? I don't think so
		var mutex = &sync.Mutex{}

		var waitgroup sync.WaitGroup

		// Limit concurrency of the for-loop below
		// to 160 goroutines.  The reason is that if we get too
		// many we run out of file descriptors, and we don't seem to
		// get much speedup after hundred or so.

		concurrency := 160
		sem := make(chan bool, concurrency)
		for _, entry := range entries {

			//
			// Only apply activation if not already noted in the
			// database.
			//

			sem <- true

			waitgroup.Add(1)
			go func(entry model.SimEntry) {

				defer func() { <-sem }()

				result, err := client.GetStatus(entry.Iccid)
				if err != nil {
					panic(err)
				}

				if result == nil {
					log.Printf("ERROR: Couldn't find any status for Iccid='%s'\n", entry.Iccid)
				}

				mutex.Lock()
				fmt.Printf("%s, %s\n", entry.Iccid, result.State)
				mutex.Unlock()
				waitgroup.Done()
			}(entry)
		}

		waitgroup.Wait()
		for i := 0; i < cap(sem); i++ {
			sem <- true
		}

	case "batch-read-out-file":

		batch, err := db.GetBatchByName(*spBatchName)

		if err != nil {
			return err
		}

		if batch == nil {
			return fmt.Errorf("no batch found with name '%s'", *spBatchName)
		}


		// TODO handle error values
		outRecord := outfileparser.ParseOutputFile(*spUploadInputFile)

		// TODO: Handle inconsistencies in the outRecord to ensure that
		//       we're not reading an incongruent output file.


		// TODO: Do all of this in a transaction!
		for _, e := range outRecord.Entries {
			// TODO: The ICCIDs may be paddec with F values, and I don't want to
			//       deal with that now, so I'm
			// simProfile, err := db.GetSimProfileByIccid(e.Iccid)
			simProfile, err := db.GetSimProfileByImsi(e.Imsi)
			if err != nil {return err}
			if simProfile == nil { return fmt.Errorf("couldn't find profile enty for IMSI=%s", e.Imsi)}
			if simProfile.Imsi != e.Imsi{
				return fmt.Errorf("profile enty for ICCID=%s has IMSI (%s), but we expected (%s)",e.Iccid,  e.Imsi, simProfile.Imsi)
			}
			db.UpdateSimEntryKi(simProfile.Id, e.Ki)
		}


	case "batch-write-hss":

		batch, err := db.GetBatchByName(*bwBatchName)

		if err != nil {
			return err
		}

		if batch == nil {
			return fmt.Errorf("no batch found with name '%s'", *bwBatchName)
		}

		outputFile := fmt.Sprintf("%s/%s.csv", *bwOutputDirName,   batch.Name)
		log.Println("outputFile = ", outputFile)

		if err := outfileparser.WriteHssCsvFile(outputFile, db, batch); err != nil {
			return fmt.Errorf("couldn't write hss output to file  '%s', .  Error = '%v'", outputFile, err)
		}


	case "batches-list":
		allBatches, err := db.GetAllBatches()
		if err != nil {
			return err
		}

		fmt.Println("Names of current batches: ")
		for _, batch := range allBatches {
			fmt.Printf("  %s\n", batch.Name)
		}

	case "batch-describe":

		batch, err := db.GetBatchByName(*describeBatchBatch)
		if err != nil {
			return err
		}

		if batch == nil {
			return fmt.Errorf("no batch found with name '%s'", *describeBatchBatch)
		} else {
			bytes, err := json.MarshalIndent(batch, "    ", "     ")
			if err != nil {
				return fmt.Errorf("can't serialize batch '%v'", batch)
			}

			fmt.Printf("%v\n", string(bytes))
		}

	case "batch-generate-activation-code-updating-sql":
		batch, err := db.GetBatchByName(*generateActivationCodeSqlBatch)
		if err != nil {
			return fmt.Errorf("couldn't find batch named '%s' (%s) ", *generateActivationCodeSqlBatch, err)
		}

		simEntries, err := db.GetAllSimEntriesForBatch(batch.BatchId)
		if err != nil {
			return err
		}

		for _, b := range simEntries {
			fmt.Printf(
				"UPDATE sim_entries SET matchingid = '%s', smdpplusstate = 'RELEASED', provisionstate = 'AVAILABLE' WHERE Iccid = '%s' and smdpplusstate = 'AVAILABLE';\n",
				b.ActivationCode,
				b.Iccid)
		}

	case "batch-generate-upload-script":
		batch, err := db.GetBatchByName(*generateUploadBatchBatch)
		if err != nil {
			return err
		}

		if batch == nil {
			return fmt.Errorf("no batch found with name '%s'", *describeBatchBatch)
		} else {
			var csvPayload = uploadtoprime.GenerateCsvPayload(db, *batch)
			uploadtoprime.GeneratePostingCurlscript(batch.Url, csvPayload)
		}

	case "batch-generate-input-file":
		batch, err := db.GetBatchByName(*generateInputFileBatchname)
		if err != nil {
			return err
		}

		if batch == nil {
			return fmt.Errorf("no batch found with name '%s'", *generateInputFileBatchname)
		} else {
			var result = GenerateInputFile(batch)
			fmt.Println(result)
		}

	case "batch-add-msisdn-from-file":
		batchName := *addMsisdnFromFileBatch
		csvFilename := *addMsisdnFromFileCsvfile
		addLuhns := *addMsisdnFromFileAddLuhn

		batch, err := db.GetBatchByName(batchName)
		if err != nil {
			return err
		}

		csvFile, _ := os.Open(csvFilename)
		reader := csv.NewReader(bufio.NewReader(csvFile))

		defer csvFile.Close()

		headerLine, err := reader.Read()
		if err == io.EOF {
			break
		} else if err != nil {
			return err
		}

		var columnMap map[string]int
		columnMap = make(map[string]int)

		for index, fieldname := range headerLine {
			columnMap[strings.ToLower(fieldname)] = index
		}

		if _, hasIccid := columnMap["Iccid"]; !hasIccid {
			return fmt.Errorf("no ICCID  column in CSV file")
		}

		if _, hasMsisdn := columnMap["msisdn"]; !hasMsisdn {
			return fmt.Errorf("no MSISDN  column in CSV file")
		}

		if _, hasImsi := columnMap["imsi"]; !hasImsi {
			return fmt.Errorf("no IMSI  column in CSV file")
		}

		type csvRecord struct {
			iccid  string
			imsi   string
			msisdn string
		}

		var recordMap map[string]csvRecord
		recordMap = make(map[string]csvRecord)

		// Read all the lines into the record map.
		for {
			line, error := reader.Read()
			if error == io.EOF {
				break
			} else if error != nil {
				log.Fatal(error)
			}

			iccid := line[columnMap["Iccid"]]

			if addLuhns {
				iccid = fieldsyntaxchecks.AddLuhnChecksum(iccid)
			}

			record := csvRecord{
				iccid:  iccid,
				imsi:   line[columnMap["imsi"]],
				msisdn: line[columnMap["msisdn"]],
			}

			if _, duplicateRecordExists := recordMap[record.iccid]; duplicateRecordExists {
				return fmt.Errorf("duplicate ICCID record in map: %s", record.iccid)
			}

			recordMap[record.iccid] = record
		}

		simEntries, err := db.GetAllSimEntriesForBatch(batch.BatchId)
		if err != nil {
			return err
		}

		// Check for compatibility
		tx := db.Begin()
		noOfRecordsUpdated := 0
		for _, entry := range simEntries {
			record, iccidRecordIsPresent := recordMap[entry.Iccid]
			if !iccidRecordIsPresent {
				tx.Rollback()
				return fmt.Errorf("ICCID not in batch: %s", entry.Iccid)
			}

			if entry.Imsi != record.imsi {
				tx.Rollback()
				return fmt.Errorf("IMSI mismatch for ICCID=%s.  Batch has %s, csv file has %s", entry.Iccid, entry.Imsi, record.iccid)
			}

			if entry.Msisdn != "" && record.msisdn != "" && record.msisdn != entry.Msisdn {
				tx.Rollback()
				return fmt.Errorf("MSISDN mismatch for ICCID=%s.  Batch has %s, csv file has %s", entry.Iccid, entry.Msisdn, record.msisdn)
			}

			if entry.Msisdn == "" && record.msisdn != "" {
				err = db.UpdateSimEntryMsisdn(entry.Id, record.msisdn)
				if err != nil {
					tx.Rollback()
					return err
				}
				noOfRecordsUpdated += 1
			}
		}
		tx.Commit()

		log.Printf("Updated %d of a total of %d records in batch '%s'\n", noOfRecordsUpdated, len(simEntries), batchName)

	case "batch-declare":
		log.Println("Declare batch")
		batch, err := db.DeclareBatch(
			*dbName,
			*dbAddLuhn,
			*dbCustomer,
			*dbBatchNo,
			*dbOrderDate,
			*dbFirstIccid,
			*dbLastIccid,
			*dbFirstIMSI,
			*dbLastIMSI,
			*dbFirstMsisdn,
			*dbLastMsisdn,
			*dbProfileType,
			*dbBatchLengthString,
			*dbHssVendor,
			*dbUploadHostname,
			*dbUploadPortnumber,
			*dbProfileVendor,
			*dbInitialHlrActivationStatusOfProfiles)


		if err != nil {
			return err
		}
		log.Printf("Declared batch '%s'", batch.Name)
		return nil


	case "iccid-get-status":
		client, err := ClientForVendor(db, *getStatusProfileVendor)
		if err != nil {
			return err
		}

		result, err := client.GetStatus(*getStatusProfileIccid)
		if err != nil {
			return err
		}
		log.Printf("Iccid='%s', state='%s', acToken='%s'\n", *getStatusProfileIccid, (*result).State, (*result).ACToken)

	case "iccid-recover-profile":
		client, err := ClientForVendor(db, *recoverProfileVendor)
		if err != nil {
			return err
		}

		err = checkEs2TargetState(*recoverProfileTarget)
		if err != nil {
			return err
		}
		result, err := client.RecoverProfile(*recoverProfileIccid, *recoverProfileTarget)
		if err != nil {
			return err
		}
		log.Println("result -> ", result)

	case "iccid-download-order":
		client, err := ClientForVendor(db, *downloadOrderVendor)
		if err != nil {
			return err
		}
		result, err := client.DownloadOrder(*downloadOrderIccid)
		if err != nil {
			return err
		}
		log.Println("result -> ", result)

	case "iccid-confirm-order":
		client, err := ClientForVendor(db, *confirmOrderVendor)
		if err != nil {
			return err
		}
		result, err := client.ConfirmOrder(*confirmOrderIccid)
		if err != nil {
			return err
		}
		fmt.Println("result -> ", result)

	case "iccid-activate":
		client, err := ClientForVendor(db, *activateIccidVendor)
		if err != nil {
			return err
		}

		result, err := client.ActivateIccid(*activateIccidIccid)

		if err != nil {
			return err
		}
		fmt.Printf("%s, %s\n", *activateIccidIccid, result.ACToken)

	case "iccid-cancel":
		client, err := ClientForVendor(db, *cancelIccidVendor)
		if err != nil {
			return err
		}
		err = checkEs2TargetState(*cancelIccidTarget)
		if err != nil {
			return err
		}
		_, err = client.CancelOrder(*cancelIccidIccid, *cancelIccidTarget)
		if err != nil {
			return err
		}

	case "iccids-activate-from-file":
		client, err := ClientForVendor(db, *activateIccidFileVendor)
		if err != nil {
			return err
		}

		csvFilename := *activateIccidFileFile

		csvFile, _ := os.Open(csvFilename)
		reader := csv.NewReader(bufio.NewReader(csvFile))

		defer csvFile.Close()

		headerLine, error := reader.Read()
		if error == io.EOF {
			break
		} else if error != nil {
			log.Fatal(error)
		}

		var columnMap map[string]int
		columnMap = make(map[string]int)

		for index, fieldname := range headerLine {
			columnMap[strings.TrimSpace(strings.ToLower(fieldname))] = index
		}

		if _, hasIccid := columnMap["iccid"]; !hasIccid {
			return fmt.Errorf("no ICCID  column in CSV file")
		}

		type csvRecord struct {
			Iccid string
		}

		var recordMap map[string]csvRecord
		recordMap = make(map[string]csvRecord)

		// Read all the lines into the record map.
		for {
			line, err := reader.Read()
			if err == io.EOF {
				break
			} else if err != nil {
				return err
			}

			iccid := line[columnMap["Iccid"]]
			iccid = strings.TrimSpace(iccid)

			record := csvRecord{
				Iccid: iccid,
			}

			if _, duplicateRecordExists := recordMap[record.Iccid]; duplicateRecordExists {
				return fmt.Errorf("duplicate ICCID record in map: %s", record.Iccid)
			}

			recordMap[record.Iccid] = record
		}

		// XXX Is this really necessary? I don't think so
		var mutex = &sync.Mutex{}

		var waitgroup sync.WaitGroup

		// Limit concurrency of the for-loop below
		// to 160 goroutines.  The reason is that if we get too
		// many we run out of file descriptors, and we don't seem to
		// get much speedup after hundred or so.

		concurrency := 160
		sem := make(chan bool, concurrency)
		fmt.Printf("%s, %s\n", "ICCID", "STATE")
		for _, entry := range recordMap {

			//
			// Only apply activation if not already noted in the
			// database.
			//

			sem <- true

			waitgroup.Add(1)
			go func(entry csvRecord) {

				defer func() { <-sem }()

				result, err := client.GetStatus(entry.Iccid)
				if err != nil {
					panic(err)
				}

				if result == nil {
					panic(fmt.Sprintf("Couldn't find any status for Iccid='%s'\n", entry.Iccid))
				}

				mutex.Lock()
				fmt.Printf("%s, %s\n", entry.Iccid, result.State)
				mutex.Unlock()
				waitgroup.Done()
			}(entry)
		}

		waitgroup.Wait()
		for i := 0; i < cap(sem); i++ {
			sem <- true
		}

	case "batch-activate-all-profiles":

		client, batch, err := ClientForBatch(db, *setBatchActivationCodesBatch)
		if err != nil {
			return err
		}

		entries, err := db.GetAllSimEntriesForBatch(batch.BatchId)
		if err != nil {
			return err
		}

		if len(entries) != batch.Quantity {
			return fmt.Errorf("batch quantity retrieved from database (%d) different from batch quantity (%d)", len(entries), batch.Quantity)
		}

		// XXX Is this really necessary? I don't think so
		var mutex = &sync.Mutex{}

		var waitgroup sync.WaitGroup

		// Limit concurrency of the for-loop below
		// to 160 goroutines.  The reason is that if we get too
		// many we run out of file descriptors, and we don't seem to
		// get much speedup after hundred or so.

		concurrency := 160
		sem := make(chan bool, concurrency)
		tx := db.Begin()
		for _, entry := range entries {

			//
			// Only apply activation if not already noted in the
			// database.

			if entry.ActivationCode == "" {

				sem <- true

				waitgroup.Add(1)
				go func(entry model.SimEntry) {

					defer func() { <-sem }()

					result, err := client.ActivateIccid(entry.Iccid)
					if err != nil {
						panic(err)
					}

					mutex.Lock()
					fmt.Printf("%s, %s\n", entry.Iccid, result.ACToken)
					db.UpdateActivationCode(entry.Id, result.ACToken)
					mutex.Unlock()
					waitgroup.Done()
				}(entry)
			}
		}

		waitgroup.Wait()
		for i := 0; i < cap(sem); i++ {
			sem <- true
		}
		tx.Commit()

	case "iccids-bulk-activate":
		client, err := ClientForVendor(db, *bulkActivateIccidsVendor)
		if err != nil {
			return err
		}

		file, err := os.Open(*bulkActivateIccidsIccids)
		if err != nil {
			log.Fatal(err)
		}
		defer file.Close()

		scanner := bufio.NewScanner(file)
		var mutex = &sync.Mutex{}
		var waitgroup sync.WaitGroup
		for scanner.Scan() {
			iccid := scanner.Text()
			waitgroup.Add(1)
			go func(i string) {

				result, err := client.ActivateIccid(i)
				if err != nil {
					panic(err)
				}
				mutex.Lock()
				fmt.Printf("%s, %s\n", i, result.ACToken)
				mutex.Unlock()
				waitgroup.Done()
			}(iccid)
		}

		waitgroup.Wait()

		if err := scanner.Err(); err != nil {
			log.Fatal(err)
		}

	default:
		return fmt.Errorf("unknown command: '%s'", cmd)
	}

	return nil
}

func checkEs2TargetState(target string) error {
	if target != "AVAILABLE" {
		return fmt.Errorf("target ES2+ state unexpected, legal value(s) is(are): 'AVAILABLE'")
	} else {
		return nil
	}
}

///
///    Input batch management
///

func GenerateInputFile(batch *model.Batch) string {
	result := "*HEADER DESCRIPTION\n" +
		"***************************************\n" +
		fmt.Sprintf("Customer        : %s\n", batch.Customer) +
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

func ClientForVendor(db *store.SimBatchDB, vendorName string) (es2plus.Es2PlusClient, error) {
	vendor, err := db.GetProfileVendorByName(vendorName)
	if err != nil {
		return nil, err
	}

	if vendor == nil {
		return nil, fmt.Errorf("unknown profile vendor '%s'", vendorName)
	}

	hostport := fmt.Sprintf("%s:%d", vendor.Es2PlusHost, vendor.Es2PlusPort)
	return es2plus.NewClient(vendor.Es2PlusCert, vendor.Es2PlusKey, hostport, vendor.Es2PlusRequesterId), nil
}

func ClientForBatch(db *store.SimBatchDB, batchName string) (es2plus.Es2PlusClient, *model.Batch, error) {

	batch, err := db.GetBatchByName(batchName)
	if err != nil {
		return nil, nil, err
	}
	if batch == nil {
		return nil, nil, fmt.Errorf("unknown batch '%s'", batchName)
	}

	client, err := ClientForVendor(db, batch.ProfileVendor)
	if err != nil {
		return nil, nil, err
	}

	return client, batch, nil
}
