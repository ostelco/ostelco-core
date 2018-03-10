"use strict";
import * as Datastore from "@google-cloud/datastore";
import { DatastoreKey } from "@google-cloud/datastore/entity";
import { Request, Response } from "express";
import * as uuidv4 from "uuid/v4";
import * as Utils from "./utils";

const userInfoTypeName = "UserInfo_test";

export interface UserInfo {
  msisdn: string;
  userId: string;
}
export interface UserInfoEntity {
  data: UserInfo;
  key: DatastoreKey;
}

function fetchOrInsertUserInfo(userEntity: UserInfoEntity, datastore: Datastore) {
  const transaction = datastore.transaction();
  return transaction
    .run()
    .then(() => transaction.get(userEntity.key))
    .then(async results => {
      const user = results[0];
      if (user) {
        // The UserInfo entity already exists.
        transaction.rollback();
        return user;
      } else {
        // Create the UserInfo entity.
        transaction.save(userEntity);
        await transaction.commit();
        return userEntity.data;
      }
    })
    .then(result => result as UserInfo)
    .catch(() => transaction.rollback());
}

function msisdnKey(msisdn: string, datastore: Datastore): DatastoreKey {
  return datastore.key([userInfoTypeName, msisdn]);
}

export async function createUserInfo(msisdn: string, datastore: Datastore) {
  const newUserId = uuidv4();
  const entity = {
    data: {
      msisdn,
      userId: newUserId
    },
    key: msisdnKey(msisdn, datastore)
  };
  const result = await fetchOrInsertUserInfo(entity, datastore);
  return result as UserInfo;
}

export function userInfoForMsisdn(msisdn: string, datastore: Datastore) {
  return datastore.get(msisdnKey(msisdn, datastore)).then(data => {
    const entities = data[0];
    return entities as UserInfo;
  });
}

export function userInfoForUserId(userId: string, datastore: Datastore) {
  const query = datastore
    .createQuery(userInfoTypeName)
    .filter("userId", "=", userId)
    .limit(1);
  return query.run().then(data => {
    return data[0][0] as UserInfo;
  });
}

export interface ResultObject {
  status: number;
  response: string;
}

export class UserInfoAPIHandler {
  private datastore: Datastore;

  constructor(datastore) {
    this.datastore = datastore;
  }

  public setResponse(result: ResultObject, response: Response) {
    response.status(result.status).send(result.response);
  }

  public async createNewUserId(msisdn: string) {
    let result: ResultObject = { status: 500, response: undefined };
    if (Utils.isValidMsisdn(msisdn)) {
      try {
        const data = await createUserInfo(msisdn, this.datastore);
        result = { status: 200, response: JSON.stringify(data, undefined, 2) };
      } catch (error) {
        result = { status: 500, response: error };
      }
    } else {
      result = { status: 400, response: "Invalid parameter" };
    }
    return result;
  }

  public async rest_createNewUserId(request: Request, response: Response) {
    const msisdn = request.params.msisdn;
    const result = await this.createNewUserId(msisdn);
    this.setResponse(result, response);
  }

  public async getUserIdforMsisdn(msisdn: string) {
    let result: ResultObject = { status: 500, response: undefined };
    if (Utils.isValidMsisdn(msisdn)) {
      try {
        const data = await userInfoForMsisdn(msisdn, this.datastore);
        result = !data
          ? { status: 404, response: "User not found" }
          : { status: 200, response: JSON.stringify(data, undefined, 2) };
      } catch (error) {
        result = { status: 500, response: error };
      }
    } else {
      result = { status: 400, response: "Invalid parameter" };
    }
    return result;
  }

  public async rest_getUserIdforMsisdn(request: Request, response: Response) {
    const msisdn = request.params.msisdn;
    const result = await this.getUserIdforMsisdn(msisdn);
    this.setResponse(result, response);
  }

  public async getMsisdnForUserId(userId: string) {
    let result: ResultObject = { status: 500, response: undefined };

    try {
      const data = await userInfoForUserId(userId, this.datastore);
      result = !data
        ? { status: 404, response: "User not found" }
        : { status: 200, response: JSON.stringify(data, undefined, 2) };
    } catch (error) {
      result = { status: 500, response: error };
    }
    return result;
  }

  public async rest_getMsisdnForUserId(request: Request, response: Response) {
    const userId = request.params.userId;
    const result = await this.getMsisdnForUserId(userId);
    this.setResponse(result, response);
  }
}
