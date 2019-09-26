package main

import (
	"bufio"
	"gotest.tools/assert"
	"log"
	"os"
	"regexp"
	"strings"
	"testing"
)

type OutputFileRecord struct {
	Filename          string
	inputVariables    map[string]string
	headerDescription map[string]string
	entries           []SimEntry
}

const (
	INITIAL            = "initial"
	HEADER_DESCRIPTION = "header_description"
	INPUT_VARIABLES    = "input_variables"
	OUTPUT_VARIABLES   = "output_variables"
	UNKNOWN_HEADER     = "unknown"
)

type SimEntry struct {
	iccid string
	imsi  string
	ki    string
}

type ParserState struct {
	currentState      string
	inputVariables    map[string]string
	headerDescription map[string]string
	entries           []SimEntry
}

func parseLineIntoKeyValueMap(line string, theMap map[string]string) {
	var splitString = strings.Split(line, ":")
	if len(splitString) != 2 {
		log.Fatalf("Unparsable colon separated key/value pair: '%s'\n", line)
	}
	key := strings.TrimSpace(splitString[0])
	value := strings.TrimSpace(splitString[1])
	theMap[key] = value
}

func ReadOutputFile(filename string) (OutputFileRecord, error) {

	_, err := os.Stat(filename)

	if os.IsNotExist(err) {
		log.Fatalf("Couldn't find file '%s'\n", filename)
	}
	if err != nil {
		log.Fatalf("Couldn't stat file '%s'\n", filename)
	}

	file, err := os.Open(filename) // For read access.
	if err != nil {
		log.Fatal(err)
	}

	defer file.Close()

	state := ParserState{
		currentState:      INITIAL,
		inputVariables:    make(map[string]string),
		headerDescription: make(map[string]string),
	}

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()

		// Is this a line we should read quickly then
		// move on to the next...?
		if isComment(line) {
			continue
		} else if isSectionHeader(line) {
			nextMode := modeFromSectionHeader(line)
			transitionMode(&state, nextMode)
			continue
		}

		// ... or should we look closer at it and parse it
		// looking for real content?
		if state.currentState == HEADER_DESCRIPTION {
			parseLineIntoKeyValueMap(line, state.headerDescription)
		} else if state.currentState == INPUT_VARIABLES {
			if line == "var_In:" {
				continue
			}
			parseLineIntoKeyValueMap(line, state.inputVariables)
		} else if state.currentState == OUTPUT_VARIABLES {
			if line == "var_Out: ICCID/IMSI/KI" {
				continue
			}

			// We won't handle all variations, only the most common one
			// if more fancy variations are necessary (with pin codes etc), then
			// we'll add them as needed.
			if strings.HasPrefix(line, "var_Out: ") {
				log.Fatalf("Unknown output format, only know how to handle ICCID/IMSI/KI, but was '%s'\n", line)
			}

			line = strings.TrimSpace(line)
			if line == "" {
				continue
			}
			iccid, imsi, ki := parseOutputLine(line)
			entry := SimEntry{iccid: iccid, imsi: imsi, ki: ki}
			state.entries = append(state.entries, entry)

		} else if state.currentState == UNKNOWN_HEADER {
			continue
		}
	}

	if err := scanner.Err(); err != nil {
		log.Fatal(err)
	}

	result := OutputFileRecord{
		Filename:          filename,
		inputVariables:    state.inputVariables,
		headerDescription: state.headerDescription,
		entries:           state.entries,
	}

	return result, nil
}

func parseOutputLine(s string) (string, string, string) {
	parsedString := strings.Split(s, " ")
	return parsedString[0], parsedString[1], parsedString[2]
}

func transitionMode(state *ParserState, targetState string) {
	state.currentState = targetState
}

func modeFromSectionHeader(s string) string {
	sectionName := s[1:len(s)]
	if sectionName == "HEADER DESCRIPTION" {
		return HEADER_DESCRIPTION
	} else if sectionName == "INPUT VARIABLES" {
		return INPUT_VARIABLES
	} else if sectionName == "OUTPUT VARIABLES" {
		return OUTPUT_VARIABLES
	} else {
		return UNKNOWN_HEADER
	}
}

func isSectionHeader(s string) bool {
	match, _ := regexp.MatchString("^\\*([A-Z0-9 ])+$", s)
	return match
}

func isComment(s string) bool {
	match, _ := regexp.MatchString("^\\*+$", s)
	return match
}

///
///   The tests
///

func testKeywordValueParser(t *testing.T) {
	theMap := make(map[string]string)
	parseLineIntoKeyValueMap("ProfileType     : BAR_FOOTEL_STD", theMap)

	assert.Equal(t, "BAR_FOOTEL_STD", theMap["ProfileType"])
}

func testReadOutputFile(t *testing.T) {
	sample_output_file_name := "sample_out_file_for_testing.out"
	record, _ := ReadOutputFile(sample_output_file_name)
	assert.Equal(t, sample_output_file_name, record.Filename)

	// Check all the header variables
	assert.Equal(t, record.headerDescription["Customer"], "Footel")
	assert.Equal(t, record.headerDescription["ProfileType"], "BAR_FOOTEL_STD")
	assert.Equal(t, record.headerDescription["Order Date"], "2019092901")
	assert.Equal(t, record.headerDescription["Batch No"], "2019092901")
	assert.Equal(t, record.headerDescription["Quantity"], "3")

	// Check all the input variables
	assert.Equal(t, record.inputVariables["ICCID"], "8947000000000012141")
	assert.Equal(t, record.inputVariables["IMSI"], "242017100011213")
}

//
//  The test suite tying it all together.
//
func Test(t *testing.T) {
	testKeywordValueParser(t)
	testReadOutputFile(t)
}
