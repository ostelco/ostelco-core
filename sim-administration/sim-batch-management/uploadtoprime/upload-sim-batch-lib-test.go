package uploadtoprime

import (
	"fmt"
	"gotest.tools/assert"
	"testing"
)

func TestParseInputFileGeneratorCommmandline(t *testing.T) {

	parsedBatch := ParseInputFileGeneratorCommmandline()
	assert.Equal(t, "Footel", parsedBatch.Customer)
	assert.Equal(t, "BAR_FOOTEL_STD", parsedBatch.ProfileType)
	assert.Equal(t, "20191007", parsedBatch.OrderDate)
	assert.Equal(t, "2019100701", parsedBatch.BatchNo)
	assert.Equal(t, 10, parsedBatch.Quantity)
	assert.Equal(t, 894700000000002214, parsedBatch.FirstIccid)
	assert.Equal(t, 242017100012213, parsedBatch.FirstImsi)
}

// TODO: Make a test that checks that the correct number of things are made,
//       and also that the right things are made.   Already had one fencepost error
//       on this stuff.

func TestGenerateInputFile(t *testing.T) {
	parsedBatch := ParseInputFileGeneratorCommmandline()
	var result = GenerateInputFile(parsedBatch)

	fmt.Println(result)
}
