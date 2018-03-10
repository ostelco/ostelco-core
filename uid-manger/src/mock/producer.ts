import { MessageProcessor } from "../message_processor";

export class MockProducer extends MessageProcessor {
  constructor() {
    super("api-response", "api-requests", "response-channel");
  }

  public createNewUser(msisdn) {
    const request = { params: { msisdn }, query: "newuser" };
    this.sendMessage({ request });
  }
  public async getUserIdforMsisdn(msisdn) {
    const request = { params: { msisdn }, query: "user" };
    this.sendMessage({ request });
  }
  public async getMsisdnForUserId(userId) {
    const request = { params: { userId }, query: "msisdn" };
    this.sendMessage({ request });
  }

  public onMessage = message => {
    const data = this.getMessageData(message);
    console.log("Response : ", JSON.stringify(data));
  };

  public generate() {
    this.createNewUser("4790300030");
    this.createNewUser("4790300031");
    this.createNewUser("4790300032");
    this.getUserIdforMsisdn("4790300031");
    this.getUserIdforMsisdn("4790300032");
  }

}
