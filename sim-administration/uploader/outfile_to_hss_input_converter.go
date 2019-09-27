//usr/bin/env go run "$0" "$@"; exit "$?"

package main

import (
	"bufio"
	"flag"
	"fmt"
	"log"
	"os"
	"regexp"
	"strconv"
	"strings"
)

type OutputFileRecord struct {
	Filename          string
	inputVariables    map[string]string
	headerDescription map[string]string
	entries           []SimEntry
	noOfEntries       int
}

const (
	INITIAL            = "initial"
	HEADER_DESCRIPTION = "header_description"
	INPUT_VARIABLES    = "input_variables"
	OUTPUT_VARIABLES   = "output_variables"
	UNKNOWN_HEADER     = "unknown"
)

type SimEntry struct {
	rawIccid             string
	iccidWithChecksum    string
	iccidWithoutChecksum string
	imsi                 string
	ki                   string
}

type ParserState struct {
	currentState      string
	inputVariables    map[string]string
	headerDescription map[string]string
	entries           []SimEntry
}

func ParseLineIntoKeyValueMap(line string, theMap map[string]string) {
	var splitString = strings.Split(line, ":")
	if len(splitString) != 2 {
		log.Fatalf("Unparsable colon separated key/value pair: '%s'\n", line)
	}
	key := strings.TrimSpace(splitString[0])
	value := strings.TrimSpace(splitString[1])
	theMap[key] = value
}

func trimSuffix(s string, suffixLen int) string {
	return s[:len(s)-suffixLen]
}

func ReadOutputFile(filename string) (OutputFileRecord) {

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
			ParseLineIntoKeyValueMap(line, state.headerDescription)
		} else if state.currentState == INPUT_VARIABLES {
			if line == "var_In:" {
				continue
			}
			ParseLineIntoKeyValueMap(line, state.inputVariables)
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
			rawIccid, imsi, ki := parseOutputLine(line)

			iccidWithChecksum := rawIccid
			if strings.HasSuffix(rawIccid, "F") {
				iccidWithChecksum = trimSuffix(rawIccid, 1)
			}

			var iccidWithoutChecksum = trimSuffix(iccidWithChecksum, 1)
			// TODO: Enable this!! CheckICCIDSyntax(iccidWithChecksum)
			entry := SimEntry{
				rawIccid:             rawIccid,
				iccidWithChecksum:    iccidWithChecksum,
				iccidWithoutChecksum: iccidWithoutChecksum,
				imsi:                 imsi,
				ki:                   ki}
			state.entries = append(state.entries, entry)

		} else if state.currentState == UNKNOWN_HEADER {
			continue
		}
	}

	if err := scanner.Err(); err != nil {
		log.Fatal(err)
	}

	countedNoOfEntries := len(state.entries)
	declaredNoOfEntities, err := strconv.Atoi(state.headerDescription["Quantity"])

	if err != nil {
		log.Fatal("Could not find  declared quantity of entities")
	}

	if countedNoOfEntries != declaredNoOfEntities {
		log.Fatalf("Declared no of entities = %d, counted nunber of entities = %d. Mismatch!",
			declaredNoOfEntities,
			countedNoOfEntries)
	}

	result := OutputFileRecord{
		Filename:          filename,
		inputVariables:    state.inputVariables,
		headerDescription: state.headerDescription,
		entries:           state.entries,
		noOfEntries:       declaredNoOfEntities,
	}

	return result
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

/// XXX Add a main function that
//   a) Reads the output file, then produces a HSS input file from that
//   b) Later, integrate with the prime input generator, and add a
//      database to keep track of the workflow.
//
//

//
// Set up command line parsing
//
func parseCommandLine() (string, string) {
	inputFile := flag.String("input-file",
		"not  a valid filename",
		"path to .out file used as input file")

	outputFile := flag.String("output-file",
		"not  a valid filename",
		"path to .csv file used as input file")

	//
	// Parse input according to spec above
	//
	flag.Parse()
	return *inputFile, *outputFile
}

func WriteHssCsvFile(filename string, entries []SimEntry) (error) {
	f, err := os.Create(filename)
	if err != nil {
		log.Fatal("Couldn't create hss csv file '",filename, "': ", err)
	}

	max := 0
	for i, entry := range entries {
		s := fmt.Sprintf("%s, %s, %s\n", entry.iccidWithChecksum, entry.imsi, entry.ki)
		_, err = f.WriteString(s)
		if err != nil {
			log.Fatal("Couldn't write to  hss csv file '",filename, "': ", err)
		}
		max = i + 1
	}
	fmt.Println("Successfully written ", max, " sim card records.")
	return f.Close()
}

///
///   Main.
///

func main() {
	inputFile, outputFile := parseCommandLine()

	fmt.Println("inputFile = ", inputFile)
	fmt.Println("outputFile = ", outputFile)
	
	outRecord := ReadOutputFile(inputFile)
	
	err := WriteHssCsvFile(outputFile, outRecord.entries)
	if err != nil {
		log.Fatal("Couldn't close output file '", outputFile, "'.  Error = '", err,"'")
	}
}

