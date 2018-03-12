import { MessageProcessor } from "../message_processor";

export class MockProducer extends MessageProcessor {
  constructor() {
    super("api-responses", "api-requests", "response-channel");
  }

  public createNewUser(msisdn) {
    const request = { params: { msisdn }, query: "newuser" };
    this.sendMessage({ request });
  }
  public async getUserIdforMsisdn(msisdn) {
    const request = { params: { msisdn }, query: "userid<-msisdn" };
    this.sendMessage({ request });
  }
  public async getMsisdnForUserId(userId) {
    const request = { params: { userId }, query: "msisdn<-userid" };
    this.sendMessage({ request });
  }
  public async createNewPseudonym(msisdn: string, userId: string) {
    const request = { params: { msisdn, userId, timestamp: Date.now() }, query: "newpseudonym" };
    this.sendMessage({ request });
  }
  public async getPseudonymForMsisdn(msisdn: string) {
    const request = { params: { msisdn, timestamp: Date.now() }, query: "pseudonym<-msisdn" };
    this.sendMessage({ request });
  }
  public async getUserIdforPseudonym(pseudonym: string) {
    const request = { params: { pseudonym }, query: "userid<-pseudonym" };
    this.sendMessage({ request });
  }

  public onMessage = message => {
    console.log("Processing Message ", message.id);
    const data = this.getMessageData(message);
    console.log("Response : ", JSON.stringify(data, undefined, 2));
    message.ack();
  };

  public generate() {
    this.createNewUser("4790300030");
    this.createNewUser("4790300031");
    this.createNewUser("4790300032");
    this.getUserIdforMsisdn("4790300031");
    this.getUserIdforMsisdn("4790300032");
  }
  public generate_pseudonyms() {
    this.createNewPseudonym("4790300030", "unknown30");
    this.createNewPseudonym("4790300031", "unknown31");
    this.createNewPseudonym("4790300032", "unknown32");
  }
  public test_pseudonyms() {
    this.getPseudonymForMsisdn("4790300030");
    this.getPseudonymForMsisdn("4790300031");
    this.getPseudonymForMsisdn("4790300032");
  }
}
