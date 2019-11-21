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

var (
	sdb           *SimBatchDB
	sdbSetupError error
)

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
	_ = os.Remove(filename)

	sdb, sdbSetupError =
		OpenFileSqliteDatabase(filename)

	if sdbSetupError != nil {
		panic(fmt.Errorf("Couldn't open new in memory database  '%s", sdbSetupError))
	}

	if sdb == nil {
		panic("Returned null database object")
	}

	if sdbSetupError = sdb.GenerateTables(); sdbSetupError != nil {
		panic(fmt.Sprintf("Couldn't generate tables  '%s'", sdbSetupError))
	}

	cleanTables()
}

func cleanTables() {
	fmt.Println("Cleaning tables ...")
	_, err := sdb.Db.Exec("DELETE FROM SIM_PROFILE")
	if err != nil {
		panic(fmt.Sprintf("Couldn't delete SIM_PROFILE  '%s'", err))
	}

	_, err = sdb.Db.Exec("DELETE FROM BATCH")
	if err != nil {
		panic(fmt.Sprintf("Couldn't delete BATCH  '%s'", err))
	}

	_, err = sdb.Db.Exec("DELETE FROM PROFILE_VENDOR")
	if err != nil {
		panic(fmt.Sprintf("Couldn't delete PROFILE_VENDOR  '%s'", err))
	}
	fmt.Println("    Cleaned tables ...")

	vendor, _ := sdb.GetProfileVendorByName("Durian")
	if vendor != nil {
		fmt.Println("   Unclean Durian detected")
	}

}

func shutdown() {
	cleanTables()
	if err := sdb.DropTables(); err != nil {
		panic(fmt.Sprintf("Couldn't drop tables  '%s'", err))
	}
	_ = sdb.Db.Close()
}

// ... just to know that everything is sane.
func TestMemoryDbPing(t *testing.T) {
	if err := sdb.Db.Ping(); err != nil {
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
		URL:             "http://vg.no",
		FirstIccid:      "1234567890123456789",
		FirstImsi:       "123456789012345",
		ProfileVendor:   "Durian",
		MsisdnIncrement: -1,
	}

	batch, _ := sdb.GetBatchByName(theBatch.Name)
	if batch != nil {
		panic(fmt.Errorf("duplicate batch detected '%s'", theBatch.Name))
	}

	err := sdb.CreateBatch(&theBatch)
	if err != nil {
		panic(err)
	}
	return &theBatch
}

func TestGetBatchByID(t *testing.T) {

	fmt.Print("TestGetBatchByID starts")
	cleanTables()
	batch, _ := sdb.GetBatchByName("SOME UNIQUE NAME")
	if batch != nil {
		t.Errorf("Duplicate detected, error in test setup")
	}

	// Inject a sample batch
	injectTestprofileVendor(t)
	theBatch := injectTestBatch()

	batchByID, _ := sdb.GetBatchByID(theBatch.BatchID)
	if !reflect.DeepEqual(batchByID, theBatch) {

		t.Logf("theBatch  = %v\n", theBatch)
		t.Logf("batchByID  = %v\n", batchByID)
		t.Errorf("getBatchByID failed")
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
	if !reflect.DeepEqual(firstInputBatch, *theBatch) {
		t.Errorf("getBatchById failed, returned batch not equal to initial batch")
	}
}

//noinspection GoUnusedParameter
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
		"Durian",               // profileVendor string,
		"ACTIVE")               // initialHlrActivationStatusOfProfiles string

	if err != nil {
		panic(err)
	}
	return theBatch
}

func TestDeclareBatch(t *testing.T) {
	injectTestprofileVendor(t)
	theBatch := declareTestBatch(t)

	retrievedValue, _ := sdb.GetBatchByID(theBatch.BatchID)
	if retrievedValue == nil {
		t.Fatalf("Null retrievedValue")
	}
	if !reflect.DeepEqual(retrievedValue, theBatch) {
		t.Fatal("getBatchById failed, stored batch not equal to retrieved batch")
	}

	retrievedEntries, err := sdb.GetAllSimEntriesForBatch(theBatch.BatchID)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, 1, len(retrievedEntries))

	// TODO: Add check for content of retrieved entity
}

//noinspection GoUnusedParameter
func injectTestprofileVendor(t *testing.T) *model.ProfileVendor {
	v := &model.ProfileVendor{
		Name:               "Durian",
		Es2PlusCert:        "cert",
		Es2PlusKey:         "key",
		Es2PlusHost:        "host",
		Es2PlusPort:        4711,
		Es2PlusRequesterId: "1.2.3",
	}

	if err := sdb.CreateProfileVendor(v); err != nil {
		panic(err)
		// t.Fatal(err)
	}
	return v
}

func TestDeclareAndRetrieveProfileVendorEntry(t *testing.T) {
	cleanTables()

	v := injectTestprofileVendor(t)

	nameRetrievedVendor, err := sdb.GetProfileVendorByName(v.Name)
	if err != nil {
		t.Fatal(err)
	}

	if !reflect.DeepEqual(nameRetrievedVendor, v) {
		t.Fatalf("name retrieved and stored profile vendor entries are different, %v v.s. %v", nameRetrievedVendor, v)
	}

	idRetrievedVendor, err := sdb.GetProfileVendorByID(v.Id)
	if err != nil {
		t.Fatal(err)
	}

	if !reflect.DeepEqual(idRetrievedVendor, v) {
		t.Fatalf("name retrieved and stored profile vendor entries are different, %v v.s. %v", idRetrievedVendor, v)
	}
}

func TestDeclareAndRetrieveSimEntries(t *testing.T) {
	cleanTables()
	injectTestprofileVendor(t)
	theBatch := declareTestBatch(t)
	batchID := theBatch.BatchID

	entry := model.SimEntry{
		BatchID:              batchID,
		RawIccid:             "1",
		IccidWithChecksum:    "2",
		IccidWithoutChecksum: "3",
		Iccid:                "4",
		Imsi:                 "5",
		Msisdn:               "6",
		Ki:                   "7",
		ActivationCode:       "8",
	}

	err := sdb.CreateSimEntry(&entry)
	if err != nil {
		t.Fatal(err)
	}
	assert.Assert(t, entry.Id != 0)

	retrivedEntry, err := sdb.GetSimEntryById(entry.Id)
	if err != nil {
		t.Fatal(err)
	}

	if !reflect.DeepEqual(retrivedEntry, &entry) {
		t.Fatal("Retrieved and stored sim entry are different")
	}
}

func TestSimBatchDB_UpdateSimEntryKi(t *testing.T) {
	cleanTables()
	injectTestprofileVendor(t)
	theBatch := declareTestBatch(t)
	batchID := theBatch.BatchID

	entry := model.SimEntry{
		BatchID:              batchID,
		RawIccid:             "1",
		IccidWithChecksum:    "2",
		IccidWithoutChecksum: "3",
		Iccid:                "4",
		Imsi:                 "5",
		Msisdn:               "6",
		Ki:                   "7",
		ActivationCode:       "8",
	}

	err := sdb.CreateSimEntry(&entry)
	if err != nil {
		t.Fatal(err)
	}

	assert.Assert(t, entry.Id != 0)

	newKi := "12"
	err = sdb.UpdateSimEntryKi(entry.Id, newKi)

	if err != nil {
		t.Fatal(err)
	}

	retrivedEntry, err := sdb.GetSimEntryById(entry.Id)
	if err != nil {
		t.Fatal(err)
	}

	if retrivedEntry.Ki != "12" {
		t.Fatalf("Retrieved (%s) and stored  (%s) ki values are different", retrivedEntry.Ki, newKi)
	}
}
