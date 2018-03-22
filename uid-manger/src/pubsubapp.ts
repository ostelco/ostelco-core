import * as Datastore from "@google-cloud/datastore";
import { MessageProcessor } from "./message_processor";
import { PseudonymAPIHandler } from "./pseudonyms";
import { UserInfoAPIHandler } from "./userinfo";

const datastoreClient = new Datastore({});

const userInfoApiHandler = new UserInfoAPIHandler(datastoreClient);
const pseudonymApiHandler = new PseudonymAPIHandler(datastoreClient);

export class RequestProcessor extends MessageProcessor {
  constructor() {
    super("api-requests", "api-responses", "request-channel");
  }

  public async createNewUser(request) {
    const msisdn = request.params.msisdn;
    const result = await userInfoApiHandler.createNewUserId(msisdn);
    this.sendMessage({ result, request });
  }
  public async getUserIdforMsisdn(request) {
    const msisdn = request.params.msisdn;
    const result = await userInfoApiHandler.getUserIdforMsisdn(msisdn);
    this.sendMessage({ result, request });
  }
  public async getMsisdnForUserId(request) {
    const userId = request.params.userId;
    const result = await userInfoApiHandler.getMsisdnForUserId(userId);
    this.sendMessage({ result, request });
  }

  public async createNewPseudonym(request) {
    const msisdn = request.params.msisdn;
    const userId = request.params.userId;
    const timestamp = request.params.timestamp;
    const result = await pseudonymApiHandler.createNewPseudonym(msisdn, userId, timestamp);
    this.sendMessage({ result, request });
  }

  public async getPseudonymForMsisdn(request) {
    const msisdn = request.params.msisdn;
    const timestamp = request.params.timestamp;
    const result = await pseudonymApiHandler.getPseudonymForMsisdn(msisdn, timestamp);
    this.sendMessage({ result, request });
  }

  public async getUserIdforPseudonym(request) {
    const pseudonym = request.params.pseudonym;
    const result = await pseudonymApiHandler.getUserIdforPseudonym(pseudonym);
    this.sendMessage({ result, request });
  }

  public onMessage = message => {
    console.log("Processing Message ", message.id);
    const data = this.getMessageData(message);
    if (!data || !data.request || !data.request.query) {
      console.log("Invalid data in Message = ", message);
      return;
    }
    const request = { ...data.request, id: message.id };
    switch (request.query) {
      case "newuser":
        this.createNewUser(request);
        break;
      case "msisdn<-userid":
        this.getMsisdnForUserId(request);
        break;
      case "userid<-msisdn":
        this.getUserIdforMsisdn(request);
        break;
      case "newpseudonym":
        this.createNewPseudonym(request);
        break;
      case "pseudonym<-msisdn":
        this.getPseudonymForMsisdn(request);
        break;
      case "userid<-pseudonym":
        this.getUserIdforPseudonym(request);
        break;
      default:
        console.log("Unprocessed Request ", message);
    }
    console.log("Acknowledge message ", message.id);
    message.ack();
  };
}
