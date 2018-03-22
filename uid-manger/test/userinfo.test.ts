import * as Datastore from "@google-cloud/datastore";
import Emulator = require("google-datastore-emulator");
import * as userinfo from "../src/userinfo";

let emulator;
process.env.GCLOUD_PROJECT = "test";
process.env.DATASTORE_EMULATOR_HOST = "localhost:8082";

beforeAll(() => {
  const options = {
    port: 8082,
    useDocker: true // if you need docker image
  };
  emulator = new Emulator(options);
  return emulator.start();
});

afterAll(() => {
  return emulator.stop();
});
const msisdn = "4790300001";

it("Create UserInfo for msisdn", async () => {
  const datastore = new Datastore({});
  const data = await userinfo.createUserInfo(msisdn, datastore);
  expect(data.msisdn).toEqual(msisdn);
});

it("Get UserInfo for msisdn", async () => {
  const datastore = new Datastore({});
  const userInfo = await userinfo.createUserInfo(msisdn, datastore);
  const data = await userinfo.userInfoForMsisdn(msisdn, datastore);
  expect(data.userId).toEqual(userInfo.userId);
});

it("Get msisdn for userid", async () => {
  const datastore = new Datastore({});
  const userInfo = await userinfo.createUserInfo(msisdn, datastore);
  const data = await userinfo.userInfoForUserId(userInfo.userId, datastore);
  expect(data.msisdn).toEqual(userInfo.msisdn);
});
