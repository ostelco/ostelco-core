// This is the entrypoint for cloud functions.
import * as Datastore from "@google-cloud/datastore";
import { PseudonymAPIHandler } from "./pseudonyms";
import { UserInfoAPIHandler } from "./userinfo";
import * as Utils from "./utils";

// Cold start initializers.
const datastoreClient = new Datastore({});
const userInfoApiHandler = new UserInfoAPIHandler(datastoreClient);
const pseudonymApiHandler = new PseudonymAPIHandler(datastoreClient);

exports.helloGET = (req, res) => {
  res.send("Hello World!");
};

exports.userIDGET = (req, res) => {
  const pathParams = Utils.splitPathComponents(req.params[0] as string);
  if (pathParams.length < 1) {
    console.log("Missing parametrs ");
    res.status(400).send("Missing parameters");
    return;
  }
  let msisdn: string = pathParams[0] as string;
  msisdn = Utils.isString(msisdn) ? msisdn : "";
  // Inject parameters like Express router
  req.params.msisdn = msisdn;
  userInfoApiHandler.rest_getUserIdforMsisdn(req, res);
};
