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
	"github.com/google/uuid"
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
	printPayload bool
	printHeaders bool
}

func Client (certFilePath string, keyFilePath string, hostport string, requesterId string) (*Es2PlusClient) {
    return &Es2PlusClient {
        httpClient: newHttpClient(certFilePath, keyFilePath),
        hostport: hostport,
        requesterId: requesterId,
        printPayload: false,
        printHeaders: false,
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


//
// Function used during debugging to print requests before they are
// sent over the wire.  Very useful, should not be deleted even though it
// is not used right now.
//
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

func newUuid() (string, error) {
    uuid, err := uuid.NewRandom()
    if err != nil {
    		return "", err
    	}
    return uuid.URN(), nil
}

func newEs2plusHeader(client *Es2PlusClient) (*ES2PlusHeader) {
   functionCallIdentifier, err := newUuid()
   if err != nil  {
        panic(err)
   }
   return &ES2PlusHeader{FunctionCallIdentifier: functionCallIdentifier, FunctionRequesterIdentifier: client.requesterId}
}


func marshalUnmarshalGeneriEs2plusCommand(client *Es2PlusClient, es2plusCommand string,  payload interface{}, result interface{}) error {

    // Serialize payload as json.
	jsonStrB, err := json.Marshal(payload)
	if err != nil {
		return  err
	}

    if client.printPayload {
    	fmt.Print("Payload ->", string(jsonStrB))
    }

    // Get the result of the HTTP POST as a byte array
    // that can be deserialized into json.  Fail fast
    // an error has been detected.
	responseBytes, err := executeGenericEs2plusCommand(jsonStrB, client.hostport, es2plusCommand,  client.httpClient, client.printHeaders)
	if err != nil {
		return  err
	}

    // Return error code from deserialisation, result is put into
    // result via referenced object.
	return json.Unmarshal(responseBytes, result)
}


func executeGenericEs2plusCommand(jsonStrB []byte, hostport string, es2plusCommand string, httpClient *http.Client, printHeaders bool) ([]byte, error) {

    // Build and execute an ES2+ protocol request by using the serialised JSON in the
    // byte array jsonStrB in a POST request.   Set up the required
    // headers for ES2+ and content type.
	url := fmt.Sprintf("https://%s/gsma/rsp2/es2plus/%s", hostport, es2plusCommand)
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonStrB))
	req.Header.Set("X-Admin-Protocol", "gsma/rsp/v2.0.0")
	req.Header.Set("Content-Type", "application/json")


    if printHeaders {
	    fmt.Println("Request -> %s\n", formatRequest(req))
	}

	resp, err := httpClient.Do(req)

    // On http protocol failure return quickly
	if err != nil {
		return nil, err
	}

	// TODO Should check response headers here!
	// (in particular X-admin-protocol) and fail if not OK.


	// Get payload bytes from response body and return them.
	// Return an error if the bytes can't be retrieved.
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


func newEs2PlusStatusRequest(iccid string, header *ES2PlusHeader) (*ES2PlusGetProfileStatusRequest) {
	return &ES2PlusGetProfileStatusRequest{
		Header:    *header,
		IccidList: [] ES2PlusIccid{ES2PlusIccid{Iccid: iccid}},
	}
}

func GetStatus(client *Es2PlusClient, iccid string) (*ES2ProfileStatusResponse, error) {
    var result = new(ES2ProfileStatusResponse)
    es2plusCommand := "getProfileStatus"
    header := newEs2plusHeader(client)
    payload := newEs2PlusStatusRequest(iccid, header)
    err := marshalUnmarshalGeneriEs2plusCommand(client, es2plusCommand, payload, result)
    return result, err
}


