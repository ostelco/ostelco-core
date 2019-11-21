package outfileparser

import (
	"bufio"
	"fmt"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/loltelutils"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/model"
	"github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/store"

	"log"
	"os"
	"regexp"
	"strconv"
	"strings"
)

const (
	INITIAL            = "initial"
	HEADER_DESCRIPTION = "header_description"
	INPUT_VARIABLES    = "input_variables"
	OUTPUT_VARIABLES   = "output_variables"
	UNKNOWN_HEADER     = "unknown"
)

type OutputFileRecord struct {
	Filename          string
	InputVariables    map[string]string
	HeaderDescription map[string]string
	Entries           []model.SimEntry
	NoOfEntries       int
	OutputFileName    string
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

type parserState struct {
	currentState      string
	inputVariables    map[string]string
	headerDescription map[string]string
	entries           []model.SimEntry
	csvFieldMap       map[string]int
}

func parseVarOutLine(varOutLine string, result *map[string]int) error {
	varOutSplit := strings.Split(varOutLine, ":")

	if len(varOutSplit) != 2 {
		return fmt.Errorf("syntax error in var_out line, more than two colon separated fields")
	}

	varOutToken := strings.TrimSpace(string(varOutSplit[0]))
	if strings.ToLower(varOutToken) != "var_out" {
		return fmt.Errorf("syntax error in var_out line.  Does not start with 'var_out', was '%s'", varOutToken)
	}

	slashedFields := strings.Split(varOutSplit[1], "/")
	for index, columnName := range slashedFields {
		(*result)[columnName] = index
	}
	return nil
}

// Implement a state machine that parses an output file.
func ParseOutputFile(filename string) (*OutputFileRecord, error) {

	if _, err := os.Stat(filename); err != nil {
		if os.IsNotExist(err) {
			return nil, fmt.Errorf("couldn't find file '%s'", filename)
		} else {
			return nil, fmt.Errorf("couldn't stat file '%s'", filename)
		}
	}

	file, err := os.Open(filename) // For read access.
	if err != nil {
		log.Fatal(err)
	}

	defer file.Close()

	state := parserState{
		currentState:      INITIAL,
		inputVariables:    make(map[string]string),
		headerDescription: make(map[string]string),
		csvFieldMap:       make(map[string]int),
	}

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {

		// Read line, trim spaces in both ends.
		line := scanner.Text()
		line = strings.TrimSpace(line)

		// Is this a line we should read quickly then
		// move on to the next...?
		if isComment(line) {
			continue
		} else if isSectionHeader(line) {
			nextMode := modeFromSectionHeader(line)
			transitionMode(&state, nextMode)
			continue
		} else if line == "OUTPUT VARIABLES" {
			transitionMode(&state, OUTPUT_VARIABLES)
			continue
		}

		// ... or should we look closer at it and parse it
		// looking for real content?

		switch state.currentState {
		case HEADER_DESCRIPTION:
			parseLineIntoKeyValueMap(line, state.headerDescription)
		case INPUT_VARIABLES:
			if line == "var_In:" || line == "Var_In_List:" || strings.TrimSpace(line) == "" {
				continue
			}

			parseLineIntoKeyValueMap(line, state.inputVariables)
		case OUTPUT_VARIABLES:

			line = strings.TrimSpace(line)
			lowercaseLine := strings.ToLower(line)

			if strings.HasPrefix(lowercaseLine, "var_out:") {
				if len(state.csvFieldMap) != 0 {
					return nil, fmt.Errorf("parsing multiple 'var_out' lines can't be right")
				}
				if err := parseVarOutLine(line, &(state.csvFieldMap)); err != nil {
					return nil, fmt.Errorf("couldn't parse output variable declaration '%s'", err)
				}
				continue
			}

			if len(state.csvFieldMap) == 0 {
				return nil, fmt.Errorf("cannot parse CSV part of input file without having first parsed a CSV header, failed when processing line '%s'", line)
			}

			line = strings.TrimSpace(line)
			if line == "" {
				continue
			}

			rawIccid, imsi, ki := parseOutputLine(state, line)

			iccidWithChecksum := rawIccid
			if strings.HasSuffix(rawIccid, "F") {
				iccidWithChecksum = loltelutils.TrimSuffix(rawIccid, 1)
			}

			var iccidWithoutChecksum = loltelutils.TrimSuffix(iccidWithChecksum, 1)
			//   TODO: Check syntax of iccid with checksum.
			entry := model.SimEntry{
				RawIccid:             rawIccid,
				IccidWithChecksum:    iccidWithChecksum,
				IccidWithoutChecksum: iccidWithoutChecksum,
				Imsi:                 imsi,
				Ki:                   ki}
			state.entries = append(state.entries, entry)

		case UNKNOWN_HEADER:
			continue

		default:
			return nil, fmt.Errorf("unknown parser state '%s'", state.currentState)
		}
	}

	if err := scanner.Err(); err != nil {
		log.Fatal(err)
	}

	countedNoOfEntries := len(state.entries)
	declaredNoOfEntities, err := strconv.Atoi(state.headerDescription["Quantity"])

	if err != nil {
		return nil, fmt.Errorf("could not find 'Quantity' field while parsing file '%s'", filename)
	}

	if countedNoOfEntries != declaredNoOfEntities {
		return nil, fmt.Errorf("mismatch between no of entities = %d, counted number of entities = %d",
			declaredNoOfEntities,
			countedNoOfEntries)
	}

	result := OutputFileRecord{
		Filename:          filename,
		InputVariables:    state.inputVariables,
		HeaderDescription: state.headerDescription,
		Entries:           state.entries,
		NoOfEntries:       declaredNoOfEntities,
		OutputFileName:    getOutputFileName(state),
	}

	return &result, nil
}

func getOutputFileName(state parserState) string {
	return "" + getCustomer(state) + "_" + getProfileType(state) + "_" + getBatchNo(state)
}

func getBatchNo(state parserState) string {
	return state.headerDescription["Batch No"]
}

func getProfileType(state parserState) string {
	return state.headerDescription["ProfileType"]
}

func getCustomer(state parserState) string {
	// TODO: Maker safe, so that it fails reliably if Customer is not in map.
	//       also use constant, not magic string
	return state.headerDescription["Customer"]
}

func parseOutputLine(state parserState, s string) (string, string, string) {
	parsedString := strings.Split(s, " ")
	return parsedString[state.csvFieldMap["ICCID"]], parsedString[state.csvFieldMap["IMSI"]], parsedString[state.csvFieldMap["KI"]]
}

func transitionMode(state *parserState, targetState string) {
	state.currentState = targetState
}

func modeFromSectionHeader(s string) string {
	sectionName := strings.Trim(s, "* ")
	switch sectionName {
	case "HEADER DESCRIPTION":
		return HEADER_DESCRIPTION
	case "INPUT VARIABLES":
		return INPUT_VARIABLES
	case "INPUT VARIABLES DESCRIPTION":
		return INPUT_VARIABLES
	case "OUTPUT VARIABLES":
		return OUTPUT_VARIABLES
	default:
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

// fileExists checks if a file exists and is not a directory before we
// try using it to prevent further errors.
func fileExists(filename string) bool {
	info, err := os.Stat(filename)
	if os.IsNotExist(err) {
		return false
	}
	return !info.IsDir()
}

// TODO: Move this into some other package. "hssoutput" or something.
func WriteHssCsvFile(filename string, sdb *store.SimBatchDB, batch *model.Batch) error {

	if fileExists(filename) {
		return fmt.Errorf("output file already exists.  '%s'", filename)
	}

	f, err := os.Create(filename)
	if err != nil {
		return fmt.Errorf("couldn't create hss csv file '%s', %v", filename, err)
	}

	if _, err = f.WriteString("ICCID, IMSI, KI\n"); err != nil {
		return fmt.Errorf("couldn't header to  hss csv file '%s', %v", filename, err)
	}

	entries, err := sdb.GetAllSimEntriesForBatch(batch.BatchID)
	if err != nil {
		return err
	}

	max := 0
	for i, entry := range entries {
		s := fmt.Sprintf("%s, %s, %s\n", entry.IccidWithChecksum, entry.Imsi, entry.Ki)
		if _, err = f.WriteString(s); err != nil {
			return fmt.Errorf("couldn't write to  hss csv file '%s', %v", filename, err)
		}
		max = i + 1
	}
	fmt.Println("Successfully written ", max, " sim card records.")
	return f.Close()
}
