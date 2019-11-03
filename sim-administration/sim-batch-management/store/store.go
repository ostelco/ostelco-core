package store

import (
	"flag"
	"fmt"
	"github.com/jmoiron/sqlx"
	_ "github.com/mattn/go-sqlite3"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/fieldsyntaxchecks"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/loltelutils"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"

	"log"
	"os"
	"strconv"
)

type Store interface {
	GenerateTables() error

	Create(theBatch *model.Batch) error
	GetAllInputBatches(id string) ([]model.Batch, error)
	GetInputBatchById(id int64) (*model.Batch, error)
	GetInputBatchByName(id string) (*model.Batch, error)

	DeclareBatch(
		db *SimBatchDB,
		firstIccid string,
		lastIccid string,
		firstIMSI string,
		lastIMSI string,
		firstMsisdn string,
		lastMsisdn string,
		profileType string,
		batchLengthString string,
		hssVendor string,
		uploadHostname string,
		uploadPortnumber string,
		profileVendor string,
		initialHlrActivationStatusOfProfiles string) (*model.Batch, error)
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

func (sdb SimBatchDB) Create(theBatch *model.Batch) error {

	res := sdb.Db.MustExec("INSERT INTO BATCH (name, orderDate,  customer, profileType, orderDate, batchNo, quantity, firstIccid, firstImsi,  firstMsisdn, msisdnIncrement, iccidIncrement, imsiIncrement, url) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ",
		(*theBatch).Name,
		(*theBatch).OrderDate,
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
		(*theBatch).Url,
	)

	id, err := res.LastInsertId()
	if err != nil {
		fmt.Errorf("Getting last inserted id failed '%s'", err)
	}
	theBatch.Id = id
	return err
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
	url VARCHAR
	)`
	_, err := sdb.Db.Exec(foo)
	return err
}

/**
 * Create a new batch, assuming that it doesn't exist.  Do all kind of checking of fields etc.
 */
func (sdb SimBatchDB) DeclareBatch(
	orderDate string,
	firstIccid string,
	lastIccid string,
	firstIMSI string,
	lastIMSI string,
	firstMsisdn string,
	lastMsisdn string,
	profileType string,
	batchLengthString string,
	hssVendor string,
	uploadHostname string,
	uploadPortnumber string,
	profileVendor string,
	initialHlrActivationStatusOfProfiles string) (*model.Batch, error) {

	// TODO:
	// 1. Check all the arguments (methods already written).
	// 2. Check that the name isn't already registred.
	// 3. If it isn't, then persist it

	//
	// Check parameters for syntactic correctness and
	// semantic sanity.
	//

	fieldsyntaxchecks.CheckICCIDSyntax("first-rawIccid", firstIccid)
	fieldsyntaxchecks.CheckICCIDSyntax("last-rawIccid", lastIccid)
	fieldsyntaxchecks.CheckIMSISyntax("last-imsi", lastIMSI)

	fieldsyntaxchecks.CheckIMSISyntax("first-imsi", firstIMSI)
	fieldsyntaxchecks.CheckMSISDNSyntax("last-msisdn", lastMsisdn)
	fieldsyntaxchecks.CheckMSISDNSyntax("first-msisdn", firstMsisdn)

	batchLength, err := strconv.Atoi(batchLengthString)
	if err != nil {
		log.Fatalf("Not a valid batch Quantity string '%s'.\n", batchLengthString)
	}

	if batchLength <= 0 {
		log.Fatalf("OutputBatch Quantity must be positive, but was '%d'", batchLength)
	}

	uploadUrl := fmt.Sprintf("http://%s:%s/ostelco/sim-inventory/%s/import-batch/profilevendor/%s?initialHssState=%s",
		uploadHostname, uploadPortnumber, hssVendor, profileVendor, initialHlrActivationStatusOfProfiles)

	fieldsyntaxchecks.CheckURLSyntax("uploadUrl", uploadUrl)
	fieldsyntaxchecks.CheckProfileType("profile-type", profileType)

	// Convert to integers, and get lengths
	msisdnIncrement := -1
	if firstMsisdn <= lastMsisdn {
		msisdnIncrement = 1
	}

	var firstMsisdnInt, _ = strconv.Atoi(firstMsisdn)
	var lastMsisdnInt, _ = strconv.Atoi(lastMsisdn)
	var msisdnLen = lastMsisdnInt - firstMsisdnInt + 1
	if msisdnLen < 0 {
		msisdnLen = -msisdnLen
	}

	var firstImsiInt, _ = strconv.Atoi(firstIMSI)
	var lastImsiInt, _ = strconv.Atoi(lastIMSI)
	var imsiLen = lastImsiInt - firstImsiInt + 1

	var firstIccidInt, _ = strconv.Atoi(fieldsyntaxchecks.IccidWithoutLuhnChecksum(firstIccid))
	var lastIccidInt, _ = strconv.Atoi(fieldsyntaxchecks.IccidWithoutLuhnChecksum(lastIccid))
	var iccidlen = lastIccidInt - firstIccidInt + 1

	// Validate that lengths of sequences are equal in absolute
	// values.
	// TODO: Perhaps use some varargs trick of some sort here?
	if loltelutils.Abs(msisdnLen) != loltelutils.Abs(iccidlen) || loltelutils.Abs(msisdnLen) != loltelutils.Abs(imsiLen) || batchLength != loltelutils.Abs(imsiLen) {
		log.Printf("msisdnLen   = %10d\n", msisdnLen)
		log.Printf("iccidLen    = %10d\n", iccidlen)
		log.Printf("imsiLen     = %10d\n", imsiLen)
		log.Fatal("FATAL: msisdnLen, iccidLen and imsiLen are not identical.")
	}

	tail := flag.Args()
	if len(tail) != 0 {
		log.Printf("Unknown parameters:  %s", flag.Args())
	}

	// Return a correctly parsed batch
	// TODO: Batch name missing!
	myBatch := model.Batch{
		OrderDate:       orderDate,
		Name:            "TODO fixme",
		ProfileType:     profileType,
		Url:             uploadUrl,
		Quantity:        loltelutils.Abs(iccidlen),
		FirstIccid:      firstIccid,
		IccidIncrement:  loltelutils.Sign(iccidlen),
		FirstImsi:       firstIMSI,
		ImsiIncrement:   loltelutils.Sign(imsiLen),
		FirstMsisdn:     firstMsisdn,
		MsisdnIncrement: msisdnIncrement,
	}

	// Create the new batch (checking for consistency not done!)
	err = sdb.Create(&myBatch)
	return &myBatch, err
}
