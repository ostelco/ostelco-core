//usr/bin/env go run "$0" "$@"; exit "$?"
package main

import (
	"fmt"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/es2plus"
)

///
///   Main.  The rest should be put into a library.
///


import "gopkg.in/alecthomas/kingpin.v2"

var (
	debug    = kingpin.Flag("debug", "enable debug mode").Default("false").Bool()

	smoketest   = kingpin.Command("es2plusSmoketest", "Register a new user.")
	smoketestCertFilePath = smoketest.Arg("cert",  "Certificate pem file.").Required().String()
	smoketestKeyFilePath = smoketest.Arg("key", "Certificate key file.").Required().String()
	smoketestHostport = smoketest.Arg("hostport", "host:port of ES2+ endpoint.").Required().String()
	smoketestRequesterId = smoketest.Arg("requesterid",  "ES2+ requester ID.").Required().String()
	smoketestIccidInput = smoketest.Arg("iccid",  "Iccid of profile to manipulate").Required().String()

	// TODO: Check if this can be used for the key files.
	// postImage   = post.Flag("image", "image to post").ExistingFile()
)

func main() {
	switch kingpin.Parse() {

	// Handle the command
	case "smoketest":
		es2PlusSmoketest(smoketestCertFilePath, smoketestKeyFilePath, smoketestHostport, smoketestRequesterId, smoketestIccidInput)
	}
}

func es2PlusSmoketest(certFilePath *string, keyFilePath *string, hostport *string, requesterId *string, iccidInput *string) {

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

	/**
	// TODO:   Generate a full roundtrip taking some suitable profile through a proper
	//         activation, and reset.
	result, err := es2plus.Activate(client, iccid)
	if err != nil {
		panic(err)
	}

	result, err := es2plus.GetStatus(client, iccid)
	if err != nil {
		panic(err)
	}

	// Make some assertion about the status at this point
	result, err := es2plus.Reset(client, iccid)
	if err != nil {
		panic(err)
	}

	result, err := es2plus.GetStatus(client, iccid)
	if err != nil {
		panic(err)
	}

	// Make some assertion about the status at this point
*/

	fmt.Println("Success")
}
