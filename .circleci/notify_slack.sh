#!/bin/bash

# This script sends a custom message as a custom slack notification.
# It is used in the pipeline to notify on failures or successes.

#### sanity check
if [ -z $1 ] || [ -z $2 ]; then
   echo "ERROR: missing input parameters to send slack notifications. Aborting!"
   exit 1
fi
if [ -z ${SLACK_WEBHOOK} ]; then
   echo "WARN: SLACK_WEBHOOK environemnt variable is not set. Slack notifications are skipped."
   exit 0
fi   
####

#### input 
app="PRIME" # app name 
pipeline=$1 # pipeline name
success=$2 # true or false
####

if [ "$success" = true ] ; then
    color="#36a64f"
    button_style="primary"
    status="succeeded"
else
   color="#FF0000"
   button_style="danger"
   status="failed"
fi

echo "sending slack notification ..."
curl -X POST -H 'Content-type: application/json' \
--data '{"attachments": [{"fallback": "'${app}' Build result","color": "'${color}'" ,"pretext": "'${app}' '${pipeline}' pipeline:","title": "Job: ['${CIRCLE_JOB}'] for ('${app}') '${status}'. Check circleci for details.","actions": [{"type": "button","name": "check_workflow","text": "Check Workflow","url": "https://circleci.com/workflow-run/'${CIRCLE_WORKFLOW_ID}'","style": "'${button_style}'"},{"type": "button","name": "check_job","text": "Check Job","url": "https://circleci.com/gh/ostelco/ostelco-core/'${CIRCLE_BUILD_NUM}'","style": "'${button_style}'"}]}] }' ${SLACK_WEBHOOK}

if [ $? != 0 ]; then
  echo "Failed to send a slack notification!"
  exit 1
fi