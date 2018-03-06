import * as Datastore from "@google-cloud/datastore";
import * as crypto from "crypto";
import * as startOfMonth from "date-fns/start_of_month";
import Emulator = require("google-datastore-emulator");
import * as tokens from "../src/tokens";

let emulator;
process.env.GCLOUD_PROJECT = "test";

beforeAll(() => {
  const options = {
    useDocker: true // if you need docker image
  };
  emulator = new Emulator(options);
  return emulator.start();
});

afterAll(() => {
  return emulator.stop();
});

const msisdn = "4792624130";
const userId = "12-23-34-45";
const timestamp = Date.now();

it("Check hash of token", async () => {
  const datastore = new Datastore({});
  const hash = crypto.createHash("sha256");
  const start = startOfMonth(timestamp);
  const hashData: string = `${userId}--${start.getTime()}`;
  hash.update(hashData);
  const token = hash.digest("hex");

  const data = await tokens.generateToken(msisdn, userId, timestamp, datastore);
  expect(data).toEqual(token);
});

it("Get token for msisdn", async () => {
  const datastore = new Datastore({});
  const token = await tokens.generateToken(msisdn, userId, timestamp, datastore);
  const data = await tokens.getToken(msisdn, timestamp, datastore);
  expect(data).toEqual(token);
});

it("Get userId from token", async () => {
  const datastore = new Datastore({});
  const token = await tokens.generateToken(msisdn, userId, timestamp, datastore);
  const data = await tokens.findUserId(token, datastore);
  expect(data).toEqual(userId);
});
