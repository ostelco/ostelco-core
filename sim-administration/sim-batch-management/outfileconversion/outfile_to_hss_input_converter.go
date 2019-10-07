//usr/bin/env go run "$0" "$@"; exit "$?"
/**
 * This program is intended to be used from the command line, and will convert an
 * output file from a sim card vendor into an input file for a HSS. The assumptions
 * necessary for this to work are:
 *
 *  * The SIM card vendor produces output files similar to the example .out file
 *     found in the same source directory as this program
 *
 *  * The HSS accepts input as a CSV file, with header line 'ICCID, IMSI, KI' and subsequent
 *    lines containing ICCID/IMSI/Ki fields, all separated by commas.
 *
 * Needless to say, the outmost care should be taken when handling Ki values and
 * this program must, as a matter of course, be considered a security risk, as
 * must all  software that touch SIM values.
 *
 * With that caveat in place, the usage of this program typically looks like
 * this:
 *
 *    ./outfile_to_hss_input_converter.go  \
 *              -input-file sample_out_file_for_testing.out
 *              -output-file-prefix  ./hss-input-for-
 *
 * (followed by cryptographically strong erasure of the .out file,
 *  encapsulation of the .csv file in strong cryptography etc., none
 *  of which are handled by this script).
 */

package outfileconversion

import (
	"fmt"
	"log"
)

func main() {
	inputFile, outputFilePrefix := parseOutputToHssConverterCommandLine()

	fmt.Println("inputFile = ", inputFile)
	fmt.Println("outputFilePrefix = ", outputFilePrefix)

	outRecord := ReadOutputFile(inputFile)
	outputFile := outputFilePrefix + outRecord.outputFileName + ".csv"
	fmt.Println("outputFile = ", outputFile)

	err := WriteHssCsvFile(outputFile, outRecord.entries)
	if err != nil {
		log.Fatal("Couldn't close output file '", outputFilePrefix, "'.  Error = '", err, "'")
	}
}
