/* eslint-disable */
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendLowStockNotification = functions.pubsub.schedule('every 24 hours').onRun(async (context) => {
    const productsSnapshot = await admin.firestore().collection('Products').get();
    const now = admin.firestore.Timestamp.now();
    const thresholdDate = new admin.firestore.Timestamp(now.seconds + (4 * 24 * 60 * 60), 0); // 4 nap

    productsSnapshot.forEach(async (productDoc) => {
        const productData = productDoc.data();
        const quantity = productData.quantity;
        const minQuantity = productData.min_quantity;
        const expiryDate = productData.expiry_date;
        const addedDate = productData.added_date;

        if (quantity <= minQuantity || expiryDate.toMillis() <= thresholdDate.toMillis()) {
            const userId = productData.user_id;

            if (expiryDate.toMillis() <= thresholdDate.toMillis()) {
                await sendNotificationToUser(userId, productData.name, "lejár a termék!");
            }

            // 5 napos tárolás figyelmeztetés
            if (addedDate && now.toMillis() - addedDate.toMillis() >= 5 * 24 * 60 * 60 * 1000) {
                await sendNotificationToUser(userId, productData.name, "már 5 napja a tárolóban van.");
            }
        }
    });

    return null;
});

async function sendNotificationToUser(userId, productName, message) {
    const userSnapshot = await admin.firestore().collection('Users').doc(userId).get();
    const userToken = userSnapshot.data().fcm_token;

    const payload = {
        notification: {
            title: 'Termék értesítés',
            body: `${productName} ${message}`,
        }
    };

    if (userToken) {
        await admin.messaging().sendToDevice(userToken, payload);
    } else {
        console.log(`Nincs FCM token a felhasználóhoz: ${userId}`);
    }
}
