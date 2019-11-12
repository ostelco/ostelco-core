package store

import (
	"database/sql"
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
	DropTables() error

	Create(theBatch *model.Batch) error
	GetAllBatches(id string) ([]model.Batch, error)
	GetBatchById(id int64) (*model.Batch, error)
	GetBatchByName(id string) (*model.Batch, error)

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

	CreateSimEntry(simEntry *model.SimEntry) error
	UpdateSimEntryMsisdn(simId int64, msisdn string)
	UpdateActivationCode(simId int64, activationCode string) error
	GetAllSimEntriesForBatch(batchId int64) ([]model.SimEntry, error)

	Begin()
}

func (sdb *SimBatchDB) Begin() *sql.Tx {
	tx, err := sdb.Db.Begin()
	if err != nil {
		panic(err)
	}
	return tx
}

type SimBatchDB struct {
	Db *sqlx.DB
}

func NewInMemoryDatabase() (*SimBatchDB, error) {
	db, err := sqlx.Connect("sqlite3", ":memory:")
	if err != nil {
		return nil, err
	}

	err = db.Ping()
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
	db, err := sqlx.Open("sqlite3", path)
	if err != nil {
		return nil, err
	}
	return &SimBatchDB{Db: db}, nil
}

func (sdb SimBatchDB) GetAllBatches() ([]model.Batch, error) {
	result := []model.Batch{}
	return result, sdb.Db.Select(&result, "SELECT * from BATCH")
}

func (sdb SimBatchDB) GetBatchById(id int64) (*model.Batch, error) {
	var result model.Batch
	return &result, sdb.Db.Get(&result, "select * from BATCH where id = ?", id)
}

func (sdb SimBatchDB) GetBatchByName(name string) (*model.Batch, error) {
	var result model.Batch
	return &result, sdb.Db.Get(&result, "select * from BATCH where name = ?", name)
}

func (sdb SimBatchDB) CreateBatch(theBatch *model.Batch) error {
	// TODO: mutex
	/*
	   	res, err  := sdb.Db.Exec("INSERT INTO BATCH (name, filenameBase, orderDate, customer, profileType, batchNo, quantity, firstIccid,  firstImsi,  firstMsisdn, msisdnIncrement, iccidIncrement, imsiIncrement, url) values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)",
	   		// "INSERT INTO BATCH (name, filenameBase, orderDate,  customer, profileType, batchNo, quantity, firstIccid, firstImsi,  firstMsisdn, msisdnIncrement, iccidIncrement, imsiIncrement, url) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ",
	   theBatch.Name ,theBatch.FilenameBase,theBatch.OrderDate, theBatch.Customer,theBatch.ProfileType,
	   		theBatch.BatchNo,
	   		theBatch.Quantity,
	   		theBatch.FirstIccid,
	   		theBatch.FirstImsi,
	   		theBatch.FirstMsisdn,
	   		theBatch.MsisdnIncrement,
	   		theBatch.IccidIncrement,
	   		theBatch.ImsiIncrement,
	   		theBatch.Url,
	   	)
	*/

	// TODO:  a) Report it as a real error (minimal reproducable)
	//        b) Insert the object, then add extra fields, do it in a transaction, and don't break
	/** foo := `CREATE TABLE IF NOT EXISTS BATCH (
	       id integer primary key autoincrement,
	  	 name VARCHAR NOT NULL UNIQUE,
	  	 filenameBase VARCHAR NOT NULL,
	  	 customer VARCHAR NOT NULL,
	  	 profileType VARCHAR NOT NULL,
	  	 orderDate VARCHAR NOT NULL,
	  	 batchNo VARCHAR NOT NULL,
	  	 quantity INTEGER NOT NULL,

	*/
	/*
		res, err := sdb.Db.NamedExec("INSERT INTO BATCH (name, filenameBase, orderDate, customer, profileType, batchNo, quantity) values (:name, :filenameBase, :orderDate, :customer, :profileType, :batchNo, :quantity)",
			theBatch,
		)
	*/

	res, err := sdb.Db.NamedExec("INSERT INTO BATCH (name, filenameBase, orderDate, customer, profileType, batchNo, quantity) values (:name, :filenameBase, :orderDate, :customer, :profileType, :batchNo, :quantity)",
		theBatch,
	)

	if err != nil {
		// XXX Should be error logging
		fmt.Printf("Failed to insert new batch '%s'", err)
		return err
	}

	id, err := res.LastInsertId()
	if err != nil {
		// XXX Should be error logging
		fmt.Printf("Getting last inserted id failed '%s'", err)
		return err
	}
	theBatch.BatchId = id

	// "UPDATE SIM_PROFILE SET msisdn=:msisdn WHERE id = :simId",

	res, err = sdb.Db.NamedExec("UPDATE BATCH  SET firstIccid = :firstIccid, firstImsi = :firstImsi, firstMsisdn = :firstMsisdn, msisdnIncrement = :msisdnIncrement, iccidIncrement = :iccidIncrement, imsiIncrement = :imsiIncrement, url=:url WHERE id = :id",
		theBatch)
	// , :firstIccid,  :firstImsi,  :firstMsisdn, :msisdnIncrement, :iccidIncrement, :imsiIncrement, :url

	return err
}

func (sdb *SimBatchDB) GenerateTables() error {
	foo := `CREATE TABLE IF NOT EXISTS BATCH (
     id integer primary key autoincrement,
	 name VARCHAR NOT NULL UNIQUE,
	 filenameBase VARCHAR,
	 customer VARCHAR,
	 profileType VARCHAR,
	 orderDate VARCHAR,
	 batchNo VARCHAR,
	 quantity INTEGER,
	 firstIccid VARCHAR,
	 firstImsi VARCHAR,
	 firstMsisdn VARCHAR,
	 msisdnIncrement INTEGER,
	 imsiIncrement INTEGER,
	 iccidIncrement INTEGER,
	 url VARCHAR)`
	_, err := sdb.Db.Exec(foo)
	if err != nil {
		return err
	}

	foo = `CREATE TABLE IF NOT EXISTS SIM_PROFILE (
         id INTEGER PRIMARY KEY AUTOINCREMENT,
         batchId INTEGER NOT NULL,
         activationCode VARCHAR NOT NULL,
         imsi VARCHAR NOT NULL,
         rawIccid VARCHAR NOT NULL,
         iccidWithChecksum VARCHAR NOT NULL,
         iccidWithoutChecksum VARCHAR NOT NULL,
         iccid VARCHAR NOT NULL,
         ki VARCHAR NOT NULL,
         msisdn VARCHAR NOT NULL)`
	_, err = sdb.Db.Exec(foo)
	return err
}

func (sdb SimBatchDB) CreateSimEntry(theEntry *model.SimEntry) error {

	res := sdb.Db.MustExec("INSERT INTO SIM_PROFILE (batchId, activationCode, rawIccid, iccidWithChecksum, iccidWithoutChecksum, iccid, imsi, msisdn, ki) values (?,?,?,?,?,?,?,?,?)",
		(*theEntry).BatchID, // XXX Fix this!
		(*theEntry).ActivationCode,
		(*theEntry).RawIccid,
		(*theEntry).IccidWithChecksum,
		(*theEntry).IccidWithoutChecksum,
		(*theEntry).Iccid,
		(*theEntry).Imsi,
		(*theEntry).Msisdn,
		(*theEntry).Ki,
	)

	id, err := res.LastInsertId()
	if err != nil {
		fmt.Errorf("Getting last inserted id failed '%s'", err)
	}
	theEntry.Id = id
	return err
}

func (sdb SimBatchDB) GetSimEntryById(simId int64) (*model.SimEntry, error) {
	var result model.SimEntry
	return &result, sdb.Db.Get(&result, "select * from SIM_PROFILE where id = ?", simId)
}

func (sdb SimBatchDB) GetAllSimEntriesForBatch(batchId int64) ([]model.SimEntry, error) {
	result := []model.SimEntry{}
	return result, sdb.Db.Select(&result, "SELECT * from SIM_PROFILE WHERE batchId = ?", batchId)
}

func (sdb SimBatchDB) UpdateSimEntryMsisdn(simId int64, msisdn string) error {
	_, err := sdb.Db.NamedExec("UPDATE SIM_PROFILE SET msisdn=:msisdn WHERE id = :simId",
		map[string]interface{}{
			"simId":  simId,
			"msisdn": msisdn,
		})
	return err

}

func (sdb SimBatchDB) UpdateActivationCode(simId int64, activationCode string) error {
	_, err := sdb.Db.NamedExec("UPDATE SIM_PROFILE SET activationCode=:activationCode WHERE id = :simId",
		map[string]interface{}{
			"simId":          simId,
			"activationCode": activationCode,
		})
	return err
}

func (sdb *SimBatchDB) DropTables() error {
	foo := `DROP  TABLE BATCH`
	_, err := sdb.Db.Exec(foo)
	if err != nil {
		return err
	}
	foo = `DROP  TABLE SIM_PROFILE`
	_, err = sdb.Db.Exec(foo)
	return err
}

/**
 * DeclareBatch a new batch, assuming that it doesn't exist.  Do all kind of checking of fields etc.
 */
func (sdb SimBatchDB) DeclareBatch(
	name string,
	addLuhn bool,
	customer string,
	batchNo string,
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

	fmt.Printf("Pre  adding luhn ? %t.   first='%s', last='%s'\n", addLuhn, firstIccid, lastIccid)

	if addLuhn {
		firstIccid = fieldsyntaxchecks.AddLuhnChecksum(firstIccid)
		lastIccid = fieldsyntaxchecks.AddLuhnChecksum(lastIccid)
	}

	fmt.Printf("Post adding luhn ? %t.   first='%s',last='%s'\n", addLuhn, firstIccid, lastIccid)

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
		log.Printf("batchLength = %10d\n", batchLength)
		log.Fatal("FATAL: msisdnLen, iccidLen and imsiLen are not identical.")
	}

	tail := flag.Args()
	if len(tail) != 0 {
		log.Printf("Unknown parameters:  %s", flag.Args())
	}

	filenameBase := fmt.Sprintf("%s%s%s", customer, orderDate, batchNo)

	batch := model.Batch{
		OrderDate:       orderDate,
		Customer:        customer,
		FilenameBase:    filenameBase,
		Name:            name,
		BatchNo:         batchNo,
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

	tx := sdb.Begin()

	// This variable should be se to "true" if all transactions
	// were successful, otherwise it will be rolled back.
	weCool := false

	defer func() {
		if weCool {
			tx.Commit()
		} else {
			tx.Rollback()
		}
	}()

	// Persist the newly created batch,
	err = sdb.CreateBatch(&batch)
	if err != nil {
		panic(err)
	}

	imsi, err := strconv.Atoi(batch.FirstImsi)
	if err != nil {
		panic(err)
	}

	// Now create all the sim profiles

	iccidWithoutLuhnChecksum := firstIccidInt

	// XXX !!! TODO THis is wrong, but I'm doing it now, just to get started!
	var msisdn, err2 = strconv.Atoi(batch.FirstMsisdn)
	if err2 != nil {
		panic(err)
	}

	for i := 0; i < batch.Quantity; i++ {

		iccidWithLuhnChecksum := fmt.Sprintf("%d%d", iccidWithoutLuhnChecksum, fieldsyntaxchecks.LuhnChecksum(iccidWithoutLuhnChecksum))

		simEntry := &model.SimEntry{
			BatchID:              batch.BatchId,
			ActivationCode:       "",
			RawIccid:             fmt.Sprintf("%d", iccidWithoutLuhnChecksum),
			IccidWithChecksum:    iccidWithLuhnChecksum,
			IccidWithoutChecksum: fmt.Sprintf("%d", iccidWithoutLuhnChecksum),
			Iccid:                iccidWithLuhnChecksum,
			Imsi:                 fmt.Sprintf("%d", imsi),
			Msisdn:               fmt.Sprintf("%d", msisdn),
			Ki:                   "", // Should be null
		}

		err = sdb.CreateSimEntry(simEntry)
		if err != nil {
			panic(err)
		}

		iccidWithoutLuhnChecksum += batch.IccidIncrement
		imsi += batch.ImsiIncrement
		msisdn += batch.MsisdnIncrement
	}

	// Signal to deferred function that we're ready to commit.
	weCool = true

	//  Return the newly created batch
	return &batch, err
}
