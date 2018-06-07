# Firebase Cloud Messages

This is a Firebase Cloud Function used to send notifications to subscribers when they are low on balance.

When the server wants to send a notification it will add a node to the firebase path `/fcm-requests/{msisdn}`

The node looks like this:
```
{
    msisdn : 4753945
    noOfBytes: 545789345
}
```


This will be picked up by the Firebase Cloud Function that will send a push notification. To be able to send the notification the cloud function needs the
subscriber applications FirebaseCloudMessage token that identifies the app.

The application will put its token in the path: `/firebaseCloudMessage/`

```
{
    msisdn: token
}
```

## Installation

 * `npm install -g firebase-tools`
 * `firebase login`
 * `cd functions`
 * `npm install`
 * `firebase deploy --only functions`
