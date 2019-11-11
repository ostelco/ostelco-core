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
	// sdb, err = OpenFileSqliteDatabase("bazunka.db")
	sdb, err = NewInMemoryDatabase()
	if err != nil {
		fmt.Sprintf("Couldn't open new in memory database  '%s", err)
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

/*
	// The tables don't seem to be guaranteed to be empty
	foo, err := sdb.Db.Exec("DELETE FROM SIM_PROFILE")
	if err != nil {
		panic(fmt.Sprintf("Couldn't delete SIM_PROFILE  '%s'", err))
	}
	bar, err := sdb.Db.Exec("DELETE FROM BATCH")
	if err != nil {
		panic(fmt.Sprintf("Couldn't delete BATCH  '%s'", err))
	}

	fooRows, _ := foo.RowsAffected()
	barRows,_ := bar.RowsAffected()
	fmt.Printf("foo = %d, bar=%d\n",fooRows, barRows)

 */
}

func shutdown() {
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
		fmt.Errorf("Could not ping in-memory database. '%s'", err)
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
	if batch != nil {
		fmt.Errorf("Duplicate batch detected %s", theBatch.Name)
	}

	err := sdb.CreateBatch(&theBatch)
	if err != nil {
		panic(err)
	}
	return &theBatch
}

func TestGetBatchById(t *testing.T) {

	theBatch := injectTestBatch()

	firstInputBatch, _ := sdb.GetBatchById(theBatch.BatchId)
	if !reflect.DeepEqual(*firstInputBatch, *theBatch) {
		fmt.Errorf("getBatchById failed")
	}
}

func TestGetAllBatches(t *testing.T) {


	allBatches, err := sdb.GetAllBatches()
	if err != nil {
		fmt.Errorf("Reading query failed '%s'", err)
	}

	assert.Equal(t, len(allBatches), 0)

	theBatch := injectTestBatch()

	allBatches, err = sdb.GetAllBatches()
	if err != nil {
		fmt.Errorf("Reading query failed '%s'", err)
	}

	assert.Equal(t, len(allBatches), 1)

	firstInputBatch := allBatches[0]
	if !reflect.DeepEqual(firstInputBatch, theBatch) {
		fmt.Errorf("getBatchById failed")
	}
}

func declareTestBatch(t *testing.T) *model.Batch {

	theBatch, err := sdb.DeclareBatch(
		"Name",
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
		"ACTIVE") // initialHlrActivationStatusOfProfiles string

	if err != nil {
		t.Fatal(err)
	}
	return theBatch
}

func TestDeclareBatch(t *testing.T) {
	theBatch := declareTestBatch(t)
	retrievedValue, _ := sdb.GetBatchById(theBatch.BatchId)
	if !reflect.DeepEqual(*retrievedValue, *theBatch) {
		t.Fatal("getBatchById failed")
	}
}

func entryEqual(a *model.SimEntry, b *model.SimEntry) bool {
	return ((a.ActivationCode == b.ActivationCode) && (a.BatchID == b.BatchID) && (a.RawIccid == b.RawIccid) && a.IccidWithChecksum == b.IccidWithChecksum	&& a.IccidWithoutChecksum == b.IccidWithoutChecksum && a.Iccid == b.Iccid && a.Imsi == b.Imsi && a.Msisdn == b.Msisdn && a.Ki == b.Ki)
}

func TestDeclareAndRetrieveSimEntries(t *testing.T) {
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

	retrievedEntries, err := sdb.GetAllSimEntriesForBatch(entry.BatchID)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, 1, len(retrievedEntries))

	retrivedEntry = &(retrievedEntries[0])

	if !entryEqual(retrivedEntry, &entry) {
		t.Fatal("Retrieved and stored sim entry are different")
	}
}
