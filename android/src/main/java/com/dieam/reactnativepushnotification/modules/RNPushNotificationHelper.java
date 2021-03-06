package com.dieam.reactnativepushnotification.modules;


import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Random;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;

public class RNPushNotificationHelper {
    public static final String PREFERENCES_KEY = "RNPushNotification";
    private static final long DEFAULT_VIBRATION = 1000L;
    private static final String TAG = RNPushNotificationHelper.class.getSimpleName();

    private static final String CHANNEL_ID = "0";
    private static final String CHANNEL_NAME = "Notifications";
    private static final String SILENCE_CHANNEL_ID = "1";
    private static final String SILENCE_CHANNEL_NAME = "Silence Notifications";

    private Context mContext;
    private final SharedPreferences mSharedPreferences;

    public RNPushNotificationHelper(Application context) {
        mContext = context;
        mSharedPreferences = (SharedPreferences)context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public Class getMainActivityClass() {
        String packageName = mContext.getPackageName();
        Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
    }

    private PendingIntent getScheduleNotificationIntent(Bundle bundle) {
        int notificationID;
        String notificationIDString = bundle.getString("id");

        if (notificationIDString != null) {
            notificationID = Integer.parseInt(notificationIDString);
        } else {
            Log.e("RNPushNotification", "No notification ID specified to cancel notification");
            return null;
        }

        Intent notificationIntent = new Intent(mContext, RNPushNotificationPublisher.class);
        notificationIntent.putExtra(RNPushNotificationPublisher.NOTIFICATION_ID, notificationID);
        notificationIntent.putExtras(bundle);

        return PendingIntent.getBroadcast(mContext, notificationID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void sendNotificationScheduled(Bundle bundle) {
        Class intentClass = getMainActivityClass();
        if (intentClass == null) {
            Log.e("RNPushNotification", "No activity class found for the notification");
            return;
        }

        if (bundle.getString("message") == null) {
            Log.e("RNPushNotification", "No message specified for the notification");
            return;
        }

        if(bundle.getString("id") == null) {
            Log.e("RNPushNotification", "No notification ID specified for the notification");
            return;
        }

        double fireDate = bundle.getDouble("fireDate");
        if (fireDate == 0) {
            Log.e("RNPushNotification", "No date specified for the scheduled notification");
            return;
        }

        long currentTime = System.currentTimeMillis();
        Log.i("RNPushNotification", "fireDate: " + fireDate + ", Now Time: " + currentTime);

        storeNotificationToPreferences(bundle);

        sendNotificationScheduledCore(bundle);
    }

    public void sendNotificationScheduledCore(Bundle bundle) {
        long fireDate = (long)bundle.getDouble("fireDate");

        // If the fireDate is in past, show the notification immediately.
        // This is to cover the case when user has scheduled a few notifications
        // and the phone has been switched off at the time of notification. Such
        // notifications should be shown to the user as soon as the phone is booted
        // again
        if(fireDate < System.currentTimeMillis()) {
            sendNotification(bundle);
        } else {
            PendingIntent pendingIntent = getScheduleNotificationIntent(bundle);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getAlarmManager().setExact(AlarmManager.RTC_WAKEUP, fireDate, pendingIntent);
            } else {
                getAlarmManager().set(AlarmManager.RTC_WAKEUP, fireDate, pendingIntent);
            }
        }
    }

    private void storeNotificationToPreferences(Bundle bundle) {
        RNPushNotificationAttributes notificationAttributes = new RNPushNotificationAttributes();
        notificationAttributes.fromBundle(bundle);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(notificationAttributes.getId(), notificationAttributes.toJson().toString());
        commitPreferences(editor);
    }

    private void commitPreferences(SharedPreferences.Editor editor) {
        if (Build.VERSION.SDK_INT < 9) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    public void sendNotification(Bundle bundle) {
        try {
        	Class intentClass = getMainActivityClass();
        	if (intentClass == null) {
            	Log.e("RNPushNotification", "No activity class found for the notification");
            	return;
        	}
            
        	if (bundle.getString("message") == null) {
            	Log.e("RNPushNotification", "No message specified for the notification");
            	return;
        	}

        	if (bundle.getString("id") == null) {
            	Log.e("RNPushNotification", "No notification ID specified for the notification");
            	return;
        	}

            Resources res = mContext.getResources();
            String packageName = mContext.getPackageName();

            String title = bundle.getString("title");
            if (title == null) {
                ApplicationInfo appInfo = mContext.getApplicationInfo();
                title = mContext.getPackageManager().getApplicationLabel(appInfo).toString();
            }

        	NotificationCompat.Builder notification = new NotificationCompat.Builder(mContext)
                .setContentTitle(title)
                .setTicker(bundle.getString("ticker"))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(bundle.getBoolean("autoCancel", true));

            String group = bundle.getString("group");
            if (group != null) {
                notification.setGroup(group);
            }

            notification.setContentText(bundle.getString("message"));

            String largeIcon = bundle.getString("largeIcon");

            String subText = bundle.getString("subText");

            if (subText != null) {
                notification.setSubText(subText);
            }

            if (bundle.containsKey("number")) {
                try {
                    int number = (int) bundle.getDouble("number");
                    notification.setNumber(number);
                } catch (Exception e) {
                    String numberAsString = bundle.getString("number");
                    if(numberAsString != null) {
                        int number = Integer.parseInt(numberAsString);
                        notification.setNumber(number);
                        Log.w(TAG, "'number' field set as a string instead of an int");
                    }
                }
            }

            int smallIconResId;
            int largeIconResId;

            String smallIcon = bundle.getString("smallIcon");

            if (smallIcon != null) {
                smallIconResId = res.getIdentifier(smallIcon, "mipmap", packageName);
            } else {
                smallIconResId = res.getIdentifier("ic_notification", "mipmap", packageName);
            }

            if (smallIconResId == 0) {
                smallIconResId = res.getIdentifier("ic_launcher", "mipmap", packageName);

            	if (smallIconResId == 0) {
                	smallIconResId = android.R.drawable.ic_dialog_info;
            	}
            }

            if (largeIcon != null) {
                largeIconResId = res.getIdentifier(largeIcon, "mipmap", packageName);
            } else {
                largeIconResId = res.getIdentifier("ic_launcher", "mipmap", packageName);
            }

            Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);

            if (largeIconResId != 0 && (largeIcon != null || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)) {
                notification.setLargeIcon(largeIconBitmap);
            }

            notification.setSmallIcon(smallIconResId);
            String bigText = bundle.getString("bigText");

            if (bigText == null) {
                bigText = bundle.getString("message");
            }

            notification.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));

            Intent intent = new Intent(mContext, intentClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            bundle.putBoolean("userInteraction", true);
            intent.putExtra("notification", bundle);

            Boolean isSilence = true;

            if (!bundle.containsKey("playSound") || bundle.getBoolean("playSound")) {
                isSilence = false;
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                notification.setSound(defaultSoundUri);
            }

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notification.setCategory(NotificationCompat.CATEGORY_CALL);

                String color = bundle.getString("color");
                if (color != null) {
                    notification.setColor(Color.parseColor(color));
                }
            }

            int notificationID = (int) System.currentTimeMillis();
            if (bundle.containsKey("id")) {
                try {
                    notificationID = (int) bundle.getDouble("id");
                } catch (Exception e) {
                    String notificationIDString = bundle.getString("id");

                    if (notificationIDString != null) {
                        try {
                            notificationID = Integer.parseInt(notificationIDString);
                        } catch (NumberFormatException nfe) {
                            Log.w(TAG, "'id' field could not be converted to an int, ignoring it", nfe);
                        }
                    }
                }
            }

            NotificationManager notificationManager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, notificationID, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);


            notification.setContentIntent(pendingIntent);

            if (!bundle.containsKey("vibrate") || bundle.getBoolean("vibrate")) {
                isSilence = false;
                long vibration = bundle.containsKey("vibration") ? (long) bundle.getDouble("vibration") : DEFAULT_VIBRATION;
                if (vibration == 0)
                    vibration = DEFAULT_VIBRATION;
                notification.setVibrate(new long[]{0, vibration});
            }

            JSONArray actionsArray = null;
            if (bundle.getString("actions") != null) {
                try {
                    actionsArray = new JSONArray(bundle.getString("actions"));
                } catch (Exception e) {
                    Log.e("RNPushNotification", "Exception while converting actions to JSON object.", e);
                }
            }

            if (actionsArray != null) {
                // No icon for now. The icon value of 0 shows no icon.
                int icon = 0;

                // Add button for each actions.
                for (int i = 0; i < actionsArray.length(); i++) {
                    String action = null;
                    try {
                        action = actionsArray.getString(i);
                    } catch (JSONException e) {
                        Log.e("RNPushNotification", "Exception while getting action from actionsArray.", e);
                        continue;
                    }

                    Intent actionIntent = new Intent();
                    actionIntent.setAction(action);
                    // Add "action" for later identifying which button gets pressed.
                    bundle.putString("action", action);
                    actionIntent.putExtra("notification", bundle);
                    PendingIntent pendingActionIntent = PendingIntent.getBroadcast(mContext, notificationID, actionIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    notification.addAction(icon, action, pendingActionIntent);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Select channel
                String channel_id = mContext.getPackageName() + (isSilence ? this.SILENCE_CHANNEL_ID : this.CHANNEL_ID);
                String channel_name = isSilence ? this.SILENCE_CHANNEL_NAME : this.CHANNEL_NAME;
                // Create or update.
                NotificationChannel channel = new NotificationChannel(
                    channel_id,
                    channel_name,
                    NotificationManager.IMPORTANCE_DEFAULT
                );
                if (!isSilence) {
                    channel.enableLights(true);
                    channel.setLightColor(Color.GREEN);
                    channel.enableVibration(true);
                    channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
                    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    channel.setSound(defaultSoundUri, null);
                } else {
                    channel.enableLights(true);
                    channel.setLightColor(Color.GREEN);
                    channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
                    channel.enableVibration(false);
                    channel.setSound(null, null);
                }

                notificationManager.createNotificationChannel(channel);
                // Assign channel to notification
                notification.setChannelId(channel_id);
            }

            Notification info = notification.build();
            info.defaults |= Notification.DEFAULT_LIGHTS;

            if(mSharedPreferences.getString(Integer.toString(notificationID), null) != null) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.remove(Integer.toString(notificationID));
                commitPreferences(editor);
            }

            if (bundle.containsKey("tag")) {
                String tag = bundle.getString("tag");
                notificationManager.notify(tag, notificationID, info);
            } else {
                notificationManager.notify(notificationID, info);
            }
        } catch (Exception e) {
            Log.e(TAG, "failed to send push notification", e);
        }
    }

    public void cancelAll() {
        Set<String> ids = mSharedPreferences.getAll().keySet();

        for (String id: ids) {
            Bundle b = new Bundle();
            b.putString("id", id);
            this.cancelNotification(b);
        }
    }

    public void cancelNotification(Bundle bundle) {
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        String notificationIDString = bundle.getString("id");

        notificationManager.cancel(Integer.parseInt(notificationIDString));

        getAlarmManager().cancel(getScheduleNotificationIntent(bundle));

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(notificationIDString);
        commitPreferences(editor);
    }
}
