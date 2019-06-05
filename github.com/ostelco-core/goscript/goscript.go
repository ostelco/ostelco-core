package goscript

import (
	"bufio"
	"bytes"
	"crypto/md5"
	"encoding/hex"
	"fmt"
	"io"
	"log"
	"os"
	"os/exec"
)

// XXX This package contains a lot of different things, that is
//     a discouraged practice: Packages should be small, and only
//     do one  thing,  which they do  well.  Unix philosophy.
//     This packa oes not follow unix philosophy. Feel free to refactor
//     until it does.

//  Read something via a scanner, then send it to stdout.
//  to every line read, prepend the name of the stream
//  that has been read.
func relayScanToStdout(nameOfStream string, scanner *bufio.Scanner) {
	for scanner.Scan() {
		fmt.Printf("%10s:  %s\n", nameOfStream, scanner.Text())
	}
	// XXX Don't know if we should do this. Probably shouldn't
	// if err := scanner.Err(); err != nil {
	//	fmt.Fprintln(os.Stderr, "reading %s : %s", nameOfStream, err)
	//}
}

func AssertSuccesfulRun(cmdTxt string) {
	err := runCmdWithPiping(cmdTxt)
	if err != nil {
		log.Fatalf("Could not successfully run command '%s': %s", cmdTxt, err)
	}
}

func runCmdWithPiping(cmdTxt string) (result error) {

	// Declare the  cmd
	cmd := exec.Command("bash", "-c", cmdTxt)

	// Get the stdout
	out, err := cmd.StdoutPipe()
	if err != nil {
		return err
	}

	// ... then the stderr
	stdErr, err := cmd.StderrPipe()
	if err != nil {
		return err
	}

	// Run the command
	err = cmd.Start()
	if err != nil {
		return err
	}

	// ... but  add these scanners to the output from it
	stdoutScanner := bufio.NewScanner(out)
	stderrScanner := bufio.NewScanner(stdErr)

	//  ... and  set up
	//  up the goroutine infrastructure to intercept stdout/stderr.  It works,  but
	//  I don't quit understand why.
	go relayScanToStdout("stdout", stdoutScanner)
	go relayScanToStdout("stderr", stderrScanner)

	// When exiting the function, run the defered  function that
	// gets the return value from the  cmd, and return that
	// as the return value for this function.
	defer func() {
		result = cmd.Wait()
	}()
	return nil
}

func Hash_file_md5(filePath string) (string, error) {
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

// Copy the src file to dst. Any existing file will be overwritten and will not
// copy file attributes.
func CopyFile(src, dst string) error {
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()

	out, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, in)
	if err != nil {
		return err
	}
	return out.Close()
}

func CheckForDependencies(dependencies ...string) {
	log.Printf("Checking if dependencies are available\n")
	for _, dep := range dependencies {
		// log.Printf("Checking dependency ('%s', '%s')", foo, dep)
		checkForDependency(dep)
	}
}

func checkForDependency(dependency string) {
	cmd := fmt.Sprintf("which %s", dependency)
	_, err := exec.Command("bash", "-c", cmd).Output()
	if err != nil {
		log.Fatalf("Could not locate dependency '%s'", dependency)
	}
}

func CheckThatEnvironmentVariableIsSet(key string) {
	if len(os.Getenv(key)) == 0 {
		log.Fatalf("Environment variable not set'%s'", key)
	}
}

func BothFilesExistsButAreDifferent(path1 string, path2 string) bool {
	return FileExists(path1) && FileExists(path2) && filesAreDifferent(path1, path2)
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

func FileExists(path string) bool {
	_, err := os.Stat(path)
	return !os.IsNotExist(err)
}

func DeleteFile(path string) {
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
