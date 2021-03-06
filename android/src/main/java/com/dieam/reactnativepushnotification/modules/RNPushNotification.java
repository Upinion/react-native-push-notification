package com.dieam.reactnativepushnotification.modules;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.json.*;

import android.content.Context;
import android.util.Log;

public class RNPushNotification extends ReactContextBaseJavaModule {
    private ReactContext mReactContext;
    private RNPushNotificationHelper mRNPushNotificationHelper;
    private String token;
    private final Random mRandomNumberGenerator;

    public RNPushNotification(ReactApplicationContext reactContext) {
        super(reactContext);

        mReactContext = reactContext;
        mRNPushNotificationHelper = new RNPushNotificationHelper((Application) reactContext.getApplicationContext());
        mRandomNumberGenerator = new Random(System.currentTimeMillis());
        registerNotificationsRegistration();
        registerNotificationsReceiveNotification();
    }

    @Override
    public String getName() {
        return "RNPushNotification";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        return constants;
    }

    private void sendEvent(String eventName, Object params) {
        if (mReactContext.hasActiveCatalystInstance()) {
            mReactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    public void newIntent(Intent intent) {
        if (intent.hasExtra("notification")) {
            Bundle bundle = intent.getBundleExtra("notification");
            bundle.putBoolean("foreground", false);
            intent.putExtra("notification", bundle);
            notifyNotification(bundle);
        }
    }

    private void registerNotificationsRegistration() {
        IntentFilter intentFilter = new IntentFilter("RNPushNotificationRegisteredToken");

        mReactContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String newtoken = intent.getStringExtra("token");
                token = newtoken;
                WritableMap params = Arguments.createMap();
                params.putString("deviceToken", token);

                sendEvent("remoteNotificationsRegistered", params);
            }
        }, intentFilter);
    }

    private void registerNotificationsReceiveNotification() {
        IntentFilter intentFilter = new IntentFilter("RNPushNotificationReceiveNotification");
        mReactContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyNotification(intent.getBundleExtra("notification"));
            }
        }, intentFilter);
    }

    private void notifyNotification(Bundle bundle) {
        String bundleString = convertJSON(bundle);

        WritableMap params = Arguments.createMap();
        params.putString("dataJSON", bundleString);

        sendEvent("remoteNotificationReceived", params);
    }

    private void registerNotificationsReceiveNotificationActions(ReadableArray actions) {
        IntentFilter intentFilter = new IntentFilter();
        // Add filter for each actions.
        for (int i=0; i<actions.size(); i++) {
            String action = actions.getString(i);
            intentFilter.addAction(action);
        }
        mReactContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getBundleExtra("notification");

                // Notify the action.
                notifyNotificationAction(bundle);

                // Dismiss the notification popup.
                NotificationManager manager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
                int notificationID = Integer.parseInt(bundle.getString("id"));
                manager.cancel(notificationID);
            }
        }, intentFilter);
    }

    private void notifyNotificationAction(Bundle bundle) {
        String bundleString = convertJSON(bundle);

        WritableMap params = Arguments.createMap();
        params.putString("dataJSON", bundleString);

        sendEvent("notificationActionReceived", params);
    }

    private String convertJSON(Bundle bundle) {
        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    json.put(key, JSONObject.wrap(bundle.get(key)));
                } else {
                    json.put(key, bundle.get(key));
                }
            } catch (JSONException e) {
                return null;
            }
        }
        return json.toString();
    }

    @ReactMethod
    public void requestPermissions() {
        Intent GCMService = new Intent(mReactContext, RNPushNotificationRegistrationService.class);

        mReactContext.startService(GCMService);
    }

    @ReactMethod
    public void cancelAllLocalNotifications() {
        mRNPushNotificationHelper.cancelAll();
    }

    @ReactMethod
    public void presentLocalNotification(ReadableMap details) {
        Bundle bundle = Arguments.toBundle(details);
        // If notification ID is an Int, log an error
        if ( bundle.getString("id") == null && bundle.getInt("id") != 0) {
            Log.i("ReactNotification", "Notification id should a String");
            return;
        }
        // If notification ID is not provided by the user, generate one at random
        if ( bundle.getString("id") == null ) {
            bundle.putString("id", String.valueOf(mRandomNumberGenerator.nextInt()));
        }
        mRNPushNotificationHelper.sendNotification(bundle);
    }

    @ReactMethod
    public void scheduleLocalNotification(ReadableMap details) {
        Bundle bundle = Arguments.toBundle(details);
        // If notification ID is an Int, log an error
        if ( bundle.getString("id") == null && bundle.getInt("id") != 0) {
            Log.i("ReactNotification", "Notification id should a String");
            return;
        }
        // If notification ID is not provided by the user, generate one at random
        if ( bundle.getString("id") == null ) {
            bundle.putString("id", String.valueOf(mRandomNumberGenerator.nextInt()));
        }
        mRNPushNotificationHelper.sendNotificationScheduled(bundle);
    }

    @ReactMethod
    public void getInitialNotification(Promise promise) {
        WritableMap params = Arguments.createMap();
        Activity activity = getCurrentActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            Bundle bundle = intent.getBundleExtra("notification");
            if (bundle != null) {
                bundle.putBoolean("foreground", false);
                String bundleString = convertJSON(bundle);
                params.putString("dataJSON", bundleString);
            }
        }
        promise.resolve(params);
    }

    @ReactMethod
    public void cancelLocalNotifications(ReadableMap details) {
        Bundle bundle = Arguments.toBundle(details);
        // If notification ID is an Int, log an error
        if ( bundle.getString("id") == null && bundle.getInt("id") != 0) {
            Log.i("ReactNotification", "Notification id should a String");
            return;
        }
        Log.i("ReactNotification", "Deleting notification with ID " + bundle.getString("id"));
        mRNPushNotificationHelper.cancelNotification(bundle);
    }

    @ReactMethod
    public void registerNotificationActions(ReadableArray actions) {
        registerNotificationsReceiveNotificationActions(actions);
    }    
}
