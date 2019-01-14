package com.dieam.reactnativepushnotification.modules;

import android.content.Intent;
import android.os.Bundle;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;

import java.util.List;
import java.util.Random;
import java.util.Map;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

public class RNPushNotificationListenerService extends FirebaseMessagingService{
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        if (data != null) {
            if (!bundle.containsKey("message")) {
                bundle.putString("message", "Notification received");
            }
            if (!bundle.containsKey("title")) {
                bundle.putString("title", null);
            }
        }

        sendNotification(bundle);
    }

    private JSONObject getPushData(String dataString) {
        try {
            return new JSONObject(dataString);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendNotification(Bundle bundle) {

        // If notification ID is not provided by the user, generate one at random
        if ( bundle.getString("id") == null ) {
            Random randomNumberGenerator = new Random(System.currentTimeMillis());
            bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));
        }

        Boolean isRunning = isApplicationRunning();

        Intent intent = new Intent("RNPushNotificationReceiveNotification");
        bundle.putBoolean("foreground", isRunning);
        bundle.putBoolean("userInteraction", false);
        intent.putExtra("notification", bundle);
        sendBroadcast(intent);

        if (!isRunning) {
            new RNPushNotificationHelper(getApplication()).sendNotification(bundle);
        }
    }

    private boolean isApplicationRunning() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.processName.equals(getApplication().getPackageName())) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String d : processInfo.pkgList) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
