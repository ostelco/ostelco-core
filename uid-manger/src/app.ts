import * as Datastore from "@google-cloud/datastore";
import express = require("express");

import { APIHandler } from "./api";
import { createTokenObject } from "./tokens";

const datastoreClient = new Datastore({});

const apiHandler = new APIHandler(datastoreClient);
// Create Express server
const app = express();
app.set("port", process.env.PORT || 3000);
app.get("/newuser/:msisdn", (req, res) => {
  apiHandler.createNewUserId(req, res);
});
app.get("/user/:msisdn", (req, res) => {
  apiHandler.getUserIdforMsisdn(req, res);
});
app.get("/msisdn/:userId", (req, res) => {
  apiHandler.getMsisdnForUserId(req, res);
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
