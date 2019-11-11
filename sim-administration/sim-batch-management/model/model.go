package model


// TODO: This  type SimEntry struct { struct isn't fully baked.

type SimEntry struct {
	Id                   int64  `db:"id" json:"id"`
	BatchID              int64  `db:"batchId" json:"batchId"`
	RawIccid             string `db:"rawIccid" json:"rawIccid"`
	IccidWithChecksum    string `db:"iccidWithChecksum" json:"iccidWithChecksum"`
	IccidWithoutChecksum string `db:"iccidWithoutChecksum" json:"iccidWithoutChecksum"`
	Iccid                string `db:"iccid" json:"iccid"`
	Imsi                 string `db:"imsi" json:"imsi"`
	Msisdn               string `db:"msisdn" json:"msisdn"`
	Ki                   string `db:"ki" json:"ki"`
	ActivationCode       string `db:"activationCode" json:"activationCode"`
}


type Batch struct {
	BatchId int64  `db:"id" json:"id"`
	Name    string `db:"name" json:"name"`

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
