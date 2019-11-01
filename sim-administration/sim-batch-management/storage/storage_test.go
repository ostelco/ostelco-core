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

/* TODO: Just for reference, use the one in model instead.
type InputBatch struct {
	Customer    string
	ProfileType string
	OrderDate   string
	BatchNo     string
	Quantity    int
	FirstIccid  int
	FirstImsi   int
}

course := Course{}
courses := []Course{}

db.Get(&course, "SELECT name AS course_name FROM courses LIMIT 1")
db.Select(&courses, "SELECT name AS course_name FROM courses")

*/

func InsertInputBatch(theBatch *model.InputBatch) {

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

func TestGenerateInputBatchTable(t *testing.T) {
	GenerateInputBatchTable(sdb)

	theBatch := model.InputBatch{
		Name:        "SOME UNIQUE NAME",
		Customer:    "foo",
		ProfileType: "banana",
		OrderDate:   "apple",
		BatchNo:     "100",
		Quantity:    100,
		FirstIccid:  "1234567890123456789",
		FirstImsi:   "123456789012345",
	}

	InsertInputBatch(&theBatch)

	// XXX Refactor into some "get all", "getById", "getByName" methods
	//     and it will all be awsome.   Continue that to completion for input
	//     batches. Wrap it in  an interface, and hook that interface up to
	//     the command line processor.  Rinse&repeat.
	allBatches, err := getAllInputBatches()
	if err != nil {
		fmt.Errorf("Reading query failed '%s'", err)
	}

	assert.Equal(t, len(allBatches), 1)

	var result2 model.InputBatch
	err = sdb.db.Get(&result2, "select * from INPUT_BATCH limit 1")
	if err != nil {
		fmt.Errorf("Get query failed '%s'", err)
	}
	if !reflect.DeepEqual(theBatch, result2) {
		fmt.Errorf("Read/write inequality for input batch")
	}

	foo, _ := getInputBatchById(1)
	if !reflect.DeepEqual(foo, theBatch) {
		fmt.Errorf("getBatchById failed")
	}
}

func getAllInputBatches() ([]model.InputBatch, error) {
	result := []model.InputBatch{}
	return result, sdb.db.Select(&result, "SELECT * from INPUT_BATCH")
}

func getInputBatchById(id int64) (*model.InputBatch, error) {
	var result model.InputBatch
	return &result, sdb.db.Get(&result, "select * from INPUT_BATCH where id = ?", id)
}

//
//  GROWTH ZONE: Where the implementation grows, eventually being moved into
//               its appropriate location.
//

type SimBatchDB struct {
	db *sqlx.DB
	mu sync.Mutex
}

func GenerateInputBatchTable(sdb *SimBatchDB) {
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

	result, err := sdb.db.Exec(foo)
	if err != nil {
		fmt.Errorf("Table creation failed. '%s', '%s'", err, result)
	}
}
