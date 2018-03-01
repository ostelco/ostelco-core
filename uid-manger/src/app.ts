import * as Datastore from "@google-cloud/datastore";
import express = require("express");
import { APIHandler } from "./api";

const datastoreClient = new Datastore({});

const apiHandler = new APIHandler(datastoreClient);
// Create Express server
const app = express();
app.set("port", process.env.PORT || 3000);
app.get("/newuser/:msisdn", (req, res) => {
  apiHandler.createNewUserId(req, res);
});
app.get("/user/:msisdn", (req, res) => {
  apiHandler.getUserId(req, res);
});

export default app;
