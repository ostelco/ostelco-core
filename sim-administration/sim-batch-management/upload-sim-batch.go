//usr/bin/env go run "$0" "$@"; exit "$?"	

package main

import "github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/uploadtoprime"

func main() {
	batch := uploadtoprime.ParseUploadFileGeneratorCommmandline()

	// TODO: Combine these two into something inside uploadtoprime.
	//       It's unecessary to break the batch thingy open in this way.
	var csvPayload = uploadtoprime.GenerateCsvPayload(batch)

	uploadtoprime.GeneratePostingCurlscript(batch.Url, csvPayload)
}