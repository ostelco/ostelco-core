package model

// TODO: There are now multiple structs that model batches.
//       It's probably a good idea to harmonize these so that it's
//       only one type of batch info that's being read, and then have
//       various ways to combine the misc. sources of batch information
//       that lets partial information from multiple records be harmonized
//       in a common persisted record that is then used for the bulk of the
//       processing.

//
// Batches as read from the input files
//
type InputBatch struct {
	Id          int64  `db:"id" json:"id"`
	Name        string `db:"name" json:"name"`
	Customer    string `db:"customer" json:"customer"`
	ProfileType string `db:"profileType" json:"profileType"`
	OrderDate   string `db:"orderDate" json:"orderDate"`
	BatchNo     string `db:"batchNo" json:"batchNo"`
	Quantity    int    `db:"quantity" json:"quantity"`
	FirstIccid  string `db:"firstIccid" json:"firstIccid"`
	FirstImsi   string `db:"firstImsi" json:"firstImsi"`
}

type OutputBatch struct {
	ProfileType     string `db:"profileType" json:"profileType"`
	Url             string `db:"url" json:"url"`
	Length          int
	FirstMsisdn     int
	MsisdnIncrement int
	FirstIccid      int
	IccidIncrement  int
	FirstImsi       int
	ImsiIncrement   int
}

type OutputFileRecord struct {
	Filename          string
	InputVariables    map[string]string
	HeaderDescription map[string]string
	Entries           []SimEntry
	// TODO: As it is today, the noOfEntries is just the number of Entries,
	//       but I may want to change that to be the declared number of Entries,
	//       and then later, dynamically, read in the individual Entries
	//       in a channel that is just piped to the goroutine that writes
	//       them to file, and fails if the number of declared Entries
	//       differs from the actual number of Entries.  .... but that is
	//       for another day.
	NoOfEntries    int
	OutputFileName string
}

type SimEntry struct {
	RawIccid             string
	IccidWithChecksum    string
	IccidWithoutChecksum string
	Imsi                 string
	Ki                   string // XXX Toxic.  Should never be stored persistently!!
	OutputFileName       string
}

//
//  Below this line we grow the final persistence model. Eventually
//  nothing below this line should be left.
//

type Batch struct {
	Id          int64   `db:"id" json:"id"`
	Name        string  `db:"name" json:"name"`
	Customer    string  `db:"customer" json:"customer"`
	ProfileType string  `db:"profileType" json:"profileType"`
	OrderDate   string  `db:"orderDate" json:"orderDate"`
	BatchNo     string  `db:"batchNo" json:"batchNo"`
	Quantity    int     `db:"quantity" json:"quantity"`
	FirstIccid  string  `db:"firstIccid" json:"firstIccid"`
	FirstImsi   string  `db:"firstImsi" json:"firstImsi"`
	Url         string  `db:"url" json:"url"`
	MsisdnIncrement int `db:"msisdnIncrement" json:"msisdnIncrement"`
	IccidIncrement int `db:"msisdnIncrement" json:"msisdnIncrement"`
	ImsiIncrement int `db:"imsiIncrement" json:"imsiIncrement"`
	FirstMsisdn string  `db:"firstMsisdn" json:"firstMsisdn"`
}
