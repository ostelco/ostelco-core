package model

// TODO: Put all records used to manage workflows in this
//       package, then build a DAO interface a "store" package

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
	Ki                   string
	OutputFileName       string
}
