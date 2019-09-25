package main

import (
	"bufio"
	"fmt"
	"gotest.tools/assert"
	"log"
	"os"
	"regexp"
	"strings"
	"testing"
)

type OutputFileRecord struct {
	Filename string
}

const (
	INITIAL            = "initial"
	HEADER_DESCRIPTION = "header_description"
	INPUT_VARIABLES    = "input_variables"
	OUTPUT_VARIABLES   = "output_variables"
	UNKNOWN_HEADER     = "unknown"
)

type ParserState struct {
	currentState string
	inputVariables     map[string]string
	headerDescription  map[string]string
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
		currentState: INITIAL,
	}


	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := scanner.Text()
		if isComment(line) {
			log.Print("comment recognized")
			continue
		} else if isSectionHeader(line) {
			log.Print("Section header recognized")
			nextMode := modeFromSectionHeader(line)
			fmt.Println("Pre-transition ", state.currentState)
			transitionMode(&state, nextMode)
			fmt.Println("Post-transition ", state.currentState)
			continue
		}

		fmt.Println("Hello ", state.currentState)
		if state.currentState == HEADER_DESCRIPTION {
			log.Print("foo", line)
			var splitString = strings.Split(line, ":")
			if len(splitString) != 2 {
				log.Fatalf("Unparsable input variable string: '%s'\n", line)
			}

			key := strings.TrimSpace(splitString[0])
			value := strings.TrimSpace(splitString[1])


			log.Print("key =", key, ", value =", value)

			state.headerDescription[key] = value
		} else if state.currentState == INPUT_VARIABLES  {
			if line == "var_In:" {
				continue
			}
			var splitString = strings.Split(line, ":")
			if len(splitString) != 2 {
				log.Fatalf("Unparsable input variable string: '%s'\n", line)
			}
			key := strings.TrimSpace(splitString[0])
			value := strings.TrimSpace(splitString[1])
			state.inputVariables[key] = value
		} else if state.currentState == OUTPUT_VARIABLES  {
			log.Print("baz", line)
		} else if state.currentState == UNKNOWN_HEADER {
			log.Print("gazonk", line)
		}
	}

	if err := scanner.Err(); err != nil {
		log.Fatal(err)
	}

	result := OutputFileRecord{
		Filename: filename,
	}

	return result, nil
}

func transitionMode(state *ParserState, targetState string) {
	log.Printf("Transitioning from state '%s' to '%s'", state.currentState, targetState)
	state.currentState = targetState
}

func modeFromSectionHeader(s string) string {
	sectionName := s[1:len(s)]
	fmt.Printf("section name '%s'\n", sectionName)
	if (sectionName == "HEADER DESCRIPTION") {
		return HEADER_DESCRIPTION
	} else if (sectionName == "INPUT VARIABLES") {
		return INPUT_VARIABLES
	} else if (sectionName == "OUTPUT VARIABLES") {
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

func Test(t *testing.T) {
	sample_output_file_name := "sample_out_file_for_testing.out"
	outputFileRecord, _ := ReadOutputFile(sample_output_file_name)

	assert.Equal(t, sample_output_file_name, outputFileRecord.Filename)
}
