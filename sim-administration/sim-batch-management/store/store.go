package store

import (
	"fmt"
	"github.com/jmoiron/sqlx"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"
)

type Store interface {
	GenerateTables() error

	Create(theBatch *model.InputBatch) error
	GetAllInputBatches(id string) ([]model.InputBatch, error)
	GetInputBatchById(id int64) (*model.InputBatch, error)
	GetInputBatchByName(id string) (*model.InputBatch, error)
}

type SimBatchDB struct {
	Db *sqlx.DB
}

func NewInMemoryDatabase() *SimBatchDB {
	db, err := sqlx.Open("sqlite3", ":memory:")
	if err != nil {
		fmt.Errorf("Didn't manage to open sqlite3 in-memory database. '%s'", err)
	}
	return &SimBatchDB{Db: db}
}

func NewFileSqliteDatabase(path string) *SimBatchDB {
	db, err := sqlx.Open("sqlite3", "foobar.db")
	if err != nil {
		fmt.Errorf("Didn't manage to open sqlite3 file database. '%s'", err)
	}
	return &SimBatchDB{Db: db}
}

func (sdb SimBatchDB) GetAllInputBatches() ([]model.InputBatch, error) {
	result := []model.InputBatch{}
	return result, sdb.Db.Select(&result, "SELECT * from INPUT_BATCH")
}

func (sdb SimBatchDB) GetInputBatchById(id int64) (*model.InputBatch, error) {
	var result model.InputBatch
	return &result, sdb.Db.Get(&result, "select * from INPUT_BATCH where id = ?", id)
}

func (sdb SimBatchDB) GetInputBatchByName(name string) (*model.InputBatch, error) {
	var result model.InputBatch
	return &result, sdb.Db.Get(&result, "select * from INPUT_BATCH where name = ?", name)
}

func (sdb SimBatchDB) Create(theBatch *model.InputBatch) {

	res := sdb.Db.MustExec("INSERT INTO INPUT_BATCH (name, customer, profileType, orderDate, batchNo, quantity, firstIccid, firstImsi) values (?,?,?,?,?,?,?,?) ",
		(*theBatch).Name,
		(*theBatch).Customer,
		(*theBatch).ProfileType,
		(*theBatch).OrderDate,
		(*theBatch).BatchNo,
		(*theBatch).Quantity,
		(*theBatch).FirstIccid,
		(*theBatch).FirstImsi,
	)

	id, err := res.LastInsertId()
	if err != nil {
		fmt.Errorf("Getting last inserted id failed '%s'", err)
	}
	theBatch.Id = id
}

func (sdb *SimBatchDB) GenerateTables() error {
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

	_, err := sdb.Db.Exec(foo)
	return err
}
