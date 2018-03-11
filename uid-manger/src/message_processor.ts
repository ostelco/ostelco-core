import PubSub = require("@google-cloud/pubsub");
import * as uuidv4 from "uuid/v4";

export class MessageProcessor {
  private pubsub: PubSub;
  private subscriptionId: string;
  private publisher;
  private subscription;
  private requestTopic;
  private responseTopic;

  constructor(requestTopicName: string, responseTopicName: string, subscriptionId: string) {
    this.pubsub = new PubSub();
    this.requestTopic = this.pubsub.topic(requestTopicName);
    this.responseTopic = this.pubsub.topic(responseTopicName);
    this.publisher = this.requestTopic.publisher();
    this.subscriptionId = subscriptionId;
    this.subscription = this.responseTopic.subscription(this.subscriptionId);
  }

  public async createSubscription() {
    const exists: boolean = await this.checkSubscriptionExists();
    if (!exists) {
      // Create the subscription.
      const data = await this.responseTopic.createSubscription(this.subscriptionId);
      this.subscription = data[0];
    }
    this.subscription.on("message", this.onMessage);
    this.subscription.on("error", this.onError);
  }

  public onError = err => {
    console.log("Error = ", err);
  };

  // Register a listener for `message` events.
  public onMessage = message => {
    console.log("Message = ", message);
    // Called every time a message is received.
    // message.id = ID of the message.
    // message.ackId = ID used to acknowledge the message receival.
    // message.data = Contents of the message.
    // message.attributes = Attributes of the message.
    // message.timestamp = Timestamp when Pub/Sub received the message.
    if (Buffer.isBuffer(message.data)) {
      const data: Buffer = message.data as Buffer;
      console.log("Buffer = ", data.toString());
    }
    // Ack the message:
    message.ack();
    // This doesn't ack the message, but allows more messages to be retrieved
    // if your limit was hit or if you don't want to ack the message.
    // message.nack();
  };

  public getMessageData(message) {
    if (Buffer.isBuffer(message.data)) {
      const data: Buffer = message.data as Buffer;
      console.log("Buffer = ", data.toString());
      return JSON.parse(data.toString());
    }
    return undefined;
  }

  public async sendMessage(message) {
    const data = Buffer.from(JSON.stringify(message));
    const messageId = await this.publisher.publish(data);
    return messageId;
  }

  public async deleteResponseSubscription() {
    // Remove the listener from receiving `message` events.
    this.subscription.removeListener("message", this.onMessage);
    this.subscription.removeListener("error", this.onError);
    const data = await this.subscription.delete();
    const apiResponse = data[0];
    return apiResponse;
  }

  private async checkSubscriptionExists() {
    const data = await this.subscription.exists();
    return data[0];
  }
}
