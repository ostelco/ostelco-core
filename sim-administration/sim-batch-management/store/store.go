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
	"strings"
)


/// Holding database abstraction for the sim batch management system.
type SimBatchDB struct {
	Db *sqlx.DB
}

// An interface used to abstract the CRUD operations on the
// various entities in the sim batch management database.
type Store interface {
	GenerateTables() error
	DropTables() error

	CreateBatch(theBatch *model.Batch) error
	GetAllBatches(id string) ([]model.Batch, error)
	GetBatchByID(id int64) (*model.Batch, error)
	GetBatchByName(id string) (*model.Batch, error)

	CreateSimEntry(simEntry *model.SimEntry) error
	UpdateSimEntryMsisdn(simID int64, msisdn string)
	UpdateActivationCode(simID int64, activationCode string) error
	UpdateSimEntryKi(simID int64, ki string) error
	GetAllSimEntriesForBatch(batchID int64) ([]model.SimEntry, error)
	GetSimProfileByIccid(msisdn string) (*model.SimEntry, error)

	CreateProfileVendor(*model.ProfileVendor) error
	GetProfileVendorByID(id int64) (*model.ProfileVendor, error)
	GetProfileVendorByName(name string) (*model.ProfileVendor, error)

	Begin()
}

func (sdb *SimBatchDB) Begin() *sql.Tx {
	tx, err := sdb.Db.Begin()
	if err != nil {
		panic(err)
	}
	return tx
}


// Create a new in-memory instance of an SQLIte database
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

// Create a new  instance of an SQLIte database backed by a file whose path
// is found in the named environment variable. If the
// file doesn't exist, then it is created.  If the environment variable is not
// defined or empty, an error is returned.
func OpenFileSqliteDatabaseFromPathInEnvironmentVariable(variablename string) (*SimBatchDB, error) {
	variableValue := strings.TrimSpace(os.Getenv(variablename))
	if variableValue == "" {
		return nil, fmt.Errorf("environment variable '%s' is empty, is should contain the path to a sqlite database used by this program.  The file will be created if it's not there, but the path can't be empty", variablename)
	}
	db, err := OpenFileSqliteDatabase(variableValue)
	return db, err
}

// Create a new  instance of an SQLIte database backed by a file. If the
// file doesn't exist, then it is created.
func OpenFileSqliteDatabase(path string) (*SimBatchDB, error) {

	/*  TODO: Introduce 'debug' flag, and let that flag light up this code.
	if _, err := os.Stat(path); err == nil {
		log.Printf("Using database file  at '%s'", path)
	} else  {
		log.Printf("No databasefile found at '%s', will create one", path)
	}
	 */

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

func (sdb SimBatchDB) GetBatchByID(id int64) (*model.Batch, error) {
	result := []model.Batch{}
	if err := sdb.Db.Select(&result, "SELECT * FROM BATCH WHERE id = ?", id); err != nil {
		return nil, err
	} else if len(result) == 0 {
		fmt.Println("returning null")
		return nil, nil
	} else {
		return &(result[0]), nil
	}
}

func (sdb SimBatchDB) GetBatchByName(name string) (*model.Batch, error) {
	result := []model.Batch{}
	if err := sdb.Db.Select(&result, "select * from BATCH where name = ?", name); err != nil {
		return nil, err
	} else if len(result) == 0 {
		return nil, nil
	} else {
		return &(result[0]), nil
	}
}

func (sdb SimBatchDB) CreateBatch(theBatch *model.Batch) error {
	// TODO: mutex?

	res, err := sdb.Db.NamedExec("INSERT INTO BATCH (name, filenameBase, orderDate, customer, profileType, batchNo, quantity, profileVendor) values (:name, :filenameBase, :orderDate, :customer, :profileType, :batchNo, :quantity, :profileVendor)",
		theBatch,
	)

	if err != nil {
		return fmt.Errorf("failed to insert new batch '%s'", err)
	}

	id, err := res.LastInsertId()
	if err != nil {
		return fmt.Errorf("getting last inserted id failed '%s'", err)
	}
	theBatch.BatchID = id

	_, err = sdb.Db.NamedExec("UPDATE BATCH  SET firstIccid = :firstIccid, firstImsi = :firstImsi, firstMsisdn = :firstMsisdn, msisdnIncrement = :msisdnIncrement, iccidIncrement = :iccidIncrement, imsiIncrement = :imsiIncrement, url=:url WHERE id = :id",
		theBatch)

	return err
}

func (sdb *SimBatchDB) GenerateTables() error {
	sql := `CREATE TABLE IF NOT EXISTS BATCH (
     id integer primary key autoincrement,
	 name VARCHAR NOT NULL UNIQUE,
	 profileVendor VARCHAR NOT NULL,
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
	_, err := sdb.Db.Exec(sql)
	if err != nil {
		return err
	}

	sql = `CREATE TABLE IF NOT EXISTS SIM_PROFILE (
         id INTEGER PRIMARY KEY AUTOINCREMENT,
         batchID INTEGER NOT NULL,
         activationCode VARCHAR NOT NULL,
         imsi VARCHAR NOT NULL,
         rawIccid VARCHAR NOT NULL,
         iccidWithChecksum VARCHAR NOT NULL,
         iccidWithoutChecksum VARCHAR NOT NULL,
         iccid VARCHAR NOT NULL,
         ki VARCHAR NOT NULL,
         msisdn VARCHAR NOT NULL)`
	_, err = sdb.Db.Exec(sql)
	if err != nil {
		return err
	}

	sql = `CREATE TABLE IF NOT EXISTS PROFILE_VENDOR (
         id INTEGER PRIMARY KEY AUTOINCREMENT,
         name VARCHAR NOT NULL UNIQUE,
         es2PlusCertPath  VARCHAR,
         es2PlusKeyPath VARCHAR,
         es2PlusHostPath VARCHAR,
         es2PlusPort VARCHAR,
         es2PlusRequesterId VARCHAR)`
	_, err = sdb.Db.Exec(sql)

	return err
}

func (sdb SimBatchDB) CreateProfileVendor(theEntry *model.ProfileVendor) error {
	// TODO: This insert string can be made through reflection, and at some point should be.

	vendor, _ := sdb.GetProfileVendorByName(theEntry.Name)
	if vendor != nil {
		return fmt.Errorf("duplicate profile vendor named %s,  %v", theEntry.Name, vendor)
	}

	res, err := sdb.Db.NamedExec(`
       INSERT INTO PROFILE_VENDOR (name,   es2PlusCertPath,  es2PlusKeyPath,  es2PlusHostPath,  es2PlusPort, es2PlusRequesterId)
                           VALUES (:name, :es2PlusCertPath, :es2PlusKeyPath, :es2PlusHostPath, :es2PlusPort, :es2PlusRequesterId)`,
		theEntry)
	if err != nil {
		return err
	}

	id, err := res.LastInsertId()
	if err != nil {
		return fmt.Errorf("getting last inserted id failed '%s'", err)
	}
	theEntry.Id = id
	return nil
}

func (sdb SimBatchDB) GetProfileVendorByID(id int64) (*model.ProfileVendor, error) {
	result := []model.ProfileVendor{}
	if err := sdb.Db.Select(&result, "select * from PROFILE_VENDOR where id = ?", id); err != nil {
		return nil, err
	}

	if len(result) == 0 {
		return nil, nil
	} else {
		return &result[0], nil
	}
}



func (sdb SimBatchDB) GetProfileVendorByName(name string) (*model.ProfileVendor, error) {
	result := []model.ProfileVendor{}
	if err := sdb.Db.Select(&result, "select * from PROFILE_VENDOR where name = ?", name); err != nil {
		return nil, err
	}

	if len(result) == 0 {
		return nil, nil
	} else {
		return &result[0], nil
	}
}

func (sdb SimBatchDB) CreateSimEntry(theEntry *model.SimEntry) error {

	res := sdb.Db.MustExec("INSERT INTO SIM_PROFILE (batchID, activationCode, rawIccid, iccidWithChecksum, iccidWithoutChecksum, iccid, imsi, msisdn, ki) values (?,?,?,?,?,?,?,?,?)",
		theEntry.BatchID,
		theEntry.ActivationCode,
		theEntry.RawIccid,
		theEntry.IccidWithChecksum,
		theEntry.IccidWithoutChecksum,
		theEntry.Iccid,
		theEntry.Imsi,
		theEntry.Msisdn,
		theEntry.Ki,
	)

	id, err := res.LastInsertId()
	if err != nil {
		return fmt.Errorf("getting last inserted id failed '%s'", err)
	}
	theEntry.Id = id
	return err
}

func (sdb SimBatchDB) GetSimEntryById(simID int64) (*model.SimEntry, error) {
	result := []model.SimEntry{}
	if err := sdb.Db.Select(&result, "select * from SIM_PROFILE where id = ?", simID); err != nil {
		return nil, err
	}

	if len(result) == 0 {
		return nil, nil
	} else {
		return &result[0], nil
	}
}

func (sdb SimBatchDB) GetAllSimEntriesForBatch(batchID int64) ([]model.SimEntry, error) {
	result := []model.SimEntry{}
	if err := sdb.Db.Select(&result, "SELECT * from SIM_PROFILE WHERE batchID = ?", batchID) ; err != nil {
		return nil, err
	}

	if len(result) == 0 {
		return nil, nil
	} else {
		return result, nil
	}
}

// TODO: Add unit test for this method.


func (sdb SimBatchDB) GetSimProfileByIccid(iccid string) (*model.SimEntry, error) {
	result := []model.SimEntry{}
	if err := sdb.Db.Select(&result, "select * from SIM_PROFILE where iccid = ?", iccid); err != nil {
		return nil, err
	}

	if len(result) == 0 {
		return nil, nil
	} else {
		return &result[0], nil
	}
}

func (sdb SimBatchDB) GetSimProfileByImsi(imsi string) (*model.SimEntry, error) {
	result := []model.SimEntry{}
	if err := sdb.Db.Select(&result, "select * from SIM_PROFILE where imsi = ?", imsi); err != nil {
		return nil, err
	}

	if len(result) == 0 {
		return nil, nil
	} else {
		return &result[0], nil
	}
}


func (sdb SimBatchDB) UpdateSimEntryMsisdn(simID int64, msisdn string) error {
	_, err := sdb.Db.NamedExec("UPDATE SIM_PROFILE SET msisdn=:msisdn WHERE id = :simID",
		map[string]interface{}{
			"simID":  simID,
			"msisdn": msisdn,
		})
	return err
}

func (sdb SimBatchDB)  UpdateSimEntryKi(simID int64, ki string) error {
	_, err := sdb.Db.NamedExec("UPDATE SIM_PROFILE SET ki=:ki WHERE id = :simID",
		map[string]interface{}{
			"simID": simID,
			"ki":    ki,
		})
	return err
}


func (sdb SimBatchDB) UpdateActivationCode(simID int64, activationCode string) error {
	_, err := sdb.Db.NamedExec("UPDATE SIM_PROFILE SET activationCode=:activationCode WHERE id = :simID",
		map[string]interface{}{
			"simID":          simID,
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

		log.Println("Declaring batch ...")

	vendor, err := sdb.GetProfileVendorByName(profileVendor)
	if err != nil {
		return nil, err
	}
	if vendor == nil {
		return nil, fmt.Errorf("unknown profile vendor: '%s'", profileVendor)
	}

	// TODO:
	// 1. Check all the arguments (methods already written).
	// 2. Check that the name isn't already registred.
	// 3. If it isn't, then persist it

	//
	// Check parameters for syntactic correctness and
	// semantic sanity.
	//
	if addLuhn {
		firstIccid = fieldsyntaxchecks.AddLuhnChecksum(firstIccid)
		lastIccid = fieldsyntaxchecks.AddLuhnChecksum(lastIccid)
	}

	fieldsyntaxchecks.CheckICCIDSyntax("first-rawIccid", firstIccid)
	fieldsyntaxchecks.CheckICCIDSyntax("last-rawIccid", lastIccid)
	fieldsyntaxchecks.CheckIMSISyntax("last-imsi", lastIMSI)

	fieldsyntaxchecks.CheckIMSISyntax("first-imsi", firstIMSI)
	fieldsyntaxchecks.CheckMSISDNSyntax("last-msisdn", lastMsisdn)
	fieldsyntaxchecks.CheckMSISDNSyntax("first-msisdn", firstMsisdn)

	batchLength, err := strconv.Atoi(batchLengthString)
	if err != nil {
		return nil, fmt.Errorf("not a valid batch Quantity string '%s'", batchLengthString)
	}

	if batchLength <= 0 {
		return nil, fmt.Errorf("OutputBatch Quantity must be positive, but was '%d'", batchLength)
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
		return nil, fmt.Errorf("unknown parameters:  %s", flag.Args())
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
		ProfileVendor:   profileVendor,
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
	if err = sdb.CreateBatch(&batch); err != nil {
		return nil, err
	}

	imsi, err := strconv.Atoi(batch.FirstImsi)
	if err != nil {
		return nil, err
	}

	// Now create all the sim profiles

	iccidWithoutLuhnChecksum := firstIccidInt

	// XXX !!! TODO THis is wrong, but I'm doing it now, just to get started!
	var msisdn, err2 = strconv.Atoi(batch.FirstMsisdn)
	if err2 != nil {
		return nil, err
	}

	for i := 0; i < batch.Quantity; i++ {

		iccidWithLuhnChecksum := fmt.Sprintf("%d%d", iccidWithoutLuhnChecksum, fieldsyntaxchecks.LuhnChecksum(iccidWithoutLuhnChecksum))

		simEntry := &model.SimEntry{
			BatchID:              batch.BatchID,
			ActivationCode:       "",
			RawIccid:             fmt.Sprintf("%d", iccidWithoutLuhnChecksum),
			IccidWithChecksum:    iccidWithLuhnChecksum,
			IccidWithoutChecksum: fmt.Sprintf("%d", iccidWithoutLuhnChecksum),
			Iccid:                iccidWithLuhnChecksum,
			Imsi:                 fmt.Sprintf("%d", imsi),
			Msisdn:               fmt.Sprintf("%d", msisdn),
			Ki:                   "", // Should be null
		}

		if err = sdb.CreateSimEntry(simEntry); err != nil {
			return nil, err
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
