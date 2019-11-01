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

func TestGenerateInputBatchTable(t *testing.T) {
	GenerateInputBatchTable(sdb)
	// TODO: Try a CRUD here, spread it out over multiple methods, and our work is done.

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

	res := sdb.db.MustExec("INSERT INTO INPUT_BATCH (name, customer, profileType, orderDate, batchNo, quantity, firstIccid, firstImsi) values (?,?,?,?,?,?,?,?) ",
		theBatch.Name,
		theBatch.Customer,
		theBatch.ProfileType,
		theBatch.OrderDate,
		theBatch.BatchNo,
		theBatch.Quantity,
		theBatch.FirstIccid,
		theBatch.FirstImsi,
	)

	theBatch.Id, err = res.LastInsertId()
	if err != nil {
		fmt.Errorf("Getting last inserted id failed '%s'", err)
	}

	rows, err := sdb.db.Query("select id, name, customer, profileType, orderDate, batchNo, quantity, firstIccid, firstImsi FROM INPUT_BATCH")
	if err != nil {
		fmt.Errorf("Reading query failed '%s'", err)
	}

	assert.Assert(t, rows != nil, "Rows shouldn't be nil in prepared roundtrip")

	noOfRows := 0
	for rows.Next() {
		var id int64
		var Name string
		var Customer string
		var ProfileType string
		var OrderDate string
		var BatchNo string
		var Quantity int
		var FirstIccid string
		var FirstImsi string
		err = rows.Scan(&id, &Name, &Customer, &ProfileType, &OrderDate, &BatchNo, &Quantity, &FirstIccid, &FirstImsi)

		queryResult := model.InputBatch{
			Id:          id,
			Name:        Name,
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

	var result2 model.InputBatch
	err = sdb.db.Get(&result2, "select * from INPUT_BATCH limit 1")
	if err != nil {
		fmt.Errorf("Get query failed '%s'", err)
	}
	if !reflect.DeepEqual(theBatch, result2) {
		fmt.Errorf("Read/write inequality for input batch")
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
