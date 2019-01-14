# React Native Push Notifications
[![npm version](https://badge.fury.io/js/react-native-push-notification.svg?update=2)](http://badge.fury.io/js/react-native-push-notification)
[![npm downloads](https://img.shields.io/npm/dm/react-native-push-notification.svg?update=2)](https://img.shields.io/npm/dm/react-native-push-notification.svg?update=2)

React Native Local and Remote Notifications for iOS and Android

## Supported React Native Versions
| Component Version     | RN Version    | README     |
|-----------------------|---------------|------------|
| **1.0.7**          | **<= 0.27**   | [Open](https://github.com/zo0r/react-native-push-notification/blob/f42723817f1687e0da23e6753eb8a9f0385b6ac5/README.md)   |
| **1.0.8**          | **0.28**   | [Open](https://github.com/zo0r/react-native-push-notification/blob/2eafd1961273ca6a82ad4dd6514fbf1d1a829089/README.md)   |
| **2.0.1**          | **0.29**   | [Open](https://github.com/zo0r/react-native-push-notification/blob/c7ab7cd84ea19e42047379aefaf568bb16a81936/README.md)   |
| **>= 2.0.2**          | **>= 0.30**   | [Open](https://github.com/zo0r/react-native-push-notification/blob/32df1d4ef8294dbf9a29e8d13d70e1e40b16ad3c/README.md)   |


## Installation
`npm install react-native-push-notification`

## iOS Installation
The component uses PushNotificationIOS for the iOS part.

[Please see: PushNotificationIOS](https://facebook.github.io/react-native/docs/pushnotificationios.html#content)

## Android Installation

**NOTE: To use a specific `play-service-gcm` version, use in your `android/app/build.gradle` (change `8.1.0` for your version):**
```gradle
...

dependencies {
    ...

    compile ('com.google.android.gms:play-services-gcm:8.1.0') {
        force = true;
    }
}
```

In your `AndroidManifest.xml`
```xml
    .....

	<uses-permission android:name="android.permission.WAKE_LOCK" />
	<permission
	android:name="${applicationId}.permission.C2D_MESSAGE"
	android:protectionLevel="signature" />
	<uses-permission android:name="${applicationId}.permission.C2D_MESSAGE" />
	<uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>

	<application ....>
		<receiver android:name="com.dieam.reactnativepushnotification.modules.RNPushNotificationPublisher" />
		<receiver android:name="com.dieam.reactnativepushnotification.modules.RNPushNotificationBootEventReceiver">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED"></action>
            </intent-filter>
        </receiver>
		<service android:name="com.dieam.reactnativepushnotification.modules.RNPushNotificationRegistrationService"/>
		<service
			android:name="com.dieam.reactnativepushnotification.modules.RNPushNotificationListenerService"
			android:exported="false" >
			<intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT"/>
			</intent-filter>
		</service>
        .....

```

In `android/settings.gradle`
```gradle
...

include ':react-native-push-notification'
project(':react-native-push-notification').projectDir = file('../node_modules/react-native-push-notification/android')
```

In `android/app/build.gradle`

```gradle
...

dependencies {
    ...

    compile project(':react-native-push-notification')
}
```

Register module (in `MainApplication.java`)

```java
import android.content.Intent; // <--- Import Intent
import com.dieam.reactnativepushnotification.ReactNativePushNotificationPackage;  // <--- Import Package

public class MainApplication extends Application implements ReactApplication {

  private ReactNativePushNotificationPackage mReactNativePushNotificationPackage; // <------ Add Package Variable

   ...

  private final ReactNativeHost mReactNativeHost = new ReactNativeHost(this) {
    @Override
    protected boolean getUseDeveloperSupport() {
      return BuildConfig.DEBUG;
    }

      @Override
      protected List<ReactPackage> getPackages() {
      mReactNativePushNotificationPackage = new ReactNativePushNotificationPackage(); // <------ Initialize the Package

      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
          mReactNativePushNotificationPackage // <---- Add the Package
      );
    }
  };

   // Add onNewIntent
   public void onNewIntent(Intent intent) {
      if ( mReactNativePushNotificationPackage != null ) {
          mReactNativePushNotificationPackage.newIntent(intent);
      }
   }

    ....
}
```

Add `onNewIntent` (in `MainActivity.java`)

```java
import android.content.Intent; // <--- Import Intent

public class MainActivity extends ReactActivity {
   ...

    // Add onNewIntent
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ((MainApplication) getApplication()).onNewIntent(intent);
    }
    ....
}
```

## Usage
```javascript
var PushNotification = require('react-native-push-notification');

PushNotification.configure({

    // (optional) Called when Token is generated (iOS and Android)
    onRegister: function(token) {
        console.log( 'TOKEN:', token );
    },

    // (required) Called when a remote or local notification is opened or received
    onNotification: function(notification) {
        console.log( 'NOTIFICATION:', notification );
    },

    // ANDROID ONLY: (optional) GCM Sender ID.
    senderID: "YOUR GCM SENDER ID",

    // IOS ONLY (optional): default: all - Permissions to register.
    permissions: {
        alert: true,
        badge: true,
        sound: true
    },

    // Should the initial notification be popped automatically
    // default: true
    popInitialNotification: true,

    /**
      * (optional) default: true
      * - Specified if permissions (ios) and token (android and ios) will requested or not,
      * - if not, you must call PushNotificationsHandler.requestPermissions() later
      */
    requestPermissions: true,
});
```

## Handling Notifications
When any notification is opened or received the callback `onNotification` is called passing an object with the notification data.

Notification object example:
```javascript
{
    foreground: false, // BOOLEAN: If the notification was received in foreground or not
    userInteraction: false, // BOOLEAN: If the notification was opened by the user from the notification area or not
    message: 'My Notification Message', // STRING: The notification message
    data: {}, // OBJECT: The push data
}
```

## Local and Schedule Notifications
`PushNotification.localNotification(details: Object)`

`PushNotification.localNotificationSchedule(details: Object)`

EXAMPLE:
```javascript
PushNotification.localNotification({
    /* Android Only Properties */
    id: '0', // (optional) Valid unique 32 bit integer specified as string. default: Autogenerated Unique ID
    title: "My Notification Title", // (optional)
    ticker: "My Notification Ticker", // (optional)
    autoCancel: true, (optional) default: true
    largeIcon: "ic_launcher", // (optional) default: "ic_launcher"
    smallIcon: "ic_notification", // (optional) default: "ic_notification" with fallback for "ic_launcher"
    bigText: "My big text that will be shown when notification is expanded", // (optional) default: "message" prop
    subText: "This is a subText", // (optional) default: none
    number: '10', // (optional) Valid 32 bit integer specified as string. default: none (Cannot be zero)
    color: "red", // (optional) default: system default
    vibrate: true, // (optional) default: true
    vibration: 300, // vibration length in milliseconds, ignored if vibrate=false, default: 1000
    tag: 'some_tag', // (optional) add tag to message
    group: "group", // (optional) add group to message
    sound: 'default', // (optional) Sound to play when the notification is shown. Value of 'default' plays the default sound. It can be set to a custom sound such as 'android.resource://com.xyz/raw/my_sound'. It will look for the 'my_sound' audio file in 'res/raw' directory and play it. default: null (no sound is played)
    
    /* iOS only properties */
    alertAction: // (optional) default: view
    category: // (optional) default: null
    userInfo: // (optional) default: null (object containing additional notification data)

    /* iOS and Android properties */
    message: "My Notification Message" // (required)
    playSound: false, // (optional) default: true
    number: 10 // (optional) default: none (Cannot be zero)
});

PushNotification.localNotificationSchedule({
	message: "My Notification Message", // (required)
	date: new Date(Date.now() + (60 * 1000)) // in 60 secs
});
```

## Cancelling scheduled notifications
`PushNotification.cancelLocalNotifications(details: Object)` 

`details` is the `userInfo` object for iOS. For Android, it should be of shape `{id: <notification-id>}` where `<notification-id>` is the notification ID used to schedule the notification.

EXAMPLE:
```javascript
PushNotification.cancelLocalNotifications({id: '123'});
```

## Sending Notification Data From Server
Same parameters as `PushNotification.localNotification()`

## iOS Only Methods
`PushNotification.checkPermissions(callback: Function)` Check permissions

`PushNotification.setApplicationIconBadgeNumber(number: number)` set badge number

`PushNotification.getApplicationIconBadgeNumber(callback: Function)` get badge number

`PushNotification.abandonPermissions()` Abandon permissions

### TODO
- [X] Add `PushNotification.localNotificationSchedule()` Android support
- [ ] Restore Android local notifications after reboot

