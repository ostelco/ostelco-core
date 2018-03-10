import * as Datastore from "@google-cloud/datastore";
import { DatastorePayload } from "@google-cloud/datastore/entity";
import * as crypto from "crypto";
import * as endOfMonth from "date-fns/end_of_month";
import * as startOfMonth from "date-fns/start_of_month";
import { Request, Response } from "express";
import { isNumber } from "util";
import { ResultObject } from "./userinfo";

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

  public setResponse(result: ResultObject, response: Response) {
    response.status(result.status).send(result.response);
  }

  public async createNewPseudonym(msisdn: string, userId: string, timestampStr: string) {
    let result: ResultObject = { status: 500, response: undefined };
    const timestamp = Number.parseInt(timestampStr);
    if (msisdn && userId && !Number.isNaN(timestamp)) {
      try {
        const data = await generatePseudonym(msisdn, userId, timestamp, this.datastore);
        result = !data
          ? { status: 404, response: `Can't generate pseudonym for ${msisdn} at ${timestamp}` }
          : { status: 200, response: JSON.stringify(data, undefined, 2) };
      } catch (error) {
        result = {
          response: `Can't generate pseudonym for ${msisdn} at ${timestamp}`,
          status: 404
        };
      }
    } else {
      result = {
        response: `Invalid parameters ${msisdn} ${userId} ${timestamp}`,
        status: 400
      };
    }
    return result;
  }

  public async rest_createNewPseudonym(request: Request, response: Response) {
    const msisdn = request.params.msisdn;
    const userId = request.params.userId;
    const timestamp = request.params.timestamp;
    const result = await this.createNewPseudonym(msisdn, userId, timestamp);
    this.setResponse(result, response);
  }

  public async getPseudonymForMsisdn(msisdn: string, timestampStr: string) {
    let result: ResultObject = { status: 500, response: undefined };
    const timestamp = Number.parseInt(timestampStr);
    if (msisdn && !Number.isNaN(timestamp)) {
      try {
        const data = await getPseudonym(msisdn, timestamp, this.datastore);
        result = !data
          ? { status: 404, response: `Can't find pseudonym for ${msisdn} at ${timestamp}` }
          : { status: 200, response: JSON.stringify(data, undefined, 2) };
      } catch (error) {
        result = {
          response: `Can't find pseudonym for ${msisdn} at ${timestamp}`,
          status: 404
        };
      }
    } else {
      result = {
        response: `Invalid parameters ${msisdn}  ${timestamp}`,
        status: 400
      };
    }
    return result;
  }

  public async rest_getPseudonymForMsisdn(request: Request, response: Response) {
    const msisdn = request.params.msisdn;
    const timestamp = request.params.timestamp;
    const result = await this.getPseudonymForMsisdn(msisdn, timestamp);
    this.setResponse(result, response);
  }

  public async getUserIdforPseudonym(pseudonym: string) {
    let result: ResultObject = { status: 500, response: undefined };
    if (pseudonym) {
      try {
        const data = await findUserId(pseudonym, this.datastore);
        result = !data
          ? { status: 404, response: `User not found for ${pseudonym}` }
          : { status: 200, response: JSON.stringify(data, undefined, 2) };
      } catch (error) {
        result = { status: 500, response: error };
      }
    } else {
      result = { status: 400, response: "Invalid parameter" };
    }
    return result;
  }

  public async rest_getUserIdforPseudonym(request: Request, response: Response) {
    const pseudonym = request.params.pseudonym;
    const result = await this.getUserIdforPseudonym(pseudonym);
    this.setResponse(result, response);
  }
}
