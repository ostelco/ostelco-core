import * as distanceInWordsStrict from "date-fns/distance_in_words_strict";
import { MessageProcessor } from "../message_processor";

export class MockProducer extends MessageProcessor {
  constructor() {
    super("api-responses", "api-requests", "response-channel");
  }

  public async sendNLogRequest(request) {
    const timeRequest = new Date();
    console.log("Sending Request = ", JSON.stringify(request, undefined, 0));
    const result = await this.sendRequest(request);
    const timeResponse = new Date();
    const timeDiff = distanceInWordsStrict(timeResponse, timeRequest);
    console.log(
      `Answer for ${result.request.id} came in ${timeDiff}`,
      JSON.stringify(result, undefined, 0)
    );
  }

  public async createNewUser(msisdn) {
    const request = { params: { msisdn }, query: "newuser" };
    this.sendNLogRequest(request);
  }
  public async getUserIdforMsisdn(msisdn) {
    const request = { params: { msisdn }, query: "userid<-msisdn" };
    this.sendNLogRequest(request);
  }
  public async getMsisdnForUserId(userId) {
    const request = { params: { userId }, query: "msisdn<-userid" };
    this.sendNLogRequest(request);
  }
  public async createNewPseudonym(msisdn: string, userId: string) {
    const request = { params: { msisdn, userId, timestamp: Date.now() }, query: "newpseudonym" };
    this.sendNLogRequest(request);
  }
  public async getPseudonymForMsisdn(msisdn: string) {
    const request = { params: { msisdn, timestamp: Date.now() }, query: "pseudonym<-msisdn" };
    this.sendNLogRequest(request);
  }
  public async getUserIdforPseudonym(pseudonym: string) {
    const request = { params: { pseudonym }, query: "userid<-pseudonym" };
    this.sendNLogRequest(request);
  }

  public onMessage = message => {
    console.log("Processing Message ", message.id);
    const data = this.getMessageData(message);
    // console.log("Response : ", JSON.stringify(data, undefined, 2));
    this.processResponse(message);
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
