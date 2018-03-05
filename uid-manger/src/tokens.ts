import * as Datastore from "@google-cloud/datastore";
import { DatastorePayload } from "@google-cloud/datastore/entity";
import * as crypto from "crypto";
import * as endOfMonth from "date-fns/end_of_month";
import * as startOfMonth from "date-fns/start_of_month";

export interface TokenObject {
  end: number;
  start: number;
  token: string;
  userId: string;
}

export async function createTokenObject(
  msisdn: string,
  userId: string,
  timestamp: number,
  datastore: Datastore
) {
  const hash = crypto.createHash("sha256");
  const start = startOfMonth(timestamp);
  const end = endOfMonth(timestamp);
  const hashData: string = `${userId}--${start.getTime()}`;
  hash.update(hashData);
  const token = hash.digest("hex");
  const tokenObject: TokenObject = {
    end: end.getTime(),
    start: start.getTime(),
    token,
    userId
  };
  const keyIdentifier: string = `${msisdn}--${start.getTime()}`;
  const tokenEntity = {
    data: tokenObject,
    key: datastore.key(["TokenObject", keyIdentifier])
  };
  await datastore.upsert(tokenEntity);
  return token;
}

export async function getToken(
  msisdn: string,
  timestamp: number,
  datastore: Datastore
) {
  const start = startOfMonth(timestamp);
  const keyIdentifier: string = `${msisdn}--${start.getTime()}`;
  const result = await datastore.get(datastore.key(["TokenObject", keyIdentifier]);
  return result[0][0];
}
