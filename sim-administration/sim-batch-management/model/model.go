package model

// TODO: There are now multiple structs that model batches.
//       It's probably a good idea to harmonize these so that it's
//       only one type of batch info that's being read, and then have
//       various ways to combine the misc. sources of batch information
//       that lets partial information from multiple records be harmonized
//       in a common persisted record that is then used for the bulk of the
//       processing.



// TODO: This  struct isn't fully baked.

type SimEntry struct {
	SimId       int64  `db:"simId" json:"simId"`
	BatchID     int64  `db:"batchId" json:"batchId"`
	RawIccid    string  `db:"rawIccid" json:"rawIccid"`
	IccidWithChecksum    string  `db:"iccidWithChecksum" json:"iccidWithChecksum"`
	IccidWithoutChecksum string  `db:"iccidWithoutChecksum" json:"iccidWithoutChecksum"`
	Iccid       string  `db:"iccid" json:"iccid"`
	Imsi        string  `db:"imsi" json:"imsi"`
	Msisdn      string  `db:"msisdn" json:"msisdn"`
	Ki          string  `db:"ki" json:"ki"`
	ActivationCode          string  `db:"activationCode" json:"activationCode"`
}


//
//  Below this line we grow the final persistence model. Eventually
//  nothing below this line should be left.
//


// TODO:   Add a filename base which is e.g. Footel201910110102, functional
//         dependencies on other fields, but we'll not worry about that right
//         now.

type Batch struct {
	Id              int64  `db:"id" json:"id"`
	Name            string `db:"name" json:"name"`

	// TODO: Customer is a misnomer: This is the customer name used when
	//       ordering a sim batch, used in the input file.  So a very
	//       specific use, not in any way the generic thing the word
	//       as it is used now points to.

	FilenameBase    string `db:"filenameBase" json:"filenameBase"`
	Customer        string `db:"customer" json:"customer"`
	ProfileType     string `db:"profileType" json:"profileType"`
	OrderDate       string `db:"orderDate" json:"orderDate"`
	BatchNo         string `db:"batchNo" json:"batchNo"`
	Quantity        int    `db:"quantity" json:"quantity"`
	FirstIccid      string `db:"firstIccid" json:"firstIccid"`
	FirstImsi       string `db:"firstImsi" json:"firstImsi"`
	Url             string `db:"url" json:"url"`
	MsisdnIncrement int    `db:"msisdnIncrement" json:"msisdnIncrement"`
	IccidIncrement  int    `db:"iccidIncrement" json:"msisdnIncrement"`
	ImsiIncrement   int    `db:"imsiIncrement" json:"imsiIncrement"`
	FirstMsisdn     string `db:"firstMsisdn" json:"firstMsisdn"`
}
