//usr/bin/env go run "$0" "$@"; exit "$?"	

package main

import "github.com/ostelco/ostelco-core/sim-administration/sim-batch-management/uploadtoprime"

func main() {
	batch := uploadtoprime.ParseUploadFileGeneratorCommmandline()
	var csvPayload = uploadtoprime.GenerateCsvPayload(batch)

	uploadtoprime.GeneratePostingCurlscript(batch.Url, csvPayload)
}