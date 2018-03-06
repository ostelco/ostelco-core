import * as Datastore from "@google-cloud/datastore";
import { DatastorePayload } from "@google-cloud/datastore/entity";
import * as crypto from "crypto";
import * as endOfMonth from "date-fns/end_of_month";
import * as startOfMonth from "date-fns/start_of_month";

export interface PseudonymObject {
  end: number;
  start: number;
  pseudonym: string;
  userId: string;
}
const pseudonymObjectTypeName = "PseudonymObject_test";

export function getKeyForPseudonymObject(msisdn: string, start: Date, datastore: Datastore) {
  const keyIdentifier: string = `${msisdn}--${start.getTime()}`;
  return datastore.key([pseudonymObjectTypeName, keyIdentifier]);
}

export function createPseudonymObject(
  msisdn: string,
  userId: string,
  timestamp: number
): PseudonymObject {
  const hash = crypto.createHash("sha256");
  const start = startOfMonth(timestamp);
  const end = endOfMonth(timestamp);
  const hashData: string = `${userId}--${start.getTime()}`;
  hash.update(hashData);
  const pseudonym = hash.digest("hex");
  const pseudonymObject: PseudonymObject = {
    end: end.getTime(),
    pseudonym,
    start: start.getTime(),
    userId
  };
  return pseudonymObject;
}

export async function generatePseudonym(
  msisdn: string,
  userId: string,
  timestamp: number,
  datastore: Datastore
) {
  const pseudonymObject = createPseudonymObject(msisdn, userId, timestamp);
  const pseudonymEntity = {
    data: pseudonymObject,
    key: getKeyForPseudonymObject(msisdn, startOfMonth(timestamp), datastore)
  };
  await datastore.upsert(pseudonymEntity);
  return pseudonymObject.pseudonym;
}

export function getPseudonym(
  msisdn: string,
  timestamp: number,
  datastore: Datastore
): Promise<string> {
  const start = startOfMonth(timestamp);
  return datastore.get(getKeyForPseudonymObject(msisdn, start, datastore)).then(data => {
    return (data[0] as PseudonymObject).pseudonym;
  });
}

export function findUserId(pseudonym: string, datastore: Datastore): Promise<string> {
  const query = datastore
    .createQuery(pseudonymObjectTypeName)
    .filter("pseudonym", "=", pseudonym)
    .limit(1);
  return query.run().then(data => {
    return (data[0][0] as PseudonymObject).userId;
  });
}

export class PseudonymAPIHandler {
  private datastore: Datastore;

  constructor(datastore) {
    this.datastore = datastore;
  }
}
