package outfileparser

import (
	"gotest.tools/assert"
	"testing"
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



/* TODO: Uncomment this test, and start on making it pass.
func TestReadingComplexOutputFile(t *testing.T) {
	sample_output_file_name := "sample-out-2.out"
	record := ParseOutputFile(sample_output_file_name)
	fmt.Println("Record = ", record)
}
 */


func TestParseOutputVariablesLine(t *testing.T) {
	varOutLine := "var_out:ICCID/IMSI/PIN1/PUK1/PIN2/PUK2/ADM1/KI/Access_Control/Code Retailer/Code ADM/ADM2/ADM3/ADM4"

	m := make(map[string]int)
	err := ParseVarOutLine(varOutLine, &m)
	if err != nil {
		t.Error("Couldn't parse var_out line:", err)
		t.Fail()
	}

	assert.Equal(t, m["ICCID"], 0)
	assert.Equal(t, m["IMSI"], 1)
	assert.Equal(t, m["PIN1"], 2)
	assert.Equal(t, m["PUK1"], 3)
	assert.Equal(t, m["PIN2"], 4)
	assert.Equal(t, m["PUK2"], 5)
	assert.Equal(t, m["ADM1"], 6)
	assert.Equal(t, m["KI"], 7)
	assert.Equal(t, m["Access_Control"], 8)
	assert.Equal(t, m["Code Retailer"], 9)
	assert.Equal(t, m["Code ADM"], 10)
	assert.Equal(t, m["ADM2"], 11)
	assert.Equal(t, m["ADM3"], 12)
	assert.Equal(t, m["ADM4"], 13)
}
