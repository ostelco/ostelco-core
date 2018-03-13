// This is the entrypoint for cloud functions.
import * as Datastore from "@google-cloud/datastore";
import { PseudonymAPIHandler } from "./pseudonyms";
import { UserInfoAPIHandler } from "./userinfo";
import * as Utils from "./utils";

// Cold start initializers.
const datastoreClient = new Datastore({});
const userInfoApiHandler = new UserInfoAPIHandler(datastoreClient);
const pseudonymApiHandler = new PseudonymAPIHandler(datastoreClient);

function validateParams(req, res, required) {
  const pathParams = Utils.splitPathComponents(req.params[0] as string);
  if (pathParams.length < required) {
    console.log("Missing parameters ", pathParams);
    res.status(400).send("Missing parameters");
    return undefined;
  }
  return pathParams;
}

exports.userIdGET = (req, res) => {
  const pathParams = validateParams(req, res, 1);
  if (!pathParams) {
    return;
  }
  const msisdn: string = pathParams[0] as string;
  // Inject parameters like Express router
  req.params.msisdn = msisdn;
  userInfoApiHandler.rest_getUserIdforMsisdn(req, res);
};

exports.msisdnGET = (req, res) => {
  const pathParams = validateParams(req, res, 1);
  if (!pathParams) {
    return;
  }
  const userId: string = pathParams[0] as string;
  // Inject parameters like Express router
  req.params.userId = userId;
  userInfoApiHandler.rest_getMsisdnForUserId(req, res);
};

exports.pseudonymGET = (req, res) => {
  const pathParams = validateParams(req, res, 2);
  if (!pathParams) {
    return;
  }
  const msisdn: string = pathParams[0] as string;
  const timestamp: string = pathParams[1] as string;
  // Inject parameters like Express router
  req.params.msisdn = msisdn;
  req.params.timestamp = timestamp;
  pseudonymApiHandler.rest_getPseudonymForMsisdn(req, res);
};

exports.userIdForPseudonymGET = (req, res) => {
  const pathParams = validateParams(req, res, 1);
  if (!pathParams) {
    return;
  }
  const pseudonym: string = pathParams[0] as string;
  // Inject parameters like Express router
  req.params.pseudonym = pseudonym;
  pseudonymApiHandler.rest_getUserIdforPseudonym(req, res);
};

// Deploy commands
// gcloud beta functions deploy userIdGET --trigger-http
// gcloud beta functions deploy msisdnGET --trigger-http
// gcloud beta functions deploy pseudonymGET --trigger-http
// gcloud beta functions deploy userIdForPseudonymGET --trigger-http
