import * as Datastore from "@google-cloud/datastore";
import express = require("express");

import { PseudonymAPIHandler } from "./pseudonyms";
import { UserInfoAPIHandler } from "./userinfo";

const datastoreClient = new Datastore({});

const userInfoApiHandler = new UserInfoAPIHandler(datastoreClient);
const pseudonymApiHandler = new PseudonymAPIHandler(datastoreClient);
// Create Express server
const app = express();
app.set("port", process.env.PORT || 3000);
app.get("/newuser/:msisdn", (req, res) => {
  userInfoApiHandler.createNewUserId(req, res);
});
app.get("/user/:msisdn", (req, res) => {
  userInfoApiHandler.getUserIdforMsisdn(req, res);
});
app.get("/msisdn/:userId", (req, res) => {
  userInfoApiHandler.getMsisdnForUserId(req, res);
});

app.get("/newpseudonym/:msisdn/:userId/:timestamp", (req, res) => {
  pseudonymApiHandler.createNewPseudonym(req, res);
});
app.get("/pseudonym/:msisdn/:timestamp", (req, res) => {
  pseudonymApiHandler.getPseudonymForMsisdn(req, res);
});
app.get("/userid/:pseudonym", (req, res) => {
  pseudonymApiHandler.getUserIdforPseudonym(req, res);
});

// readiness_check request from App Engine
// configured in app.yaml
app.get("/readiness_check", (req, res) => {
  console.log("Health check for UID Manager");
  res.send(200);
});

app.enable("trust proxy");
app.get("/", (req, res, next) => {
  res
    .status(200)
    .send(`Nothing to see here !!!`)
    .end();
});

export default app;
