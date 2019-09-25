package main

import (
	"gotest.tools/assert"
	"testing"
)

type OutputFileRecord struct {
	Filename string
}

func ReadOutputFile(filename string) (OutputFileRecord, error) {
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
