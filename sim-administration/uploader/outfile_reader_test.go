package main

import (
	"bufio"
	"fmt"
	"gotest.tools/assert"
	"log"
	"os"
	"testing"
)

type OutputFileRecord struct {
	Filename string
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

	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fmt.Println(scanner.Text())
	}

	if err := scanner.Err(); err != nil {
		log.Fatal(err)
	}

	result := OutputFileRecord{
		Filename: filename,
	}

	return result, nil
}

func Test(t *testing.T) {
	sample_output_file_name := "sample_out_file_for_testing.out"
	outputFileRecord, _ := ReadOutputFile(sample_output_file_name)

	assert.Equal(t, sample_output_file_name, outputFileRecord.Filename)
}
