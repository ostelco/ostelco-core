//usr/bin/env go run "$0" "$@"; exit "$?"
package main

import (
	"crypto/tls"
	"crypto/x509"
	"flag"
	"fmt"
	"io"
	"encoding/json"
	"log"
)

//
//   Our new ES2+ library
//

type ES2PlusHeader struct {
	FunctionRequesterIdentifier   string  `json:"functionRequesterIdentifier"`
	FunctionCallIdentifier        string  `json:"functionCallIdentifier"`
}

type ES2PlusGetProfileStatusRequest struct {
	Header    ES2PlusHeader     `json:"header"`
	IccidList []ES2PlusIccid    `json:"iccidList"`
}


type ES2PlusIccid struct {
	Iccid    string     `json:"iccid"`
}


func NewStatusRequest(iccid string, functionRequesterIdentifier string, functionCallIdentifier string) ES2PlusGetProfileStatusRequest {
	return ES2PlusGetProfileStatusRequest {
		Header: ES2PlusHeader{ FunctionCallIdentifier: functionCallIdentifier, FunctionRequesterIdentifier:functionRequesterIdentifier },
		IccidList: [] ES2PlusIccid {ES2PlusIccid{Iccid: iccid}},
	}
}

func main() {


	certFilePath := flag.String("cert", "", "Certificate pem file.")
	keyFilePath := flag.String("key", "", "Certificate key file.")
	hostport := flag.String("hostport", "", "host:port of ES2+ endpoint.")
	requesterId := flag.String("requesterid", "", "ES2+ requester ID.")


	fmt.Println("certFilePath = ", *certFilePath)
	fmt.Println("keyFilePath  = ", *keyFilePath)
	fmt.Println("hostport     = ", *hostport)
	fmt.Println("requesterId  = ", *requesterId)

	flag.Parse()

	foo := NewStatusRequest("8947000000000000038", *requesterId, "banana")
	fooB, _  := json.Marshal(&foo)
	fmt.Println(string(fooB))


	// funcName(*certFilePath, *keyFilePath, *hostport)
}

func funcName(certFilePath string, keyFilePath string, hostport string) {
	cert, err := tls.LoadX509KeyPair(
		certFilePath,
		keyFilePath)
	if err != nil {
		log.Fatalf("server: loadkeys: %s", err)
	}
	config := tls.Config{Certificates: []tls.Certificate{cert}, InsecureSkipVerify: true}
	conn, err := tls.Dial("tcp", hostport, &config)
	if err != nil {
		log.Fatalf("client: dial: %s", err)
	}
	defer conn.Close()
	log.Println("client: connected to: ", conn.RemoteAddr())
	state := conn.ConnectionState()
	for _, v := range state.PeerCertificates {
		fmt.Println("Client: Server public key is:")
		fmt.Println(x509.MarshalPKIXPublicKey(v.PublicKey))
	}
	log.Println("client: handshake: ", state.HandshakeComplete)
	log.Println("client: mutual: ", state.NegotiatedProtocolIsMutual)
	message := "Hello\n"
	n, err := io.WriteString(conn, message)
	if err != nil {
		log.Fatalf("client: write: %s", err)
	}
	log.Printf("client: wrote %q (%d bytes)", message, n)
	reply := make([]byte, 256)
	n, err = conn.Read(reply)
	log.Printf("client: read %q (%d bytes)", string(reply[:n]), n)
	log.Print("client: exiting")
}
