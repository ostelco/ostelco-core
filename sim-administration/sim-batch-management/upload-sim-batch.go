//usr/bin/env go run "$0" "$@"; exit "$?"

package sim_batch_management

func main() {
	batch := ParseUploadFileGeneratorCommmandline()
	var csvPayload = generateCsvPayload(batch)

	GeneratePostingCurlscript(batch.url, csvPayload)
}