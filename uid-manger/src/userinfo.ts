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

export class UserInfoAPIHandler {
  private datastore: Datastore;

  constructor(datastore) {
    this.datastore = datastore;
  }

  public async createNewUserId(request: Request, response: Response) {
    const msisdn = request.params.msisdn;
    if (Utils.isValidMsisdn(msisdn)) {
      try {
        const result = await createUserInfo(msisdn, this.datastore);
        response.send(JSON.stringify(result, undefined, 2));
      } catch (error) {
        response.status(500).send(error);
      }
    } else {
      response.status(400).send("Invalid parameter");
    }
  }

  public async getUserIdforMsisdn(request: Request, response: Response) {
    const msisdn = request.params.msisdn;
    if (Utils.isValidMsisdn(msisdn)) {
      try {
        const result = await userInfoForMsisdn(msisdn, this.datastore);
        if (!result) {
          response.status(404).send("User not found");
        } else {
          response.send(JSON.stringify(result, undefined, 2));
        }
      } catch (error) {
        response.status(500).send(error);
      }
    } else {
      response.status(400).send("Invalid parameter");
    }
  }

  public async getMsisdnForUserId(request: Request, response: Response) {
    const userId = request.params.userId;
    try {
      const result = await userInfoForUserId(userId, this.datastore);
      if (!result) {
        response.status(404).send("User not found");
      } else {
        response.send(JSON.stringify(result, undefined, 2));
      }
    } catch (error) {
      response.status(500).send(error);
    }
  }
}
