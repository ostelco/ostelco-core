"use strict";
import * as Datastore from "@google-cloud/datastore";
import { Request, Response } from "express";
import * as uuidv4 from "uuid/v4";

export class APIHandler {
  private static isString(x) {
    return typeof x === "string";
  }
  private static isValidMsisdn(msisdn) {
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
  public async createNewUserId(request, response) {
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
  public async getUserId(request: Request, response: Response) {
    const msisdn = request.params.msisdn;
    if (APIHandler.isValidMsisdn(msisdn)) {
      try {
        const msisdnKey = this.datastore.key(["UserInfo_test", msisdn]);
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

  private getOrCreate(userEntity) {
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
  private async getUserEntity(msisdn, response) {
    const msisdnKey = this.datastore.key(["UserInfo_test", msisdn]);
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
  private getUserInfo(userKey) {
    return this.datastore.get(userKey).then(data => {
      const entities = data[0];
      return entities;
    });
  }
}
