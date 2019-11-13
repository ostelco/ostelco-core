package es2plus

import (
	"bytes"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"github.com/google/uuid"
	"log"
	"net/http"
	"strings"
)

///
///   External interface
///
type Es2PlusClient interface {
	GetStatus(iccid string) (*ProfileStatus, error)
	RecoverProfile(iccid string, targetState string) (*ES2PlusRecoverProfileResponse, error)
	CancelOrder(iccid string, targetState string) (*ES2PlusCancelOrderResponse, error)
	DownloadOrder(iccid string) (*ES2PlusDownloadOrderResponse, error)
	ConfirmOrder(iccid string) (*ES2PlusConfirmOrderResponse, error)
	ActivateIccid(iccid string) (*ProfileStatus, error)
	RequesterId() string
}

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
	FunctionExecutionStatusType string                `json:"status"`
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
//  Profile reset invocation
//

type ES2PlusRecoverProfileRequest struct {
	Header        ES2PlusHeader `json:"header"`
	Iccid         string        `json:"iccid"`
	ProfileStatus string        `json:"profileStatus"`
}

type ES2PlusRecoverProfileResponse struct {
	Header ES2PlusResponseHeader `json:"header"`
}

//
//  Cancel order  invocation
//

type ES2PlusCancelOrderRequest struct {
	Header                      ES2PlusHeader `json:"header"`
	Iccid                       string        `json:"iccid"`
	FinalProfileStatusIndicator string        `json:"finalProfileStatusIndicator"`
}

type ES2PlusCancelOrderResponse struct {
	Header ES2PlusResponseHeader `json:"header"`
}

//
//  Download order invocation
//

type ES2PlusDownloadOrderRequest struct {
	Header      ES2PlusHeader `json:"header"`
	Iccid       string        `json:"iccid"`
	Eid         string        `json:"eid,omitempty"`
	Profiletype string        `json:"profiletype,omitempty"`
}

type ES2PlusDownloadOrderResponse struct {
	Header ES2PlusResponseHeader `json:"header"`
	Iccid  string                `json:"iccid"`
}

//
// ConfirmOrder invocation
//

type ES2PlusConfirmOrderRequest struct {
	Header           ES2PlusHeader `json:"header"`
	Iccid            string        `json:"iccid"`
	Eid              string        `json:"eid,omitempty"`
	MatchingId       string        `json:"matchingId,omitempty"`
	ConfirmationCode string        `json:"confirmationCode,omitempty"`
	SmdpAddress      string        `json:"smdpAddress,omitempty"`
	ReleaseFlag      bool          `json:"releaseFlag"`
}

type ES2PlusConfirmOrderResponse struct {
	Header      ES2PlusResponseHeader `json:"header"`
	Iccid       string                `json:"iccid"`
	Eid         string                `json:"eid,omitempty"`
	MatchingId  string                `json:"matchingId,omitempty"`
	SmdpAddress string                `json:"smdpAddress,omitempty"`
}

//
//  Generating new ES2Plus clients
//

type Es2PlusClientState struct {
	httpClient   *http.Client
	hostport     string
	requesterId  string
	logPayload   bool
	logHeaders   bool
}

func Client(certFilePath string, keyFilePath string, hostport string, requesterId string) *Es2PlusClientState {
	return &Es2PlusClientState{
		httpClient:   newHttpClient(certFilePath, keyFilePath),
		hostport:     hostport,
		requesterId:  requesterId,
		logPayload:   false,
		logHeaders:   false,
	}
}

func newHttpClient(certFilePath string, keyFilePath string) *http.Client {
	cert, err := tls.LoadX509KeyPair(
		certFilePath,
		keyFilePath)
	if err != nil {
		log.Fatalf("server: loadkeys: %s", err)
	}

	// TODO: The certificate used to sign the other end of the TLS connection
	//       is privately signed, and at this time we don't require the full
	//       certificate chain to  be available.
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

func newEs2plusHeader(client Es2PlusClient) (*ES2PlusHeader, error) {

	functionCallIdentifier, err := newUuid()
	if err != nil {
		return nil, err
	}

	return &ES2PlusHeader{FunctionCallIdentifier: functionCallIdentifier, FunctionRequesterIdentifier: client.RequesterId()}, nil
}


func marshalUnmarshalGenericEs2plusCommand(
    client *Es2PlusClientState,
    es2plusCommand string,
    payload interface{}, result interface{}) error {

	// Serialize payload as json.
    jsonStrB := new(bytes.Buffer)
    err := json.NewEncoder(jsonStrB).Encode(payload)

	if err != nil {
		return err
	}

	if client.logPayload {
		log.Print("Payload ->", jsonStrB.String())
	}

	// Get the result of the HTTP POST as a byte array
	// that can be deserialized into json.  Fail fast
	// an error has been detected.
	return executeGenericEs2plusCommand(
	        result,
	        jsonStrB,
	        client.hostport,
	        es2plusCommand,
	        client.httpClient,
	        client.logHeaders)
}

func executeGenericEs2plusCommand(result interface {}, jsonStrB *bytes.Buffer, hostport string, es2plusCommand string, httpClient *http.Client, logHeaders bool) (error) {

	// Build and execute an ES2+ protocol request by using the serialised JSON in the
	// byte array jsonStrB in a POST request.   Set up the required
	// headers for ES2+ and content type.
	url := fmt.Sprintf("https://%s/gsma/rsp2/es2plus/%s", hostport, es2plusCommand)
	req, err := http.NewRequest("POST", url, jsonStrB)
	if err != nil {
		return  err
	}
	req.Header.Set("X-Admin-Protocol", "gsma/rsp/v2.0.0")
	req.Header.Set("Content-Type", "application/json")

	if logHeaders {
		log.Printf("Request -> %s\n", formatRequest(req))
	}

	resp, err := httpClient.Do(req)
	defer resp.Body.Close()

	// On http protocol failure return quickly
	if err != nil {
		return err
	}

	// TODO Should check response headers here!
	// (in particular X-admin-protocol) and fail if not OK.

	return json.NewDecoder(resp.Body).Decode(&result)
}

///
///  Externally visible API for Es2Plus protocol
///

func (client *Es2PlusClientState) GetStatus(iccid string) (*ProfileStatus, error) {
	result := new(ES2ProfileStatusResponse)
	es2plusCommand := "getProfileStatus"
	header, err := newEs2plusHeader(client)
	if err != nil {
		return nil, err
	}
	payload := &ES2PlusGetProfileStatusRequest{
		Header:    *header,
		IccidList: []ES2PlusIccid{ES2PlusIccid{Iccid: iccid}},
	}
	err = marshalUnmarshalGenericEs2plusCommand(client, es2plusCommand, payload, result)
	if err != nil {
		return nil, err
	}

	if len(result.ProfileStatusList) == 0 {
		return nil, nil
	} else if len(result.ProfileStatusList) == 1 {
		return &result.ProfileStatusList[0], nil
	} else {
		return nil, fmt.Errorf("GetStatus returned more than one profile")
	}
}

func (client *Es2PlusClientState) RecoverProfile(iccid string, targetState string) (*ES2PlusRecoverProfileResponse, error) {
	result := new(ES2PlusRecoverProfileResponse)
	es2plusCommand := "recoverProfile"
	header, err := newEs2plusHeader(client)
	if err != nil {
		return nil, err
	}
	payload := &ES2PlusRecoverProfileRequest{
		Header:        *header,
		Iccid:         iccid,
		ProfileStatus: targetState,
	}
	return result, marshalUnmarshalGenericEs2plusCommand(client, es2plusCommand, payload, result)
}

func (client *Es2PlusClientState) CancelOrder(iccid string, targetState string) (*ES2PlusCancelOrderResponse, error) {
	result := new(ES2PlusCancelOrderResponse)
	es2plusCommand := "cancelOrder"
	header, err := newEs2plusHeader(client)
	if err != nil {
		return nil, err
	}
	payload := &ES2PlusCancelOrderRequest{
		Header:                      *header,
		Iccid:                       iccid,
		FinalProfileStatusIndicator: targetState,
	}
	return result, marshalUnmarshalGenericEs2plusCommand(client, es2plusCommand, payload, result)
}

func (client *Es2PlusClientState) DownloadOrder(iccid string) (*ES2PlusDownloadOrderResponse, error) {
	result := new(ES2PlusDownloadOrderResponse)
	es2plusCommand := "downloadOrder"
	header, err := newEs2plusHeader(client)
	if err != nil {
		return nil, err
	}
	payload := &ES2PlusDownloadOrderRequest{
		Header:      *header,
		Iccid:       iccid,
		Eid:         "",
		Profiletype: "",
	}
	err = marshalUnmarshalGenericEs2plusCommand(client, es2plusCommand, payload, result)
	if err != nil {
		return nil, err
	}

	executionStatus := result.Header.FunctionExecutionStatus.FunctionExecutionStatusType
	if executionStatus == "Executed-Success" {
		return result, nil
	} else {
		return result, fmt.Errorf("ExecutionStatus was: ''%s'", executionStatus)
	}
}

func (client *Es2PlusClientState) ConfirmOrder(iccid string) (*ES2PlusConfirmOrderResponse, error) {
	result := new(ES2PlusConfirmOrderResponse)
	es2plusCommand := "confirmOrder"
	header, err := newEs2plusHeader(client)
	if err != nil {
		return nil, err
	}
	payload := &ES2PlusConfirmOrderRequest{
		Header:           *header,
		Iccid:            iccid,
		Eid:              "",
		ConfirmationCode: "",
		MatchingId:       "",
		SmdpAddress:      "",
		ReleaseFlag:      true,
	}

	err = marshalUnmarshalGenericEs2plusCommand(client, es2plusCommand, payload, result)
	if err != nil {
		return nil, err
	}

	executionStatus := result.Header.FunctionExecutionStatus.FunctionExecutionStatusType
	if executionStatus != "Executed-Success" {
		return result, fmt.Errorf("ExecutionStatus was: ''%s'", executionStatus)
	} else {
		return result, nil
	}
}

func (client *Es2PlusClientState) ActivateIccid(iccid string) (*ProfileStatus, error) {

	result, err := client.GetStatus(iccid)

	if err != nil {
		panic(err)
	}

	if result.ACToken == "" {

		if result.State == "AVAILABLE" {
			_, err := client.DownloadOrder(iccid)
			if err != nil {
				return nil, err
			}
			result, err = client.GetStatus(iccid)
			if err != nil {
				return nil, err
			}
		}

		if result.State == "ALLOCATED" {
			_, err = client.ConfirmOrder(iccid)
			if err != nil {
				return nil, err
			}
		}

	}
	result, err = client.GetStatus(iccid)
	return result, err
}

// TODO: This shouldn't have to be public, but how can it be avoided?
func (clientState *Es2PlusClientState) RequesterId() string {
	return clientState.requesterId
}
