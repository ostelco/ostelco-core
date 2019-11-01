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
*/

func TestGenerateInputBatchTable(t *testing.T) {
	GenerateInputBatchTable(sdb)
	// TODO: Try a CRUD here, spread it out over multiple methods, and our work is done.

	theBatch := model.InputBatch{
		Customer:    "foo",
		ProfileType: "banana",
		OrderDate:   "apple",
		BatchNo:     "100",
		Quantity:    100,
		FirstIccid:  1234567890123456789,
		FirstImsi:   123456789012345,
	}

	insertionResult := sdb.db.MustExec("INSERT INTO INPUT_BATCH (CUSTOMER, PROFILE_TYPE, ORDER_DATE, BATCH_NO, QUANTITY, FIRST_ICCID, FIRST_IMSI) values (?,?,?,?,?,?,?) ",
		theBatch.Customer,
		theBatch.ProfileType,
		theBatch.OrderDate,
		theBatch.BatchNo,
		theBatch.Quantity,
		theBatch.FirstIccid,
		theBatch.FirstImsi,
	)

	fmt.Println("The batch = ", insertionResult)

	rows, err := sdb.db.Query("select CUSTOMER, PROFILE_TYPE, ORDER_DATE, BATCH_NO, QUANTITY, FIRST_ICCID, FIRST_IMSI FROM INPUT_BATCH")
	if err != nil {
		fmt.Errorf("Reading query failed '%s'", err)
	}

	assert.Assert(t, rows != nil, "Rows shouldn't be nil in prepared roundtrip")

	noOfRows := 0
	for rows.Next() {
		var Customer string
		var ProfileType string
		var OrderDate string
		var BatchNo string
		var Quantity int
		var FirstIccid int
		var FirstImsi int
		err = rows.Scan(&Customer, &ProfileType, &OrderDate, &BatchNo, &Quantity, &FirstIccid, &FirstImsi)

		queryResult := model.InputBatch{
			Customer:    Customer,
			ProfileType: ProfileType,
			OrderDate:   OrderDate,
			BatchNo:     BatchNo,
			Quantity:    Quantity,
			FirstIccid:  FirstIccid,
			FirstImsi:   FirstImsi,
		}

		if !reflect.DeepEqual(theBatch, queryResult) {
			fmt.Errorf("Read/write inequality for input batch")
		}
		noOfRows += 1
	}
	assert.Equal(t, noOfRows, 1)
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
	CUSTOMER VARCHAR NOT NULL,
	PROFILE_TYPE VARCHAR NOT NULL,
	ORDER_DATE VARCHAR NOT NULL,
	BATCH_NO VARCHAR NOT NULL,
	QUANTITY INTEGER NOT NULL,
	FIRST_ICCID BIGINT,
	FIRST_IMSI BIGINT
	)`

	_, err := sdb.db.Exec(foo)
	if err != nil {
		fmt.Errorf("Table creation failed. '%s'", err)
	}
}
