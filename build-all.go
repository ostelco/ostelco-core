//usr/bin/env go run "$0" "$@"; exit "$?"

// INTENT:         Replace the current build-all.sh script with a go program
//   	           that can be run from the command line as if it were a script

package main

import (
	"github.com/ostelco/ostelco-core/github.com/ostelco-core/goscript"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"os/signal"
)

func generateEspEndpointCertificates(originalCertPath string, activeCertPath string, domainName string) {
	if goscript.BothFilesExistsAndAreDifferent(originalCertPath, activeCertPath) {
		goscript.DeleteFile(originalCertPath)
		goscript.DeleteFile(activeCertPath)
	}
	// If no original certificate (for whatever reason),
	// generate a new one.
	if !goscript.FileExists(originalCertPath) {
		generateNewCertificate(originalCertPath, domainName)
	}
	if goscript.FileExists(activeCertPath) {
		_ = goscript.CopyFile(originalCertPath, activeCertPath)
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
	log.Printf("Distributing service account configs ...\n")
	if !goscript.FileExists(serviceAccountJsonFilename) {
		log.Fatalf("ERROR: : Could not find master service-account file'%s'", serviceAccountJsonFilename)
	}
	rootMd5, err := goscript.HashFileMd5(serviceAccountJsonFilename)
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
			localMd5, err := goscript.HashFileMd5(serviceAccountJsonFilename)
			if err != nil {
				log.Fatalf("ERROR: Could not calculate md5 from file '%s'", serviceAccountJsonFilename)
			}

			if localMd5 != rootMd5 {
				_ = goscript.CopyFile(serviceAccountJsonFilename, currentFilename)
			}
		} else {
			_ = goscript.CopyFile(serviceAccountJsonFilename, currentFilename)
		}
	}
	log.Printf("    Done\n")
}

func generateDummyStripeEndpointSecretIfNotSet() {
	// If the stripe endpoint secret is not set, then
	// then just set a random value.  It's not important right now,
	// but it may cause build failure, so we set it to something.
	if len(os.Getenv("STRIPE_ENDPOINT_SECRET")) == 0 {
		log.Printf("Setting value of STRIPE_ENDPOINT_SECRET to 'thisIsARandomString'")
		_ = os.Setenv("STRIPE_ENDPOINT_SECRET", "thisIsARandomString")
	}
}

func main() {

	cleanPtr := flag.Bool("clean", false, "If set, run a './gradlew clean' and 'go clean' before building and testing.")
	stayUpPtr := flag.Bool("stay-up", false, "If set, keep test environment up in docker after running tests.")
	doJvmPtr := flag.Bool("build-jvm-components", true, "If set, then compile and test JVM based components.")
	doGoPtr := flag.Bool("build-golang-components", true, "If set, then compile and test GO based components.")

	flag.Parse()

	log.Printf("About to get started\n")
	if *cleanPtr {
		log.Printf("    ... will clean.")
	}

	if *stayUpPtr {
		log.Printf("    ... will keep environment up after acceptance tests have run (if any).")
	}

	log.Printf("Starting building.")


	if !*doGoPtr {
		log.Printf("    ...  Not building/testing GO code")
	} else {
		log.Printf("    ...  Building of GO code not implemented yet.")
	}

	if !*doJvmPtr {
		log.Printf("    ...  Not building/testing JVM based code.")

	} else {
		//
		// Ensure that  all preconditions for building and testing are met, if not
		// fail and terminate execution.
		//

		goscript.AssertThatScriptCommandsAreAvailable("docker-compose", "./gradlew", "docker", "cmp")

		projectProfile := parseServiceAccountFile("prime-service-account.json")

		gcpProjectId := projectProfile.ProjectId

		os.Setenv("GCP_PROJECT_ID", gcpProjectId)

		goscript.AssertThatEnvironmentVariableaAreSet("STRIPE_API_KEY", "GCP_PROJECT_ID")
		goscript.AssertDockerIsRunning()
		generateDummyStripeEndpointSecretIfNotSet()
		distributeServiceAccountConfigs(
			"prime-service-account.json",
			"c54b903790340dd9365fa59fce3ad8e2",
			"acceptance-tests/config",
			"dataflow-pipelines/config",
			"ocsgw/config",
			"auth-server/config prime/config")
		generateEspEndpointCertificates(
			"certs/ocs.dev.ostelco.org/nginx.crt",
			"ocsgw/cert/metrics.crt",
			"ocs.dev.ostelco.org")

		buildUsingGradlew(cleanPtr)

		runIntegrationTestsViaDocker(stayUpPtr)

		log.Printf("Build and integration tests succeeded\n")
	}
}

type GcpProjectProfile struct {
	Type                    string `json:"type"`
	ProjectId               string `json:"project_id"`
	PrivateKeyId            string `json:"private_key_id"`
	PrivateKey              string `json:"private_key"`
	ClientEmail             string `json:"client_email"`
	ClientId                string `json:"client_id"`
	AuthUri                 string `json:"auth_uri"`
	TokenUri                string `json:"token_uri"`
	AuthProviderX509CertUrl string `json:"auth_provider_x509_cert_url"`
	ClientX509CertUrl       string `json:"client_x509_cert_url"`
}

func parseServiceAccountFile(filename string) (GcpProjectProfile) {
	// Open our jsonFile
	jsonFile, err := os.Open(filename)
	// if we os.Open returns an error then handle it
	if err != nil {
		log.Fatalln(err)
	}
	fmt.Println("Successfully Opened ", filename)
	byteValue, _ := ioutil.ReadAll(jsonFile)
	// defer the closing of our jsonFile so that we can parse it later on
	defer jsonFile.Close()

	var profile GcpProjectProfile

	// we unmarshal our byteArray which contains our
	// jsonFile's content into 'users' which we defined above
	json.Unmarshal(byteValue, &profile)

	return profile
}

func runIntegrationTestsViaDocker(stayUpPtr *bool) {

	// First take down any lingering docker jobs that may interfer with
	// the current run.
	goscript.AssertSuccesfulRun("docker-compose down")

	if *stayUpPtr {
		// If  ctrl-c is pushed during stay-up, then take the whole thing down using docker-compose down
		c := make(chan os.Signal, 1)
		signal.Notify(c, os.Interrupt)
		go func() {
			<-c
			goscript.AssertSuccesfulRun("docker-compose down")
			os.Exit(1)
		}()
		goscript.AssertSuccesfulRun("docker-compose up --build")
	} else {
		goscript.AssertSuccesfulRun("docker-compose up --build --abort-on-container-exit")
	}
}

func buildUsingGradlew(cleanPtr *bool) {
	//
	// All preconditions are now satisfied, now run the actual build/test commands
	// and terminate the build process if any of them fails.
	//
	if *cleanPtr {
		goscript.AssertSuccesfulRun("./gradlew build")
	}
	goscript.AssertSuccesfulRun("./gradlew build")
}
