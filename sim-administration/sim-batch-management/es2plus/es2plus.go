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


// Client is an external interface for ES2+ client
type Client interface {
	GetStatus(iccid string) (*ProfileStatus, error)
	RecoverProfile(iccid string, targetState string) (*RecoverProfileResponse, error)
	CancelOrder(iccid string, targetState string) (*CancelOrderResponse, error)
	DownloadOrder(iccid string) (*DownloadOrderResponse, error)
	ConfirmOrder(iccid string) (*ConfirmOrderResponse, error)
	ActivateIccid(iccid string) (*ProfileStatus, error)
	RequesterID() string
}

///
///  Generic headers for invocations and responses
///


// Header is a generic header for all ES2+ invocations.
type Header struct {
	FunctionrequesterIDentifier string `json:"functionrequesterIDentifier"`
	FunctionCallIdentifier      string `json:"functionCallIdentifier"`
}

// GetProfileStatusRequest holds a request object for the profileStatus es2+ command.
type GetProfileStatusRequest struct {
	Header    Header  `json:"header"`
	IccidList []ICCID `json:"iccidList"`
}

// ICCID holder of ICCID values in the Es2+ protocol.
type ICCID struct {
	Iccid string `json:"iccid"`
}

// FunctionExecutionStatus is part of the generic es2+ response.
type FunctionExecutionStatus struct {
	FunctionExecutionStatusType string                `json:"status"`
	StatusCodeData              StatusCodeData `json:"statusCodeData"`
}

// ResponseHeader is part of the generic response header in es2+ reponses.
type ResponseHeader struct {
	FunctionExecutionStatus FunctionExecutionStatus `json:"FunctionExecutionStatus"`
}

//
//  Status code invocation.
//


// StatusCodeData payload from the function execution status field of the
// ES2+ protocol header.
type StatusCodeData struct {
	SubjectCode       string `json:"subjectCode"`
	ReasonCode        string `json:"reasonCode"`
	SubjectIdentifier string `json:"subjectIdentifier"`
	Message           string `json:"message"`
}

type es2ProfileStatusResponse struct {
	Header              ResponseHeader `json:"header"`
	ProfileStatusList   []ProfileStatus       `json:"profileStatusList"`
	CompletionTimestamp string                `json:"completionTimestamp"`
}


// ProfileStatus holds the "profile status" part of a ProfileStatusResponse
// - response returned from an es2+ request.
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

// RecoverProfileRequest is the payload of the recoverProfile request.
type RecoverProfileRequest struct {
	Header        Header        `json:"header"`
	Iccid         string        `json:"iccid"`
	ProfileStatus string        `json:"profileStatus"`
}

// RecoverProfileResponse is the payload of the recoverProfile response.
type RecoverProfileResponse struct {
	Header ResponseHeader `json:"header"`
}


// CancelOrderRequest the payload of the cancelOrder request response.
type CancelOrderRequest struct {
	Header                      Header `json:"header"`
	Iccid                       string        `json:"iccid"`
	FinalProfileStatusIndicator string        `json:"finalProfileStatusIndicator"`
}

// CancelOrderResponse the payload of the cancelOrder request response.
type CancelOrderResponse struct {
	Header ResponseHeader `json:"header"`
}


// DownloadOrderRequest The payload of a downloadOrder request.
type DownloadOrderRequest struct {
	Header      Header `json:"header"`
	Iccid       string        `json:"iccid"`
	Eid         string        `json:"eid,omitempty"`
	Profiletype string        `json:"profiletype,omitempty"`
}


// DownloadOrderResponse the response of a DownloadOrder command
type DownloadOrderResponse struct {
	Header ResponseHeader `json:"header"`
	Iccid  string                `json:"iccid"`
}

//
// ConfirmOrder invocation
//


// ConfirmOrderRequest contains parameters for the es2+ confirmOrder
// command.
type ConfirmOrderRequest struct {
	Header           Header `json:"header"`
	Iccid            string        `json:"iccid"`
	Eid              string        `json:"eid,omitempty"`
	MatchingID       string        `json:"matchingId,omitempty"`
	ConfirmationCode string        `json:"confirmationCode,omitempty"`
	SmdpAddress      string        `json:"smdpAddress,omitempty"`
	ReleaseFlag      bool          `json:"releaseFlag"`
}


// ConfirmOrderResponse contains the response value for the es2+ confirmOrder
// command.
type ConfirmOrderResponse struct {
	Header      ResponseHeader `json:"header"`
	Iccid       string                `json:"iccid"`
	Eid         string                `json:"eid,omitempty"`
	MatchingID  string                `json:"matchingId,omitempty"`
	SmdpAddress string                `json:"smdpAddress,omitempty"`
}

//
//  Generating new ES2Plus clients
//


// ClientState struct representing the state of a ES2+ client.
type ClientState struct {
	httpClient  *http.Client
	hostport    string
	requesterID string
	logPayload  bool
	logHeaders  bool
}


// NewClient create a new es2+ client instance
func NewClient(certFilePath string, keyFilePath string, hostport string, requesterID string) *ClientState {
	return &ClientState{
		httpClient:  newHTTPClient(certFilePath, keyFilePath),
		hostport:    hostport,
		requesterID: requesterID,
		logPayload:  false,
		logHeaders:  false,
	}
}

func newHTTPClient(certFilePath string, keyFilePath string) *http.Client {
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

func newUUID() (string, error) {
	uuid, err := uuid.NewRandom()
	if err != nil {
		return "", err
	}
	return uuid.URN(), nil
}

func newHeader(client Client) (*Header, error) {

	functionCallIdentifier, err := newUUID()
	if err != nil {
		return nil, err
	}

	return &Header{FunctionCallIdentifier: functionCallIdentifier, FunctionrequesterIDentifier: client.RequesterID()}, nil
}

// execute is an internal function that will package a payload
// by json serializing it, then execute an ES2+ command
// and unmarshal the result into the result object.
func (client *ClientState) execute(
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

	url := fmt.Sprintf("https://%s/gsma/rsp2/es2plus/%s", client.hostport, es2plusCommand)
	req, err := http.NewRequest("POST", url, jsonStrB)
	if err != nil {
		return err
	}
	req.Header.Set("X-Admin-Protocol", "gsma/rsp/v2.0.0")
	req.Header.Set("Content-Type", "application/json")

	if client.logHeaders {
		log.Printf("Request -> %s\n", formatRequest(req))
	}

	resp, err := client.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	// TODO Should check response headers here!
	// (in particular X-admin-protocol) and fail if not OK.

	return json.NewDecoder(resp.Body).Decode(&result)
}

///
///  Externally visible API for Es2Plus protocol
///


// GetStatus  will return the status of a profile with a specific ICCID.
func (client *ClientState) GetStatus(iccid string) (*ProfileStatus, error) {
	result := new(es2ProfileStatusResponse)
	es2plusCommand := "getProfileStatus"
	header, err := newHeader(client)
	if err != nil {
		return nil, err
	}
	payload := &GetProfileStatusRequest{
		Header:    *header,
		IccidList: []ICCID{ICCID{Iccid: iccid}},
	}
	if err = client.execute(es2plusCommand, payload, result); err != nil {
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

// RecoverProfile will recover the state of the profile with a particular ICCID,
// by setting it to the target state.
func (client *ClientState) RecoverProfile(iccid string, targetState string) (*RecoverProfileResponse, error) {
	result := new(RecoverProfileResponse)
	es2plusCommand := "recoverProfile"
	header, err := newHeader(client)
	if err != nil {
		return nil, err
	}
	payload := &RecoverProfileRequest{
		Header:        *header,
		Iccid:         iccid,
		ProfileStatus: targetState,
	}
	return result,  client.execute(es2plusCommand, payload, result)
}

// CancelOrder will cancel an order by setting  the state of the profile with a particular ICCID,
// the target state.
func (client *ClientState) CancelOrder(iccid string, targetState string) (*CancelOrderResponse, error) {
	result := new(CancelOrderResponse)
	es2plusCommand := "cancelOrder"
	header, err := newHeader(client)
	if err != nil {
		return nil, err
	}
	payload := &CancelOrderRequest{
		Header:                      *header,
		Iccid:                       iccid,
		FinalProfileStatusIndicator: targetState,
	}
	return result,  client.execute(es2plusCommand, payload, result)
}

// DownloadOrder will prepare the profile to be downloaded (first of two steps, the
// ConfirmDownload is also necessary).
func (client *ClientState) DownloadOrder(iccid string) (*DownloadOrderResponse, error) {
	result := new(DownloadOrderResponse)
	es2plusCommand := "downloadOrder"
	header, err := newHeader(client)
	if err != nil {
		return nil, err
	}
	payload := &DownloadOrderRequest{
		Header:      *header,
		Iccid:       iccid,
		Eid:         "",
		Profiletype: "",
	}
	if err =  client.execute(es2plusCommand, payload, result); err != nil {
		return nil, err
	}

	executionStatus := result.Header.FunctionExecutionStatus.FunctionExecutionStatusType
	if executionStatus == "Executed-Success" {
		return result, nil
	}
	return result, fmt.Errorf("ExecutionStatus was: ''%s'", executionStatus)
}

// ConfirmOrder will execute the second of the two steps that are necessary to prepare a profile for
// to be downloaded.
func (client *ClientState) ConfirmOrder(iccid string) (*ConfirmOrderResponse, error) {
	result := new(ConfirmOrderResponse)
	es2plusCommand := "confirmOrder"
	header, err := newHeader(client)
	if err != nil {
		return nil, err
	}
	payload := &ConfirmOrderRequest{
		Header:           *header,
		Iccid:            iccid,
		Eid:              "",
		ConfirmationCode: "",
		MatchingID:       "",
		SmdpAddress:      "",
		ReleaseFlag:      true,
	}

	if err =  client.execute(es2plusCommand, payload, result); err != nil {
		return nil, err
	}

	executionStatus := result.Header.FunctionExecutionStatus.FunctionExecutionStatusType
	if executionStatus != "Executed-Success" {
		return result, fmt.Errorf("ExecutionStatus was: ''%s'", executionStatus)
	}

	return result, nil
}


// ActivateIccid will take a profile to the state "READY" where it can be downloaded.
// This function will if poll the current status of the profile, and if
// necessary advance the state by executing the DownloadOrder and
// ConfirmOrder functions.
func (client *ClientState) ActivateIccid(iccid string) (*ProfileStatus, error) {

	result, err := client.GetStatus(iccid)
	if err != nil {
		return nil, err
	}

	if result.ACToken == "" {

		if result.State == "AVAILABLE" {
			if _, err := client.DownloadOrder(iccid); err != nil {
				return nil, err
			}
			if result, err = client.GetStatus(iccid); err != nil {
				return nil, err
			}
		}

		if result.State == "ALLOCATED" {
			if _, err = client.ConfirmOrder(iccid); err != nil {
				return nil, err
			}
		}
	}
	result, err = client.GetStatus(iccid)
	return result, err
}

// RequesterID TODO: This shouldn't have to be public, but how can it be avoided?
func (client *ClientState) RequesterID() string {
	return client.requesterID
}
