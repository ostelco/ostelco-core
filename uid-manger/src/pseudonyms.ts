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
const tokenObjectTypeName = "TokenObject_test";

export function getKeyForTokenObject(msisdn: string, start: Date, datastore: Datastore) {
  const keyIdentifier: string = `${msisdn}--${start.getTime()}`;
  return datastore.key([tokenObjectTypeName, keyIdentifier]);
}

export function createTokenObject(msisdn: string, userId: string, timestamp: number): TokenObject {
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
  return tokenObject;
}

export async function generateToken(
  msisdn: string,
  userId: string,
  timestamp: number,
  datastore: Datastore
) {
  const tokenObject = createTokenObject(msisdn, userId, timestamp);
  const tokenEntity = {
    data: tokenObject,
    key: getKeyForTokenObject(msisdn, startOfMonth(timestamp), datastore)
  };
  await datastore.upsert(tokenEntity);
  return tokenObject.token;
}

export function getToken(msisdn: string, timestamp: number, datastore: Datastore): Promise<string> {
  const start = startOfMonth(timestamp);
  return datastore.get(getKeyForTokenObject(msisdn, start, datastore)).then(data => {
    return (data[0] as TokenObject).token;
  });
}

export function findUserId(token: string, datastore: Datastore): Promise<string> {
  const query = datastore
    .createQuery(tokenObjectTypeName)
    .filter("token", "=", token)
    .limit(1);
  return query.run().then(data => {
    return (data[0][0] as TokenObject).userId;
  });
}

export class TokenAPIHandler {
  private datastore: Datastore;

  constructor(datastore) {
    this.datastore = datastore;
  }
}
