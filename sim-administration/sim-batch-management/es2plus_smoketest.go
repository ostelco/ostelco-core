//usr/bin/env go run "$0" "$@"; exit "$?"
package main

import (
	"encoding/json"
	"bytes"
	"flag"
	"fmt"
	"net/http"
	"log"
	"io/ioutil"
	"strings"
	"crypto/tls"
)

//
//   Our new ES2+ library
//

type ES2PlusHeader struct {
	FunctionRequesterIdentifier string `json:"functionRequesterIdentifier"`
	FunctionCallIdentifier      string `json:"functionCallIdentifier"`
}

type ES2PlusGetProfileStatusRequest struct {
	Header    ES2PlusHeader  `json:"header"`
	IccidList []ES2PlusIccid `json:"iccidList"`
}

type ES2PlusIccid struct {
	Iccid string `json:"iccid"`
}

type FunctionExecutionStatus struct {
	FunctionExecutionStatusType string                `json:"status"` // Should be an enumeration type
	StatusCodeData              ES2PlusStatusCodeData `json:"statusCodeData"`
}

type ES2PlusStatusCodeData struct {
	SubjectCode       string `json:"subjectCode"`
	ReasonCode        string `json:"reasonCode"`
	SubjectIdentifier string `json:"subjectIdentifier"`
	Message           string `json:"message"`
}

type ES2PlusResponseHeader struct {
	FunctionExecutionStatus string `json:"functionExecutionStatus"`
}

type ES2ProfileStatusResponse struct {
	Header              ES2PlusResponseHeader `json:"header"`
	ProfileStatusList   []ProfileStatus       `json:"profileStatusList"`
	CompletionTimestamp string                `json:"completionTimestamp"`
}

type ProfileStatus struct {
	StatusLastUpdateTimestamp string `json:"status_last_update_timestamp"`
	ACToken                   string `json:"acToken"`
	State                     string `json:"state"`
	Eid                       string `json:"eid"`
	Iccid                     string `json:"iccid"`
	LockFlag                  bool   `json:"lockFlag"`
}

//
//  Protocol code
//

func NewStatusRequest(iccid string, functionRequesterIdentifier string, functionCallIdentifier string) ES2PlusGetProfileStatusRequest {
	return ES2PlusGetProfileStatusRequest{
		Header:    ES2PlusHeader{FunctionCallIdentifier: functionCallIdentifier, FunctionRequesterIdentifier: functionRequesterIdentifier},
		IccidList: [] ES2PlusIccid{ES2PlusIccid{Iccid: iccid}},
	}
}

func main() {

	certFilePath := flag.String("cert", "", "Certificate pem file.")
	keyFilePath := flag.String("key", "", "Certificate key file.")
	hostport := flag.String("hostport", "", "host:port of ES2+ endpoint.")
	requesterId := flag.String("requesterid", "", "ES2+ requester ID.")

	fmt.Printf("certFilePath = '%s'\n", *certFilePath)
	fmt.Printf("keyFilePath  = '%s'\n", *keyFilePath)
	fmt.Printf("hostport     = '%s'\n", *hostport)
	fmt.Printf("requesterId  = '%s'\n", *requesterId)

	flag.Parse()

	info, _ := getProfileInfo(*certFilePath, *keyFilePath, *hostport, *requesterId, "8947000000000000038", "Applecart")
	fmt.Println("Info -> ", info)
}

// formatRequest generates ascii representation of a request
func formatRequest(r *http.Request) string {
	// Create return string
	var request []string
	// Add the request string
	url := fmt.Sprintf("%v %v %v", r.Method, r.URL, r.Proto)
	request = append(request, url)
	// Add the host
	request = append(request, fmt.Sprintf("Host: %v", r.Host))
	// Loop through headers
	for name, headers := range r.Header {
		name = strings.ToLower(name)
		for _, h := range headers {
			request = append(request, fmt.Sprintf("%v: %v", name, h))
		}
	}

	// If this is a POST, add post data
	if r.Method == "POST" {
		r.ParseForm()
		request = append(request, "\n")
		request = append(request, r.Form.Encode())
	}
	// Return the request as a string
	return strings.Join(request, "\n")
}






func getProfileInfo(certFilePath string, keyFilePath string, hostport string, requesterId string, iccid string, functionCallIdentifier string) (*ES2ProfileStatusResponse, error) {
	client := newClient(certFilePath, keyFilePath)

	// Generate a "hole in the wall" getProfileStatus request, to be generalized later.
	payload := NewStatusRequest(iccid, requesterId, functionCallIdentifier)
	jsonStrB, _ := json.Marshal(&payload)
	fmt.Println(string(jsonStrB))

	url := fmt.Sprintf("https://%s/gsma/rsp2/es2plus/getProfileStatus", hostport)

	// TODO: Consider also https://github.com/parnurzeal/gorequest
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonStrB))

	req.Header.Set("X-Admin-Protocol", "gsma/rsp/v2.0.0")
	req.Header.Set("Content-Type", "application/json")

	resp, err := client.Do(req)
	if err != nil {
		panic(err)
	}

	// TODO Should check response headers here!
	// (in particular X-admin-protocol)

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		panic(err.Error())
	}

	bodyB := []byte(body)
	var result = new(ES2ProfileStatusResponse)
	err = json.Unmarshal(bodyB, &result)
	if (err != nil) {
		fmt.Println("whoops:", err)
	}
	return result, err
}

func newClient(certFilePath string, keyFilePath string) *http.Client {
	cert, err := tls.LoadX509KeyPair(
		certFilePath,
		keyFilePath)
	if err != nil {
		log.Fatalf("server: loadkeys: %s", err)
	}
	config := tls.Config{Certificates: []tls.Certificate{cert}, InsecureSkipVerify: true}
	client := &http.Client{
		Transport: &http.Transport{
			TLSClientConfig: &config,
		},
	}
	return client
}
