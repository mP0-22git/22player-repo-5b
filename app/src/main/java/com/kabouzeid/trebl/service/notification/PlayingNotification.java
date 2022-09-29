package com.kabouzeid.trebl.service.notification;

import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.RequiresApi;

import com.kabouzeid.trebl.R;
import com.kabouzeid.trebl.service.MusicService;

import static android.content.Context.NOTIFICATION_SERVICE;

public abstract class PlayingNotification {

    private static final int NOTIFICATION_ID = 1;
    static final String NOTIFICATION_CHANNEL_ID = "playing_notification";

    private static final int NOTIFY_MODE_FOREGROUND = 1;
    private static final int NOTIFY_MODE_BACKGROUND = 0;

    private int notifyMode = NOTIFY_MODE_BACKGROUND;

    private NotificationManager notificationManager;
    protected MusicService service;
    boolean stopped;

    public synchronized void init(MusicService service) {
        this.service = service;
        notificationManager = (NotificationManager) service.getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    public abstract void update();

    public synchronized void stop() {
        stopped = true;
        service.stopForeground(true);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    void updateNotifyModeAndPostNotification(Notification notification) {
        int newNotifyMode;
        if (service.isPlaying()) {
            newNotifyMode = NOTIFY_MODE_FOREGROUND;
        } else {
            newNotifyMode = NOTIFY_MODE_BACKGROUND;
        }

        if (notifyMode != newNotifyMode && newNotifyMode == NOTIFY_MODE_BACKGROUND) {
            service.stopForeground(false);
        }

        if (newNotifyMode == NOTIFY_MODE_FOREGROUND) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                try{
                service.startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                );
                }catch(ForegroundServiceStartNotAllowedException ignored){
                    /* Important Note: In android 12 and up, a foreground service cannot be started from background
                    * i.e if app is idle or playback is interrupted by something like an alarm, the app will sometimes go into
                    * background mode. Upon resumption a ForegroundServiceNotAllowedException is thrown.
                    *
                    * https://developer.android.com/guide/components/foreground-services#background-start-restrictions
                    *
                    * One proposed solution restricts stopForeground(false) to everything below SDK 31.
                    * This makes the notification player impossible to dismiss, so a dismiss button is added to the notification.
                    * SDK 33 onwards users can dismiss the foreground service notification even if it isn't stopped or removed from the foreground.
                    *
                    * Another temporary solution is to catch the exception with no recourse, as is done here. Although UX is potentially impacted,
                    * it will serve while other options are explored.*/
                }
            } else {
                service.startForeground(NOTIFICATION_ID,notification);
            }
        } else if (newNotifyMode == NOTIFY_MODE_BACKGROUND) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }

        notifyMode = newNotifyMode;
    }

    @RequiresApi(26)
    private void createNotificationChannel() {
        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
        if (notificationChannel == null) {
            notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, service.getString(R.string.playing_notification_name), NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setDescription(service.getString(R.string.playing_notification_description));
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);

            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
