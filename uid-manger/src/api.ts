"use strict";
import * as Datastore from "@google-cloud/datastore";
import { DatastoreKey } from "@google-cloud/datastore/entity";
import { Request, Response } from "express";
import * as uuidv4 from "uuid/v4";

const userInfoTypeName = "UserInfo_test";

export interface UserInfo {
  msisdn: string;
  userId: string;
}
export interface UserInfoEntity {
  data: UserInfo;
  key: DatastoreKey;
}

export class APIHandler {
  private static isString(x) {
    return typeof x === "string";
  }
  private static isValidMsisdn(msisdn: string) {
    if (!APIHandler.isString(msisdn)) {
      return false;
    }
    const msisdnPattern = /^[1-9]\d*$/;
    return msisdnPattern.test(msisdn);
  }

  private datastore: Datastore;
  constructor(datastore) {
    this.datastore = datastore;
  }
  public async createNewUserId(request: Request, response: Response) {
    const msisdn = request.params.msisdn;
    if (APIHandler.isValidMsisdn(msisdn)) {
      try {
        await this.getUserEntity(msisdn, response);
      } catch (error) {
        response.status(500).send(error);
      }
    } else {
      response.status(400).send("Invalid parameter");
    }
  }
  public async getUserIdforMsisdn(request: Request, response: Response) {
    const msisdn = request.params.msisdn;
    if (APIHandler.isValidMsisdn(msisdn)) {
      try {
        const msisdnKey = this.datastore.key([userInfoTypeName, msisdn]);
        const result = await this.getUserInfo(msisdnKey);
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
      const result = await this.findMsisdn(userId);
      if (!result) {
        response.status(404).send("User not found");
      } else {
        response.send(JSON.stringify(result, undefined, 2));
      }
    } catch (error) {
      response.status(500).send(error);
    }
  }

  private getOrCreate(userEntity: UserInfoEntity) {
    const transaction = this.datastore.transaction();
    return transaction
      .run()
      .then(() => transaction.get(userEntity.key))
      .then(results => {
        const user = results[0];
        if (user) {
          // The UserInfo entity already exists.
          transaction.rollback();
          return user;
        } else {
          // Create the UserInfo entity.
          transaction.save(userEntity);
          transaction.commit();
          return userEntity.data;
        }
      })
      .then(result => result)
      .catch(() => transaction.rollback());
  }
  private async getUserEntity(msisdn: string, response: Response) {
    const msisdnKey = this.datastore.key([userInfoTypeName, msisdn]);
    const newUserId = uuidv4();
    const entity = {
      data: {
        msisdn,
        userId: newUserId
      },
      key: msisdnKey
    };
    const result = await this.getOrCreate(entity);
    response.send(JSON.stringify(result, undefined, 2));
  }
  private getUserInfo(userKey: DatastoreKey) {
    return this.datastore.get(userKey).then(data => {
      const entities = data[0];
      return entities;
    });
  }
  private findMsisdn(userId: string) {
    const query = this.datastore
      .createQuery(userInfoTypeName)
      .filter("userId", "=", userId)
      .limit(1);
    return query.run().then(data => {
      return data[0][0];
    });
  }
}
