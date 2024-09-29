package truewatcher.signaltrackwriter;
// copied intact from truewatcher.tower; 2.9.6

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

// https://www.here.com/docs/bundle/sdk-for-android-navigate-developer-guide/page/topics/get-locations-enable-background-updates.html

public class ForegroundService extends Service {

  private static final int NOTIFICATION_ID = 12345678;
  private static final String CHANNEL_ID = "channel_01";

  @Override
  public void onCreate() {
    super.onCreate();
    if (U.DEBUG) Log.i(U.TAG, "ForegroundService:"+"onCreate");
  }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (U.DEBUG) Log.i(U.TAG, "ForegroundService:"+"onStartCommand");
    Model.getInstance().getTrackStorage().saveNote(
            "onStartCommand",
            String.format("intnt:%b,flags:%d,id:%d",intent,flags,startId));
    createNotificationChannel();
    try {
      startForeground(NOTIFICATION_ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
    }
    catch (NoSuchMethodError e) { // API <= 28
      startForeground(NOTIFICATION_ID, getNotification());
    }
    if (U.DEBUG) Log.i(U.TAG, "ForegroundService:"+"onStartCommand"+": service started!");
    return START_STICKY;
  }

  private Notification getNotification() {
    String text="recording started";
    Intent intent = new Intent(this, MainActivity.class);
    PendingIntent contentIntent = PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
            .setContentTitle("SignalTrackWriter")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            ;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setChannelId(CHANNEL_ID);
    }

    return builder.build();
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel serviceChannel = new NotificationChannel(
              CHANNEL_ID,
              "Foreground Service Channel",
              NotificationManager.IMPORTANCE_DEFAULT
      );

      NotificationManager manager = getSystemService(NotificationManager.class);
      manager.createNotificationChannel(serviceChannel);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
