package storage

import (
	// "database/sql"
	"fmt"
	"github.com/jmoiron/sqlx"
	_ "github.com/mattn/go-sqlite3"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"
	"gotest.tools/assert"
	"os"
	"reflect"
	"sync"
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
	db, err := sqlx.Open("sqlite3", ":memory:")
	// db, err := sqlx.Open("sqlite3", "foobar.db")
	if err != nil {
		fmt.Errorf("Didn't manage to open sqlite3 in-memory database. '%s'", err)
	}

	sdb = &SimBatchDB{db: db, mu: sync.Mutex{}}

}

func shutdown() {
}

// ... just to know that everything is sane.
func TestMemoryDbPing(t *testing.T) {
	sdb.mu.Lock()
	defer sdb.mu.Unlock()
	err = sdb.db.Ping()
	if err != nil {
		fmt.Errorf("Could not ping in-memory database. '%s'", err)
	}
}

func TestInputBatchRoundtrip(t *testing.T) {
	sdb.GenerateTables()

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

//
//  GROWTH ZONE: Where the implementation grows, eventually being moved into
//               its appropriate location.
//

type SimBatchDB struct {
	db *sqlx.DB
	mu sync.Mutex
}

// Store is the storage interface for ds.
type Store interface {

	//Generate all the tables if not already there.
	GenerateTables() error

	// Input Batches
	Create(doc *model.InputBatch) error
	GetAllInputBatches(id string) ([]model.InputBatch, error)
	GetInputBatchById(id int64) (*model.InputBatch, error)
	GetInputBatchByName(id string) (*model.InputBatch, error)
}

func (sdb SimBatchDB) GetAllInputBatches() ([]model.InputBatch, error) {
	result := []model.InputBatch{}
	return result, sdb.db.Select(&result, "SELECT * from INPUT_BATCH")
}

func (sdb SimBatchDB) GetInputBatchById(id int64) (*model.InputBatch, error) {
	var result model.InputBatch
	return &result, sdb.db.Get(&result, "select * from INPUT_BATCH where id = ?", id)
}

func (sdb SimBatchDB) GetInputBatchByName(name string) (*model.InputBatch, error) {
	var result model.InputBatch
	return &result, sdb.db.Get(&result, "select * from INPUT_BATCH where name = ?", name)
}

func (sdb SimBatchDB) Create(theBatch *model.InputBatch) {

	res := sdb.db.MustExec("INSERT INTO INPUT_BATCH (name, customer, profileType, orderDate, batchNo, quantity, firstIccid, firstImsi) values (?,?,?,?,?,?,?,?) ",
		(*theBatch).Name,
		(*theBatch).Customer,
		(*theBatch).ProfileType,
		(*theBatch).OrderDate,
		(*theBatch).BatchNo,
		(*theBatch).Quantity,
		(*theBatch).FirstIccid,
		(*theBatch).FirstImsi,
	)

	theBatch.Id, err = res.LastInsertId()
	if err != nil {
		fmt.Errorf("Getting last inserted id failed '%s'", err)
	}
}

func (sdb *SimBatchDB) GenerateTables() error {
	sdb.mu.Lock()
	defer sdb.mu.Unlock()
	foo := `CREATE TABLE IF NOT EXISTS INPUT_BATCH (
    id integer primary key autoincrement,
    name VARCHAR NOT NULL,
	customer VARCHAR NOT NULL,
	profileType VARCHAR NOT NULL,
	orderDate VARCHAR NOT NULL,
	batchNo VARCHAR NOT NULL,
	quantity INTEGER NOT NULL,
	firstIccid VARCHAR,
	firstImsi VARCHAR
	)`

	_, err := sdb.db.Exec(foo)
	return err
}
