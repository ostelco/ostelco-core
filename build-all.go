//usr/bin/env go run "$0" "$@"; exit "$?"

// EXPERIMENTAL:   This may or may not end up as production code.
// INTENT:         Replace the current build-all.sh script with a go program
//   	           that can be run from the command line as if it were a script
//		           The intent is straignt forward replacement, but with type
//                 safety and perhaps a bit of additional reliability.
//                 The build-all.sh script isn't used by a lot of people, so
//                 it's some distance "off broadway", and that may help the test
//                 very prestigious.  On the other hand, the script does contain
//                 sufficient complexity to be a worthy target

package main

import (
	"bytes"
	"crypto/md5"
	"encoding/hex"
	"fmt"
	"github.com/ostelco-core/goscript"
	"io"
	"log"
	"os"
	"os/exec"
)

func generateEspEndpointCertificates() {

	originalCertPath := "certs/ocs.dev.ostelco.org/nginx.crt"
	activeCertPath := "ocsgw/cert/metrics.crt"

	if goscript.bothFilesExistsButAreDifferent(originalCertPath, activeCertPath) {
		goscript.deleteFile(originalCertPath)
		goscript.deleteFile(activeCertPath)
	}

	// If no original certificate (for whatever reason),
	// generate a new one.
	if !fileExists(originalCertPath) {
		generateNewCertificate(originalCertPath, "ocs.dev.ostelco.org")
	}

	if goscript.fileExists(activeCertPath) {
		goscript.copyFile(originalCertPath, activeCertPath)
	}
}

func generateNewCertificate(certificateFilename string, certificateDomain string) {
	cmd := fmt.Sprintf("scripts/generate-selfsigned-ssl-certs.sh %s", certificateDomain)
	out, err := exec.Command("bash", "-c", cmd).Output()
	if err != nil {
		log.Fatalf("Could not generate self signed certificate for domain '%s'.\n      Reason: %s", certificateDomain, err)
	}

	log.Printf("out = %s", out)

	if !fileExists(certificateFilename) {
		log.Fatalf("Did not generate self signed for domain '%s'", certificateDomain)
	}
}


func distributeServiceAccountConfigs() {
	log.Printf("Distributing service account configs\n")
	dirsThatNeedsServiceAccountConfigs := [...]string{
		0: " acceptance-tests/config",
		1: "dataflow-pipelines/config",
		2: "ocsgw/config",
		3: "bq-metrics-extractor/config",
		4: "auth-server/config prime/config"}
	serviceAccountMD5 := "c54b903790340dd9365fa59fce3ad8e2"
	serviceAccountJsonFilename := "prime-service-account.json"

	if !goscript.fileExists(serviceAccountJsonFilename) {
		log.Fatalf("ERROR: : Could not find master service-account file'%s'", serviceAccountJsonFilename)
	}

	rootMd5, err := goscript.hash_file_md5(serviceAccountJsonFilename)
	if err != nil {
		log.Fatalf("Could not calculate md5 from file '%s'", serviceAccountJsonFilename)
	}

	if serviceAccountMD5 != rootMd5 {
		log.Fatalf("MD5 of root service acccount file '%s' is not '%s', so bailing out", serviceAccountJsonFilename, serviceAccountMD5)
	}

	for _, dir := range dirsThatNeedsServiceAccountConfigs {
		currentFilename := fmt.Sprintf("%s/%s", dir, serviceAccountJsonFilename)

		if !goscript.fileExists(currentFilename) {
			goscript.copyFile(serviceAccountJsonFilename, currentFilename)
		} else {

			localMd5, err := hash_file_md5(serviceAccountJsonFilename)
			if err != nil {
				log.Fatalf("ERROR: Could not calculate md5 from file '%s'", serviceAccountJsonFilename)
			}

			if localMd5 != rootMd5 {
				goscript.copyFile(serviceAccountJsonFilename, currentFilename)
			}
		}
	}
}

func checkIfDockerIsRunning() bool {
	cmd := "if [[  -z \"$( docker version | grep Version:) \" ]] ; then echo 'Docker not running' ; fi"
	out, err := exec.Command("bash", "-c", cmd).Output()
	return "Docker not running" != string(out) && err == nil
}

func assertDockerIsRunning() {
	if !checkIfDockerIsRunning() {
		log.Fatal("Docker is not running")
	}
}


func main() {
	log.Printf("About to get started\n")

	//
	// Check all preconditions for building
	//

	goscript.checkForDependencies()
	goscript.checkThatEnvironmentVariableIsSet("STRIPE_API_KEY")
	generateEspEndpointCertificates()
	goscript.checkThatEnvironmentVariableIsSet("GCP_PROJECT_ID")

	distributeServiceAccountConfigs()

	// If the stripe endpoint secret is not set, then
	// then just set a random value.  It's not important right now,
	// but it may cause build failure, so we set it to something.
	if len(os.Getenv("STRIPE_ENDPOINT_SECRET")) == 0 {
		log.Printf("Setting value of STRIPE_ENDPOINT_SECRET to 'thisIsARandomString'")
		os.Setenv("STRIPE_ENDPOINT_SECRET", "thisIsARandomString")
	}
	assertDockerIsRunning()

	//
	// All preconditions are now satisfied, now run the actual build commands
	// and terminate the build process if any of them fails.
	//

	goscript.AssertSuccesfulRun("./gradlew build")
	goscript.AssertSuccesfulRun("docker-compose down")
	goscript.AssertSuccesfulRun("docker-compose up --build --abort-on-container-exit")
}

