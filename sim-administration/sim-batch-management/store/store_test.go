package store

import (
	"fmt"
	_ "github.com/mattn/go-sqlite3"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/store"
	"gotest.tools/assert"
	"os"
	"reflect"
	"testing"
)

var sdb *store.SimBatchDB
var err error

func TestMain(m *testing.M) {
	setup()
	code := m.Run()
	shutdown()
	os.Exit(code)
}

func setup() {
	sdb, err = store.NewInMemoryDatabase()
	if err != nil {
		fmt.Errorf("Couldn't open new in memory database  '%s", err)
	}
	err = sdb.GenerateTables()
	if err != nil {
		fmt.Errorf("Couldn't generate tables  '%s'", err)
	}
}

func shutdown() {
}

// ... just to know that everything is sane.
func TestMemoryDbPing(t *testing.T) {
	err = sdb.Db.Ping()
	if err != nil {
		fmt.Errorf("Could not ping in-memory database. '%s'", err)
	}
}

func TestInputBatchRoundtrip(t *testing.T) {

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

	sdb.Create(&theBatch)
	allBatches, err := sdb.GetAllInputBatches()
	if err != nil {
		fmt.Errorf("Reading query failed '%s'", err)
	}

	assert.Equal(t, len(allBatches), 1)

	firstInputBatch, _ := sdb.GetInputBatchById(1)
	if !reflect.DeepEqual(*firstInputBatch, theBatch) {
		fmt.Errorf("getBatchById failed")
	}
}

func TestDeclareBatch(t *testing.T) {
	theBatch, err := sdb.DeclareBatch(
		"20200101", // date string
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
		panic(err)
	}

	retrievedValue, _ := sdb.GetInputBatchById(theBatch.Id)
	if !reflect.DeepEqual(*retrievedValue, *theBatch) {
		fmt.Errorf("getBatchById failed")
	}
}
