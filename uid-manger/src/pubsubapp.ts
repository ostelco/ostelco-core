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

  public onMessage = message => {
    const data = this.getMessageData(message);
    if (!data || !data.query) {
      console.log("Invalid data in Message = ", message);
      return;
    }
    switch (data.query) {
      case "newuser":
        this.createNewUser(data);
        break;
      case "user":
        this.getUserIdforMsisdn(data);
        break;
      case "msisdn":
        this.getUserIdforMsisdn(data);
        break;
      default:
        console.log("Unprocessed Request ", message);
    }
  };
}
