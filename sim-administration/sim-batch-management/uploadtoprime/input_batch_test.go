package uploadtoprime

import (
	"gotest.tools/assert"
	"testing"
)

func TestParseInputFileGeneratorCommmandline(t *testing.T) {
	parsedBatch := ParseInputFileGeneratorCommmandline()
	assert.Equal(t, "Foo", parsedBatch.customer)
	assert.Equal(t, "Bar", parsedBatch.profileType)
	assert.Equal(t, "baz", parsedBatch.orderDate)
	assert.Equal(t, "gazonk", parsedBatch.batchNo)
	assert.Equal(t, 5, parsedBatch.length)
	assert.Equal(t, 0, parsedBatch.firstIccid)
	assert.Equal(t, 2, parsedBatch.firstImsi)

	return InputBatch{customer: "Foo", profileType: "Bar", orderDate: "baz", batchNo: "gazonk", length: 5, firstIccid: 0, firstImsi:2}
}
