package es2plus

import (
	"bytes"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"strings"
)

///
///  Generic headers for invocations and responses
///

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


type ES2PlusResponseHeader struct {
	FunctionExecutionStatus FunctionExecutionStatus `json:"functionExecutionStatus"`
}

//
//  Status code invocation.
//

type ES2PlusStatusCodeData struct {
	SubjectCode       string `json:"subjectCode"`
	ReasonCode        string `json:"reasonCode"`
	SubjectIdentifier string `json:"subjectIdentifier"`
	Message           string `json:"message"`
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
//  Generating new ES2Plus clients
//


type Es2PlusClient struct {
	httpClient *http.Client
	hostport string
	requesterId string
}

func Client (certFilePath string, keyFilePath string, hostport string, requesterId string) (*Es2PlusClient) {
    return &Es2PlusClient {
        httpClient: newHttpClient(certFilePath, keyFilePath),
        hostport: hostport,
        requesterId: requesterId,
    }
}

func newHttpClient(certFilePath string, keyFilePath string) *http.Client {
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


///
/// Generic protocol code
///



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


func marshalUnmarshalGeneriEs2plusCommand(client *Es2PlusClient, es2plusCommand string,  payload interface{}, result interface{}) error {

	jsonStrB, err := json.Marshal(payload)
	if err != nil {
		return  err
	}

	responseBytes, err := executeGenericEs2plusCommand(jsonStrB, client.hostport, es2plusCommand,  client.httpClient)
	if err != nil {
		return  err
	}

	err = json.Unmarshal(responseBytes, result)
	return err
}


func executeGenericEs2plusCommand(jsonStrB []byte, hostport string, es2plusCommand string, httpClient *http.Client) ([]byte, error) {
	fmt.Println(string(jsonStrB))
	url := fmt.Sprintf("https://%s/gsma/rsp2/es2plus/%s", hostport, es2plusCommand)
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonStrB))
	req.Header.Set("X-Admin-Protocol", "gsma/rsp/v2.0.0")
	req.Header.Set("Content-Type", "application/json")
	resp, err := httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	// TODO Should check response headers here!
	// (in particular X-admin-protocol)
	response, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}
	responseBytes := []byte(response)
	return responseBytes, nil
}


///
///  Externally visible API for Es2Plus protocol
///

func newEs2PlusStatusRequest(iccid string, functionRequesterIdentifier string, functionCallIdentifier string) ES2PlusGetProfileStatusRequest {
	return ES2PlusGetProfileStatusRequest{
		Header:    ES2PlusHeader{FunctionCallIdentifier: functionCallIdentifier, FunctionRequesterIdentifier: functionRequesterIdentifier},
		IccidList: [] ES2PlusIccid{ES2PlusIccid{Iccid: iccid}},
	}
}

func GetStatus(client *Es2PlusClient, iccid string) (*ES2ProfileStatusResponse, error) {
    var result = new(ES2ProfileStatusResponse)
    es2plusCommand := "getProfileStatus"
    payload := newEs2PlusStatusRequest(iccid, client.requesterId, "oo") // TODO: Add a proper function call identifier
    err := marshalUnmarshalGeneriEs2plusCommand(client, es2plusCommand,  &payload, result)
    return result, err
}


