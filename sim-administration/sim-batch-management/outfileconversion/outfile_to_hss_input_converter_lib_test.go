package outfileconversion

import (
	"gotest.tools/assert"
	"testing"
	"fmt"
)


func TestKeywordValueParser(t *testing.T) {
	theMap := make(map[string]string)
	ParseLineIntoKeyValueMap("ProfileType     : BAR_FOOTEL_STD", theMap)

	assert.Equal(t, "BAR_FOOTEL_STD", theMap["ProfileType"])
}

func TestReadingSimpleOutputFile(t *testing.T) {
	sample_output_file_name := "sample_out_file_for_testing.out"
	record := ParseOutputFile(sample_output_file_name)

	// First parameter to check
	assert.Equal(t, sample_output_file_name, record.Filename)

	// Check that all the header variables are there
	assert.Equal(t, record.HeaderDescription["Customer"], "Footel")
	assert.Equal(t, record.HeaderDescription["ProfileType"], "BAR_FOOTEL_STD")
	assert.Equal(t, record.HeaderDescription["Order Date"], "2019092901")
	assert.Equal(t, record.HeaderDescription["Batch No"], "2019092901")
	assert.Equal(t, record.HeaderDescription["Quantity"], "3")

	// Check all the input variables are there
	assert.Equal(t, record.InputVariables["ICCID"], "8947000000000012141")
	assert.Equal(t, record.InputVariables["IMSI"], "242017100011213")

	// Check that the output entry set looks legit.
	assert.Equal(t, 3, len(record.Entries))
	assert.Equal(t, 3, record.NoOfEntries)
}

func TestReadingComplexOutputFile(t *testing.T) {
	sample_output_file_name := "sample_out_file_for_testing.out"
	record := ParseOutputFile(sample_output_file_name)
	fmt.Println("Record = ",record)
}
