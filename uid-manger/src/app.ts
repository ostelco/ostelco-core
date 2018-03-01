import * as Datastore from "@google-cloud/datastore";
import express = require("express");
import * as got from "got";

import { APIHandler } from "./api";

const METADATA_NETWORK_INTERFACE_URL =
  "http://metadata.google.internal/computeMetadata/v1/" +
  "instance/network-interfaces/0/access-configs/0/external-ip";

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

function getExternalIp() {
  const options = {
    headers: {
      "Metadata-Flavor": "Google"
    }
  };

  return got(METADATA_NETWORK_INTERFACE_URL, options)
    .then(response => response.body)
    .catch(err => {
      if (err || err.statusCode !== 200) {
        console.log(err);
        console.log(
          "Error while talking to metadata server, assuming localhost"
        );
        return Promise.resolve("localhost");
      }
      return Promise.reject(err);
    });
}

app.enable("trust proxy");
app.get("/", (req, res, next) => {
  getExternalIp()
    .then(externalIp => {
      res
        .status(200)
        .send(`External IP: ${externalIp}`)
        .end();
    })
    .catch(next);
});

export default app;
