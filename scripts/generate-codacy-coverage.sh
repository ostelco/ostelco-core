#!/bin/bash

# This script finds all instances of jacocoTestReport.xml and generates a code coverage report 
# and uploads it to Codacy wherever that file is found. This script needs a set of environment 
# variables which are defined in the pipeline config (.circleci/config.yml).

#### sanity checks 
if [ -z "${CODACY_JAR_FILE}" ]; then
 echo "ERROR: CODACY_JAR_FILE is not set in the environment! Aborting!"
  exit 1
fi  

if [ -z "${CODACY_MODULE}" ]; then
  echo "ERROR: CODACY_MODULE is not set in the environment! Aborting!"
  exit 1
fi  
####

REPORT_TARGETS=$(find . -name jacocoTestReport.xml)
            
if [ -n "${REPORT_TARGETS}" ]; then
    echo "Found 'jacocoTestReport.xml' file under 'build' directories in the following modules,"
    echo ", implying - tests were run for them."
    echo "${REPORT_TARGETS}" ; echo 
else
    echo "There were no 'build' directories found under each module." 
    echo "This means tests were not run in the previous build job."
fi

for REPORT_TARGET in ${REPORT_TARGETS}; do
    echo "======> Processing code-coverage report for ======> ${REPORT_TARGET} <======"
    java -cp ~/${CODACY_JAR_FILE} ${CODACY_MODULE} report -l Java -r ${REPORT_TARGET} --partial
done

if [ -n "${REPORT_TARGETS}" ]; then
    echo "======> Uploading final code-coverage report to CODACY website. <======"
    java -cp ~/${CODACY_JAR_FILE} ${CODACY_MODULE} final
else
    echo "There were no 'jacocoTestReport.xml' files found under 'build' directories in each module." 
    echo "This means tests were not run in the previous build job."
    echo "... so, not uploading any code-coverage reports to CODACY website. "
fi
