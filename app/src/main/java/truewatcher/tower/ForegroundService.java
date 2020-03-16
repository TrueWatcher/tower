package truewatcher.tower;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

// https://github.com/android/location-samples/blob/master/LocationUpdatesForegroundService/app/src/main/java/com/google/android/gms/location/sample/locationupdatesforegroundservice/LocationUpdatesService.java
// https://www.hellsoft.se/how-to-service-on-android---part-2/

public class ForegroundService extends Service {

  private static final int NOTIFICATION_ID = 12345678;
  private static final String CHANNEL_ID = "channel_01";

  @Override
  public void onCreate() {
    super.onCreate();
    if (U.DEBUG) Log.i(U.TAG, "ForegroundService:"+"onCreate");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (U.DEBUG) Log.i(U.TAG, "ForegroundService:"+"onStartCommand");
    Model.getInstance().getTrackStorage().saveNote(
            "onStartCommand",
            String.format("intnt:%b,flags:%d,id:%d",intent,flags,startId));
    startForeground(NOTIFICATION_ID, getNotification());
    return START_STICKY;
  }

  private Notification getNotification() {
    String text="tracking is on";
    Intent intent = new Intent(this, MainActivity.class);
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
            .setContentTitle("Tower")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher3)
            .setContentIntent(contentIntent)
            ;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.setChannelId(CHANNEL_ID);
    }

    return builder.build();
  }

  private final IBinder mBinder = new LocalBinder();

  public class LocalBinder extends Binder {
    ForegroundService getService() {
      return ForegroundService.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }
}
