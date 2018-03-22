import * as Datastore from "@google-cloud/datastore";
import express = require("express");
import { Request, Response } from "express";
import * as rp from "request-promise-native";

import { MockProducer } from "./mock/producer";
import { PseudonymAPIHandler } from "./pseudonyms";
import { RequestProcessor } from "./pubsubapp";
import { UserInfoAPIHandler } from "./userinfo";

const datastoreClient = new Datastore({});

const userInfoApiHandler = new UserInfoAPIHandler(datastoreClient);
const pseudonymApiHandler = new PseudonymAPIHandler(datastoreClient);
let pubsubRequestProcessor: RequestProcessor;
let mockProducer: MockProducer;

const startMode = process.env.START_MODE || "default";

// Create Express server
const app = express();
app.set("port", process.env.PORT || 3000);
if (startMode === "default") {
  console.log("Starting default app server");
} else if (startMode === "uid-manager") {
  console.log("Starting UID manager");
  pubsubRequestProcessor = new RequestProcessor();
  pubsubRequestProcessor.createSubscription();

  app.get("/newuser/:msisdn", (req, res) => {
    userInfoApiHandler.rest_createNewUserId(req, res);
  });
  app.get("/user/:msisdn", (req, res) => {
    userInfoApiHandler.rest_getUserIdforMsisdn(req, res);
  });
  app.get("/msisdn/:userId", (req, res) => {
    userInfoApiHandler.rest_getMsisdnForUserId(req, res);
  });

  app.get("/newpseudonym/:msisdn/:userId/:timestamp", (req, res) => {
    pseudonymApiHandler.rest_createNewPseudonym(req, res);
  });
  app.get("/pseudonym/:msisdn/:timestamp", (req, res) => {
    pseudonymApiHandler.rest_getPseudonymForMsisdn(req, res);
  });
  app.get("/userid/:pseudonym", (req, res) => {
    pseudonymApiHandler.rest_getUserIdforPseudonym(req, res);
  });
} else if (startMode === "generator") {
  mockProducer = new MockProducer();
  mockProducer.createSubscription();
  app.get("/gen-users", (req, res) => {
    mockProducer.generate();
    res.status(200).send("Generating fake requests");
  });
  app.get("/gen-pseudonym", (req, res) => {
    mockProducer.generate_pseudonyms();
    res.status(200).send("Generating fake pseudonyms");
  });
  app.get("/test-pseudonym", (req, res) => {
    mockProducer.test_pseudonyms();
    res.status(200).send("Testing fake pseudonyms");
  });
}
async function testRequest(request: Request, response: Response) {
  try {
    const result = await rp("https://uid-manager-dot-pantel-2decb.appspot.com/");
    response.send("Request Success");
  } catch (error) {
    response.status(500).send(error);
  }
}

app.get("/testRequest", (req, res) => {
  testRequest(req, res);
});

// readiness_check request from App Engine
// configured in app.yaml
app.get("/readiness_check", (req, res) => {
  // console.log("Health check for UID Manager");
  res.send(200);
});

app.enable("trust proxy");
app.get("/", (req, res, next) => {
  console.log(JSON.stringify(req.headers));
  res
    .status(200)
    .send(`Nothing to see here !!!`)
    .end();
});

export default app;
