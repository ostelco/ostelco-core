//usr/bin/env go run "$0" "$@"; exit "$?"

// EXPERIMENTAL:   This may or may not end up as production code.
// INTENT:         Replace the current build-all.sh script with a go program
//   	           that can be run from the command line as if it were a script
//		   The intent is straignt forward replacement, but with type
//                 safety and perhaps a bit of additional reliability.
//                 The build-all.sh script isn't used by a lot of people, so
//                 it's some distance "off broadway", and that may help the test
//                 very prestigious.  On the other hand, the script does contain
//                 sufficient complexity to be a worthy target

// Take a look here for inspiration:
//
//  func getCPUmodel() string {
//          cmd := "cat /proc/cpuinfo | egrep '^model name' | uniq | awk '{print substr($0, index($0,$4))}'"
//          out, err := exec.Command("bash","-c",cmd).Output()
//          if err != nil {
//                  return fmt.Sprintf("Failed to execute command: %s", cmd)
//          }
//          return string(out)
//  }
//
//  (from https://stackoverflow.com/questions/10781516/how-to-pipe-several-commands-in-go)
//

package main

import (
	"bytes"
	"crypto/md5"
	"encoding/hex"
	"fmt"
	"io"
	"log"
	"os"
	"os/exec"
)

func checkForDependencies() {
	log.Printf("Checking if dependencies are available\n")
	dependencies := [...]string{0: "docker-compose", 1: "./gradlew", 2: "docker", 3: "cmp"}
	for _, dep := range dependencies {
		// log.Printf("Checking dependency ('%s', '%s')", foo, dep)
		checkForDependency(dep)
	}
}

func checkForDependency(dependency string) {
	cmd := fmt.Sprintf("which %s", dependency)
	_, err := exec.Command("bash", "-c", cmd).Output()
	if err != nil {
		log.Fatalf("ERROR: Could not locate dependency '%s'", dependency)
		os.Exit(1)
	}
}

func checkThatEnvironmentVariableIsSet(key string) {
	if len(os.Getenv(key)) == 0 {
		log.Fatalf("ERROR: Environment variable not set'%s'", key)
		os.Exit(1)
	}
}

func bothFilesExistsButAreDifferent(s string, s2 string) bool {
	return fileExists(s) && fileExists(s2) && filesAreDifferent(s, s2)
}

const chunkSize = 64000

//
// True iff files are equal.
//
func deepCompare(file1, file2 string) bool {
	// Check file size ...

	f1, err := os.Open(file1)
	if err != nil {
		log.Fatal(err)
	}

	f2, err := os.Open(file2)
	if err != nil {
		log.Fatal(err)
	}

	for {
		b1 := make([]byte, chunkSize)
		_, err1 := f1.Read(b1)

		b2 := make([]byte, chunkSize)
		_, err2 := f2.Read(b2)

		if err1 != nil || err2 != nil {
			if err1 == io.EOF && err2 == io.EOF {
				return true
			} else if err1 == io.EOF || err2 == io.EOF {
				return false
			} else {
				log.Fatal(err1, err2)
			}
		}

		if !bytes.Equal(b1, b2) {
			return false
		}
	}
}

func filesAreDifferent(s string, s2 string) bool {
	return !deepCompare(s, s2)
}

func fileExists(path string) bool {
	_, err := os.Stat(path)
	return !os.IsNotExist(err)
}

func deleteFile(path string) {
	// delete file
	var err = os.Remove(path)
	if isError(err) {
		return
	}

	fmt.Println("==> done deleting file")
}

func isError(err error) bool {
	if err != nil {
		fmt.Println(err.Error())
	}

	return (err != nil)
}

func generateEspEndpointCertificates() {

	originalCertPath := "certs/ocs.dev.ostelco.org/nginx.crt"
	activeCertPath   := "ocsgw/cert/metrics.crt"

	if bothFilesExistsButAreDifferent(originalCertPath, activeCertPath) {
		deleteFile(originalCertPath)
		deleteFile(activeCertPath)
	}

	// If no original certificate (for whatever reason),
	// generate a new one.
	if !fileExists(originalCertPath) {
		generateNewCertificate(originalCertPath, "ocs.dev.ostelco.org")
	}

	if fileExists(activeCertPath) {
		copyFile(originalCertPath, activeCertPath)
	}
}

// XXX This does not work.
func copyFile(src string, dest string) {
	cp := fmt.Sprintf("cp %s %s", src, dest)
	_, err := exec.Command("bash", "-c", cp).Output()

	if err != nil {
		log.Fatalf("ERROR: Could not copy from '%s' to '%s'", src, dest)
		os.Exit(1)
	}
}

func generateNewCertificate(certificateFilename string, certificateDomain string) {
	cmd := fmt.Sprintf("scripts/generate-selfsigned-ssl-certs.sh %s", certificateDomain)
	out, err := exec.Command("bash", "-c", cmd).Output()
	if err != nil {
		log.Fatalf("ERROR: Could not generate self signed certificate for domain '%s'.\n      Reason: %s", certificateDomain, err)
		os.Exit(1)
	}

	log.Printf("out = %s", out)


	if !fileExists(certificateFilename) {
		log.Fatalf("ERROR: Did not generate self signed for domain '%s'", certificateDomain)
		os.Exit(1)
	}
}

func hash_file_md5(filePath string) (string, error) {
	//Initialize variable returnMD5String now in case an error has to be returned
	var returnMD5String string

	//Open the passed argument and check for any error
	file, err := os.Open(filePath)
	if err != nil {
		return returnMD5String, err
	}

	//Tell the program to call the following function when the current function returns
	defer file.Close()

	//Open a new hash interface to write to
	hash := md5.New()

	//Copy the file in the hash interface and check for any error
	if _, err := io.Copy(hash, file); err != nil {
		return returnMD5String, err
	}

	//Get the 16 bytes hash
	hashInBytes := hash.Sum(nil)[:16]

	//Convert the bytes to a string
	returnMD5String = hex.EncodeToString(hashInBytes)

	return returnMD5String, nil

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

	if !fileExists(serviceAccountJsonFilename) {
		log.Fatalf("ERROR: : Could not find master service-account file'%s'", serviceAccountJsonFilename)
		os.Exit(1)
	}

	rootMd5, err := hash_file_md5(serviceAccountJsonFilename)
	if err != nil {
		log.Fatalf("ERROR: Could not calculate md5 from file ", serviceAccountJsonFilename)
		os.Exit(1)
	}

	if serviceAccountMD5 != rootMd5 {
		log.Fatalf("MD5 of root service acccount file '%s' is not '%s', so bailing out", serviceAccountJsonFilename, serviceAccountMD5)
		os.Exit(1)
	}

	for _, dir := range dirsThatNeedsServiceAccountConfigs {
		currentFilename := fmt.Sprintf("%s/%s", dir, serviceAccountJsonFilename)

		if !fileExists(currentFilename) {
			copyFile(serviceAccountJsonFilename, currentFilename)
		} else {

			localMd5, err := hash_file_md5(serviceAccountJsonFilename)
			if err != nil {
				log.Fatalf("ERROR: Could not calculate md5 from file ", serviceAccountJsonFilename)
				os.Exit(1)
			}

			if localMd5 != rootMd5 {
				copyFile(serviceAccountJsonFilename, currentFilename)
			}
		}
	}
}

func main() {
	log.Printf("About to get started\n")
	checkForDependencies()
	checkThatEnvironmentVariableIsSet("STRIPE_API_KEY")
	generateEspEndpointCertificates()
	checkThatEnvironmentVariableIsSet("GCP_PROJECT_ID")

	distributeServiceAccountConfigs()

	// If not set, then just set a random value
	if len(os.Getenv("STRIPE_ENDPOINT_SECRET")) == 0 {
		log.Printf("Setting value of STRIPE_ENDPOINT_SECRET to 'thisIsARandomString'")
		os.Setenv("STRIPE_ENDPOINT_SECRET", "thisIsARandomString")
	}

	assertDockerIsRunning()
	log.Printf("Docker is running")
}

func assertDockerIsRunning() {
	cmd := "if [[ ! -z \"$( docker version | grep Version:)\" ]] ; echo 'Docker not running' ; fi"
	_, err := exec.Command("bash", "-c", cmd).Output()
	if err != nil {
		log.Fatalf("ERROR: Docker not running")
		os.Exit(1)
	}
}

// if [[ -z "$( docker version | grep Version:)" ]] ; then
//     echo "$0 INFO: Docker not running, please start it before trying again'"
//     exit 1
// fi
//
//
// #
// # Then start running the build
// #
//
// ./gradlew build
//
// #
// # If that didn't go too well, then bail out.
// #
//
// if [[ $? -ne 0 ]] ; then echo
//    echo "Compilation failed, aborting. Not running acceptance tests."
//    exit 1
// fi
//
// #
// # .... but it did go well, so we'll proceed to acceptance test
// #
//
// echo "$0 INFO: Building/unit tests went well, Proceeding to acceptance tests."
//
// docker-compose down
// docker-compose up --build --abort-on-container-exit
