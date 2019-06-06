//usr/bin/env go run "$0" "$@"; exit "$?"

// INTENT:         Replace the current build-all.sh script with a go program
//   	           that can be run from the command line as if it were a script

package main

import (
	"./github.com/ostelco-core/goscript"
	"fmt"
	"log"
	"os"
)

func generateEspEndpointCertificates(originalCertPath string, activeCertPath string, domainName string) {
	if goscript.BothFilesExistsButAreDifferent(originalCertPath, activeCertPath) {
		goscript.DeleteFile(originalCertPath)
		goscript.DeleteFile(activeCertPath)
	}
	// If no original certificate (for whatever reason),
	// generate a new one.
	if !goscript.FileExists(originalCertPath) {
		generateNewCertificate(originalCertPath, domainName)
	}
	if goscript.FileExists(activeCertPath) {
		goscript.CopyFile(originalCertPath, activeCertPath)
	}
}

func generateNewCertificate(certificateFilename string, certificateDomain string) {
	cmd := fmt.Sprintf("scripts/generate-selfsigned-ssl-certs.sh %s", certificateDomain)
	goscript.AssertSuccesfulRun(cmd)
	if !goscript.FileExists(certificateFilename) {
		log.Fatalf("Did not generate self signed for domain '%s'", certificateDomain)
	}
}

func distributeServiceAccountConfigs(
	serviceAccountJsonFilename string,
	serviceAccountFileMD5Checksum string,
	dirsThatNeedsServiceAccountConfigs ...string) {
	log.Printf("Distributing service account configs\n")
	if !goscript.FileExists(serviceAccountJsonFilename) {
		log.Fatalf("ERROR: : Could not find master service-account file'%s'", serviceAccountJsonFilename)
	}
	rootMd5, err := goscript.Hash_file_md5(serviceAccountJsonFilename)
	if err != nil {
		log.Fatalf("Could not calculate md5 from file '%s'", serviceAccountJsonFilename)
	}
	if serviceAccountFileMD5Checksum != rootMd5 {
		log.Fatalf("MD5 of root service acccount file '%s' is not '%s', so bailing out",
			serviceAccountJsonFilename,
			serviceAccountFileMD5Checksum)
	}
	for _, dir := range dirsThatNeedsServiceAccountConfigs {
		currentFilename := fmt.Sprintf("%s/%s", dir, serviceAccountJsonFilename)

		if goscript.FileExists(currentFilename) {
			localMd5, err := goscript.Hash_file_md5(serviceAccountJsonFilename)
			if err != nil {
				log.Fatalf("ERROR: Could not calculate md5 from file '%s'", serviceAccountJsonFilename)
			}

			if localMd5 != rootMd5 {
				goscript.CopyFile(serviceAccountJsonFilename, currentFilename)
			}
		} else {
			goscript.CopyFile(serviceAccountJsonFilename, currentFilename)
		}
	}
}

func generateDummyStripeEndpointSecretIfNotSet() {
	// If the stripe endpoint secret is not set, then
	// then just set a random value.  It's not important right now,
	// but it may cause build failure, so we set it to something.
	if len(os.Getenv("STRIPE_ENDPOINT_SECRET")) == 0 {
		log.Printf("Setting value of STRIPE_ENDPOINT_SECRET to 'thisIsARandomString'")
		os.Setenv("STRIPE_ENDPOINT_SECRET", "thisIsARandomString")
	}
}

func main() {
	log.Printf("About to get started\n")

	//
	// Ensure that  all preconditions for building and testing are met, if not
	// fail and terminate execution.
	//

	goscript.AssertThatScriptCommandsAreAvailable("docker-compose", "./gradlew", "docker", "cmp")
	goscript.AssertThatEnvironmentVariableaAreSet("STRIPE_API_KEY", "GCP_PROJECT_ID")
	goscript.AssertDockerIsRunning()
	generateDummyStripeEndpointSecretIfNotSet()
	distributeServiceAccountConfigs(
		"prime-service-account.json",
		"c54b903790340dd9365fa59fce3ad8e2",
		"acceptance-tests/config",
		"dataflow-pipelines/config",
		"ocsgw/config",
		"bq-metrics-extractor/config",
		"auth-server/config prime/config")
	generateEspEndpointCertificates(
		"certs/ocs.dev.ostelco.org/nginx.crt",
		"ocsgw/cert/metrics.crt",
		"ocs.dev.ostelco.org")

	//
	// All preconditions are now satisfied, now run the actual build/test commands
	// and terminate the build process if any of them fails.
	//

	goscript.AssertSuccesfulRun("./gradlew build")
	goscript.AssertSuccesfulRun("docker-compose down")
	goscript.AssertSuccesfulRun("docker-compose up --build --abort-on-container-exit")

	log.Printf("Build and integration tests succeeded\n")
}
