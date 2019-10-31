package storage

import (
	"fmt"
	"github.com/jmoiron/sqlx"
	_ "github.com/mattn/go-sqlite3"
	"os"
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
	// exactly the same as the built-in
	db, err := sqlx.Open("sqlite3", ":memory:")
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
	// Try a CRUD here, spread it out over multiple methods, and our work is done.
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
	foo := `CREATE TABLE IF NOT EXISTS SIMBATCH.INPUT_BATCH (
	CUSTOMER VARCHAR,
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
