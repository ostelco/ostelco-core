//usr/bin/env go run "$0" "$@"; exit "$?"
package main

import (
	"flag"
	"fmt"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/es2plus"
)

//
// This is a "smoketest", a program that is formulated somewhat as a test,
// and I've used to develop the es2plus library.  It is not formulated as a
// unit test, since it requires access to an external resource and I don't feel it
// is appropriate to run unit tests against that external resource. Some degree of
// deliberation seems appropriate.
//
// The program illustrates the API being used, and logs progress in the state of the
// profile being manipulated.
//
func main() {

	certFilePath := flag.String("cert", "", "Certificate pem file.")
	keyFilePath := flag.String("key", "", "Certificate key file.")
	hostport := flag.String("hostport", "", "host:port of ES2+ endpoint.")
	requesterId := flag.String("requesterid", "", "ES2+ requester ID.")
	iccidInput := flag.String("iccid", "", "Iccid of profile to manipulate")


	flag.Parse()

	fmt.Printf("certFilePath = '%s'\n", *certFilePath)
	fmt.Printf("keyFilePath  = '%s'\n", *keyFilePath)
	fmt.Printf("hostport     = '%s'\n", *hostport)
	fmt.Printf("requesterId  = '%s'\n", *requesterId)
	fmt.Printf("iccidInput   = '%s'\n", *iccidInput)

	iccid := *iccidInput

	client := es2plus.Client(*certFilePath, *keyFilePath, *hostport, *requesterId)

	result, err := es2plus.GetStatus(client, iccid)
	if err != nil {
		panic(err)
	}
	fmt.Println("result1 -> ", result.State)

	result2, err := es2plus.RecoverProfile(client, iccid, "AVAILABLE")
	if err != nil {
		panic(err)
	}

	fmt.Println("result2 -> ", result2)

	result, err = es2plus.GetStatus(client, iccid)
	if err != nil {
		panic(err)
	}

	fmt.Println("result3 -> ", result.State)


	result4, err := es2plus.RecoverProfile(client, iccid, "AVAILABLE")
	if err != nil {
		panic(err)
	}

	fmt.Println("result4 -> ", result4)

	result, err = es2plus.GetStatus(client, iccid)
	if err != nil {
		panic(err)
	}

	fmt.Println("result5 -> ", result.State)

	if result.State != "AVAILABLE" {
		panic("Couldn't convert state of iccid into AVAILABLE")
	}


	result6, err := es2plus.DownloadOrder(client, iccid)
	fmt.Println("result6 -> ", result6)
	if err != nil {
		panic(err)
	}


	result7, err := es2plus.GetStatus(client, iccid)
	if err != nil {
		panic(err)
	}
	fmt.Println("result7 -> ", result7)


	result8, err := es2plus.ConfirmOrder(client, iccid)
	fmt.Println("result8 -> ", result8)
	if err != nil {
		panic(err)
	}


	result9, err := es2plus.GetStatus(client, iccid)
	if err != nil {
		panic(err)
	}
	fmt.Println("result9 -> ", result9)


	if result9.State != "RELEASED" {
		panic("Couldn't convert state of iccid into RELEASED")
	}


	fmt.Println("Success")
}
