package com.mikerinehart.rideguide;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.mikerinehart.rideguide.activities.Constants;
import com.mikerinehart.rideguide.activities.DecisionActivity;
import com.mikerinehart.rideguide.activities.MainActivity;
import com.mikerinehart.rideguide.activities.NotificationCenterActivity;
import com.mikerinehart.rideguide.models.Notification;

import java.sql.Timestamp;
import java.util.Date;

public class GcmIntentService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "com.mikerinehart.rideguide.action.FOO";
    private static final String ACTION_BAZ = "com.mikerinehart.rideguide.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.mikerinehart.rideguide.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.mikerinehart.rideguide.extra.PARAM2";

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
            String messageType = gcm.getMessageType(intent);

            if (!extras.isEmpty())
            {
                if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                    String message = extras.getString("notification");
                    String type = extras.getString("type");
                    Date date = new Date();
                    String fbUid = extras.getString("fbUid");

                    storeNotification(new Notification(message, date, fbUid, type));
                    sendNotification(extras.getString("notification"));

                }
            }

            GcmBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private void storeNotification(Notification n) {
        SharedPreferences sp = GcmIntentService.this.getSharedPreferences(Constants.NOTIFICATIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        Long timestamp = n.getDate().getTime();

        Gson gson = new Gson();
        String jsonNotification = gson.toJson(n);
        editor.putString(timestamp.toString(), jsonNotification);
        editor.commit();
    }

    private void sendNotification(String message) {
        mNotificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, NotificationCenterActivity.class), 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setContentText(message);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();
    }
}
