package outfileconversion

import (
	"gotest.tools/assert"
	"testing"
)

func testKeywordValueParser(t *testing.T) {
	theMap := make(map[string]string)
	ParseLineIntoKeyValueMap("ProfileType     : BAR_FOOTEL_STD", theMap)

	assert.Equal(t, "BAR_FOOTEL_STD", theMap["ProfileType"])
}

func testReadOutputFile(t *testing.T) {
	sample_output_file_name := "sample_out_file_for_testing.out"
	record := ReadOutputFile(sample_output_file_name)

	// First parameter to check
	assert.Equal(t, sample_output_file_name, record.Filename)

	// Check that all the header variables are there
	assert.Equal(t, record.headerDescription["Customer"], "Footel")
	assert.Equal(t, record.headerDescription["ProfileType"], "BAR_FOOTEL_STD")
	assert.Equal(t, record.headerDescription["Order Date"], "2019092901")
	assert.Equal(t, record.headerDescription["Batch No"], "2019092901")
	assert.Equal(t, record.headerDescription["Quantity"], "3")

	// Check all the input variables are there
	assert.Equal(t, record.inputVariables["ICCID"], "8947000000000012141")
	assert.Equal(t, record.inputVariables["IMSI"], "242017100011213")

	// Check that the output entry set looks legit.
	assert.Equal(t, 3, len(record.Entries))
	assert.Equal(t, 3, record.noOfEntries)
}
