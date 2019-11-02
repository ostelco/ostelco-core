package store

import (
	"fmt"
	_ "github.com/mattn/go-sqlite3"
	"github.com/jmoiron/sqlx"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"
	"os"
)

type Store interface {
	GenerateTables() error

	Create(theBatch *model.Batch) error
	GetAllInputBatches(id string) ([]model.Batch, error)
	GetInputBatchById(id int64) (*model.Batch, error)
	GetInputBatchByName(id string) (*model.Batch, error)
}

type SimBatchDB struct {
	Db *sqlx.DB
}

func NewInMemoryDatabase() (*SimBatchDB, error) {
	db, err := sqlx.Open("sqlite3", ":memory:")
	if err != nil {
		return nil, err
	}
	return &SimBatchDB{Db: db}, nil
}

func OpenFileSqliteDatabaseFromPathInEnvironmentVariable(variablename string) (*SimBatchDB, error) {
	variableValue := os.Getenv(variablename)
	db, err := OpenFileSqliteDatabase(variableValue)
	return db, err
}

func OpenFileSqliteDatabase(path string) (*SimBatchDB, error) {
	db, err := sqlx.Open("sqlite3", "foobar.db")
	if err != nil {
		return nil, err
	}
	return &SimBatchDB{Db: db}, nil
}

func (sdb SimBatchDB) GetAllInputBatches() ([]model.Batch, error) {
	result := []model.Batch{}
	return result, sdb.Db.Select(&result, "SELECT * from BATCH")
}

func (sdb SimBatchDB) GetInputBatchById(id int64) (*model.Batch, error) {
	var result model.Batch
	return &result, sdb.Db.Get(&result, "select * from BATCH where id = ?", id)
}

func (sdb SimBatchDB) GetInputBatchByName(name string) (*model.Batch, error) {
	var result model.Batch
	return &result, sdb.Db.Get(&result, "select * from BATCH where name = ?", name)
}

func (sdb SimBatchDB) Create(theBatch *model.Batch) {

	res := sdb.Db.MustExec("INSERT INTO BATCH (name, customer, profileType, orderDate, batchNo, quantity, firstIccid, firstMsisdn, firstImsi, msisdnIncrement, iccidIncrement, firstMsisdn, url) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ",
		(*theBatch).Name,
		(*theBatch).Customer,
		(*theBatch).ProfileType,
		(*theBatch).OrderDate,
		(*theBatch).BatchNo,
		(*theBatch).Quantity,
		(*theBatch).FirstIccid,
		(*theBatch).FirstImsi,
		(*theBatch).FirstMsisdn,
		(*theBatch).MsisdnIncrement,
		(*theBatch).IccidIncrement,
		(*theBatch).ImsiIncrement,
		(*theBatch).FirstMsisdn,
		(*theBatch).Url,
	)

	id, err := res.LastInsertId()
	if err != nil {
		fmt.Errorf("Getting last inserted id failed '%s'", err)
	}
	theBatch.Id = id
}

func (sdb *SimBatchDB) GenerateTables() error {
	foo := `CREATE TABLE IF NOT EXISTS BATCH (
    id integer primary key autoincrement,
    name VARCHAR NOT NULL UNIQUE,
	customer VARCHAR NOT NULL,
	profileType VARCHAR NOT NULL,
	orderDate VARCHAR NOT NULL,
	batchNo VARCHAR NOT NULL,
	quantity INTEGER NOT NULL,
	firstIccid VARCHAR,
	firstImsi VARCHAR,
	firstMsisdn VARCHAR,
	msisdnIncrement INTEGER,
	imsiIncrement INTEGER,
	iccidIncrement INTEGER,
	iccidIncrement INTEGER,
	firstMsisdn VARCHAR,
	url VARCHAR
	)`

	_, err := sdb.Db.Exec(foo)
	return err
}
