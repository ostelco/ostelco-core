// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database.
const admin = require('firebase-admin');
admin.initializeApp()

/**
 * Triggers when a user balance get below a threshold and sends a notification.
 */
exports.sendUserNotification = functions.database.ref('/fcm-requests/{msisdn}')
    .onCreate(event => {

        const userObject = event.val()
        const noOfBytes = userObject.noOfBytes
        const msisdn = userObject.msisdn

        console.log('We have a new balance:', noOfBytes, 'for user:', msisdn);

        // Get the device notification token.
        admin.database().ref(`/firebaseCloudMessage/${msisdn}`).once('value').then(result => {

            // Notification details.
            const payload = {
                notification: {
                    title: 'You have a low data balance!',
                    body: `${noOfBytes} bytes left.`
                }
            };

            console.log('Sending notification.' + payload);
            const token = result.val();

            return admin.messaging().sendToDevice(token, payload);
        }, result => {
            return console.log('Something failed', result);
        }).catch(error => { console.log('Error : ', error)});
    });