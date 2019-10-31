package storage

import (
	"github.com/jmoiron/sqlx"
	_ "github.com/mattn/go-sqlite3"
	"testing"
)

func TestMemoryDbPing(t *testing.T) {

	var db *sqlx.DB

	// exactly the same as the built-in
	db, err := sqlx.Open("sqlite3", ":memory:")
	if err != nil {
		t.Errorf("Didn't manage to open sqlite3 in-memory database. '%s'", err)
	}
	// from a pre-existing sql.DB; note the required driverName
	// db = sqlx.NewDb(sql.Open("sqlite3", ":memory:"), "sqlite3")

	// force a connection and test that it worked
	err = db.Ping()
}
