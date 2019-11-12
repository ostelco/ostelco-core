package store

import (
	"fmt"
	_ "github.com/mattn/go-sqlite3"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"
	"gotest.tools/assert"
	"os"
	"reflect"
	"testing"
)

var sdb *SimBatchDB
var err error

func TestMain(m *testing.M) {
	setup()
	code := m.Run()
	shutdown()
	os.Exit(code)
}

func setup() {

	// In memory database fails, so we try this gonzo method of getting
	// a fresh database
	filename := "bazunka.db"
	// delete file, ignore any errors
	os.Remove(filename)

	sdb, err = OpenFileSqliteDatabase(filename)
	// sdb, err = NewInMemoryDatabase()
	if err != nil {
		panic(fmt.Errorf("Couldn't open new in memory database  '%s", err))
	}
	/*
		batches, err := sdb.GetAllBatches()
		if err == nil {
			panic(fmt.Sprintf("Couldn't generate tables  '%s'", err))
		}

		if len(batches) != 0 {
			panic(fmt.Sprintf("batches already registred, test misconfigured"))
		}
	*/

	err = sdb.GenerateTables()
	if err != nil {
		panic(fmt.Sprintf("Couldn't generate tables  '%s'", err))
	}

	cleanTables()
}

func cleanTables() {

	// return
	// The tables don't seem to be guaranteed to be empty
	simProfileDeletionResult, err := sdb.Db.Exec("DELETE FROM SIM_PROFILE")
	if err != nil {
		panic(fmt.Sprintf("Couldn't delete SIM_PROFILE  '%s'", err))
	}
	batchDeleteResult, err := sdb.Db.Exec("DELETE FROM BATCH")
	if err != nil {
		panic(fmt.Sprintf("Couldn't delete BATCH  '%s'", err))
	}

	simRows, _ := simProfileDeletionResult.RowsAffected()
	batchRows, _ := batchDeleteResult.RowsAffected()
	fmt.Printf("simProfileDeletionResult = %d, batchDeleteResult=%d\n", simRows, batchRows)
}

func shutdown() {
	cleanTables()
	err := sdb.DropTables()
	if err != nil {
		panic(fmt.Sprintf("Couldn't drop tables  '%s'", err))
	}
	sdb.Db.Close()
}

// ... just to know that everything is sane.
func TestMemoryDbPing(t *testing.T) {
	err = sdb.Db.Ping()
	if err != nil {
		t.Errorf("Could not ping in-memory database. '%s'", err)
	}
}

func injectTestBatch() *model.Batch {
	theBatch := model.Batch{
		Name:            "SOME UNIQUE NAME",
		OrderDate:       "20200101",
		Customer:        "firstInputBatch",
		ProfileType:     "banana",
		BatchNo:         "100",
		Quantity:        100,
		Url:             "http://vg.no",
		FirstIccid:      "1234567890123456789",
		FirstImsi:       "123456789012345",
		MsisdnIncrement: -1,
	}

	batch, _ := sdb.GetBatchByName(theBatch.Name)
	if batch.BatchId != -1 {
		panic(fmt.Errorf("Duplicate batch detected '%s'", theBatch.Name))
	}

	err := sdb.CreateBatch(&theBatch)
	if err != nil {
		panic(err)
	}
	return &theBatch
}

func TestGetBatchById(t *testing.T) {

	cleanTables()
	batch, _ := sdb.GetBatchByName("SOME UNIQUE NAME")
	if batch.BatchId != -1  {
		t.Errorf("Duplicate detected, error in test setup")
	}

	theBatch := injectTestBatch()

	firstInputBatch, _ := sdb.GetBatchById(theBatch.BatchId)
	if !reflect.DeepEqual(*firstInputBatch, *theBatch) {
		t.Errorf("getBatchById failed")
	}
}

func TestGetAllBatches(t *testing.T) {

	cleanTables()

	allBatches, err := sdb.GetAllBatches()
	if err != nil {
		t.Errorf("Reading query failed '%s'", err)
	}

	assert.Equal(t, len(allBatches), 0)

	theBatch := injectTestBatch()

	allBatches, err = sdb.GetAllBatches()
	if err != nil {
		t.Errorf("Reading query failed '%s'", err)
	}

	assert.Equal(t, len(allBatches), 1)

	firstInputBatch := allBatches[0]
	if !reflect.DeepEqual(firstInputBatch, theBatch) {
		t.Errorf("getBatchById failed, returned batch not equal to initial batch")
	}
}

func declareTestBatch(t *testing.T) *model.Batch {

	theBatch, err := sdb.DeclareBatch(
		"Name",
		 false,
		"Customer",
		"8778fsda",             // batch number
		"20200101",             // date string
		"89148000000745809013", // firstIccid string,
		"89148000000745809013", // lastIccid string,
		"242017100012213",      // firstIMSI string,
		"242017100012213",      // lastIMSI string,
		"47900184",             // firstMsisdn string,
		"47900184",             // lastMsisdn string,
		"BAR_FOOTEL_STD",       //profileType string,
		"1",                    // batchLengthString string,
		"LOL",                  // hssVendor string,
		"localhost",            // uploadHostname string,
		"8088",                 // uploadPortnumber string,
		"snuff",                // profileVendor string,
		"ACTIVE")               // initialHlrActivationStatusOfProfiles string

	if err != nil {
		t.Fatal(err)
	}
	return theBatch
}

func TestDeclareBatch(t *testing.T) {
	theBatch := declareTestBatch(t)
	retrievedValue, _ := sdb.GetBatchById(theBatch.BatchId)
	if !reflect.DeepEqual(*retrievedValue, *theBatch) {
		t.Fatal("getBatchById failed, stored batch not equal to retrieved batch")
	}

	retrievedEntries, err := sdb.GetAllSimEntriesForBatch(theBatch.BatchId)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, 1, len(retrievedEntries))

	// TODO: Add check for content of retrieved entity

}

func entryEqual(a *model.SimEntry, b *model.SimEntry) bool {
	return ((a.ActivationCode == b.ActivationCode) && (a.BatchID == b.BatchID) && (a.RawIccid == b.RawIccid) && a.IccidWithChecksum == b.IccidWithChecksum && a.IccidWithoutChecksum == b.IccidWithoutChecksum && a.Iccid == b.Iccid && a.Imsi == b.Imsi && a.Msisdn == b.Msisdn && a.Ki == b.Ki)
}

func TestDeclareAndRetrieveSimEntries(t *testing.T) {
	cleanTables()
	theBatch := declareTestBatch(t)
	batchId := theBatch.BatchId

	entry := model.SimEntry{
		BatchID:              batchId,
		RawIccid:             "1",
		IccidWithChecksum:    "2",
		IccidWithoutChecksum: "3",
		Iccid:                "4",
		Imsi:                 "5",
		Msisdn:               "6",
		Ki:                   "7",
		ActivationCode:       "8",
	}

	// assert.Equal(t, 0, entry.Id)
	sdb.CreateSimEntry(&entry)
	assert.Assert(t, entry.Id != 0)

	retrivedEntry, err := sdb.GetSimEntryById(entry.Id)
	if err != nil {
		t.Fatal(err)
	}
	if !entryEqual(retrivedEntry, &entry) {
		t.Fatal("Retrieved and stored sim entry are different")
	}
}
