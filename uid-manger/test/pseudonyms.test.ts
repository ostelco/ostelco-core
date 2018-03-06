import * as Datastore from "@google-cloud/datastore";
import * as crypto from "crypto";
import * as startOfMonth from "date-fns/start_of_month";
import Emulator = require("google-datastore-emulator");
import * as pseudonyms from "../src/pseudonyms";

let emulator;
process.env.GCLOUD_PROJECT = "test";
process.env.DATASTORE_EMULATOR_HOST = "localhost:8081";

beforeAll(() => {
  const options = {
    port: 8081,
    useDocker: true // if you need docker image
  };
  emulator = new Emulator(options);
  return emulator.start();
});

afterAll(() => {
  return emulator.stop();
});

const msisdn = "4790300001";
const userId = "12-23-34-45";
const timestamp = Date.now();

it("Check hash of pseudonym", async () => {
  const datastore = new Datastore({});
  const hash = crypto.createHash("sha256");
  const start = startOfMonth(timestamp);
  const hashData: string = `${userId}--${start.getTime()}`;
  hash.update(hashData);
  const pseudonym = hash.digest("hex");

  const data = await pseudonyms.generatePseudonym(msisdn, userId, timestamp, datastore);
  expect(data).toEqual(pseudonym);
});

it("Get pseudonym for msisdn", async () => {
  const datastore = new Datastore({});
  const pseudonym = await pseudonyms.generatePseudonym(msisdn, userId, timestamp, datastore);
  const data = await pseudonyms.getPseudonym(msisdn, timestamp, datastore);
  expect(data).toEqual(pseudonym);
});

it("Get userId from pseudonym", async () => {
  const datastore = new Datastore({});
  const pseudonym = await pseudonyms.generatePseudonym(msisdn, userId, timestamp, datastore);
  const data = await pseudonyms.findUserId(pseudonym, datastore);
  expect(data).toEqual(userId);
});
