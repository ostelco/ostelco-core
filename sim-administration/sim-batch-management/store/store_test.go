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
		fmt.Errorf("Couldn't open new in memory database")
	}
	sdb.GenerateTables()
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

	theBatch := model.InputBatch{
		Name:        "SOME UNIQUE NAME",
		Customer:    "firstInputBatch",
		ProfileType: "banana",
		OrderDate:   "apple",
		BatchNo:     "100",
		Quantity:    100,
		FirstIccid:  "1234567890123456789",
		FirstImsi:   "123456789012345",
	}

	sdb.Create(&theBatch)
	allBatches, err := sdb.GetAllInputBatches()
	if err != nil {
		fmt.Errorf("Reading query failed '%s'", err)
	}

	assert.Equal(t, len(allBatches), 1)

	firstInputBatch, _ := sdb.GetInputBatchById(1)
	if !reflect.DeepEqual(firstInputBatch, theBatch) {
		fmt.Errorf("getBatchById failed")
	}
}
