package uploadtoprime

import (
	"fmt"
	"gotest.tools/assert"
	"testing"
)

func TestParseInputFileGeneratorCommmandline(t *testing.T) {

	parsedBatch := ParseInputFileGeneratorCommmandline()
	assert.Equal(t, "Loltel", parsedBatch.customer)
	assert.Equal(t, "OYA_LOLTEL_ACB", parsedBatch.profileType)
	assert.Equal(t, "20191007", parsedBatch.orderDate)
	assert.Equal(t, "2019100701", parsedBatch.batchNo)
	assert.Equal(t, 10, parsedBatch.quantity)
	assert.Equal(t, 894700000000002214, parsedBatch.firstIccid)
	assert.Equal(t, 242017100012213, parsedBatch.firstImsi)
}


func TestGenerateInputFile(t *testing.T) {
	parsedBatch := ParseInputFileGeneratorCommmandline()
	var result = GenerateInputFile(parsedBatch)
	fmt.Println(result)
}

