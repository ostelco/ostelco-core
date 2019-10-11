package uploadtoprime

import (
	"fmt"
	"gotest.tools/assert"
	"testing"
)

func TestParseInputFileGeneratorCommmandline(t *testing.T) {

	parsedBatch := ParseInputFileGeneratorCommmandline()
	assert.Equal(t, "Footel", parsedBatch.customer)
	assert.Equal(t, "BAR_FOOTEL_STD", parsedBatch.profileType)
	assert.Equal(t, "20191007", parsedBatch.orderDate)
	assert.Equal(t, "2019100701", parsedBatch.batchNo)
	assert.Equal(t, 10, parsedBatch.quantity)
	assert.Equal(t, 894700000000002214, parsedBatch.firstIccid)
	assert.Equal(t, 242017100012213, parsedBatch.firstImsi)
}

// TODO: Make a test that checks that the correct number of things are made,
//       and also that the right things are made.   Already had one fencepost error
//       on this stuff.

func TestGenerateInputFile(t *testing.T) {
	parsedBatch := ParseInputFileGeneratorCommmandline()
	var result = GenerateInputFile(parsedBatch)

	fmt.Println(result)
}
