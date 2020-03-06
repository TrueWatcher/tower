package truewatcher.tower;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class TrackListener implements LocationListener {

  private boolean mOn=false;
  private int mCounter=0;
  public String status=" new ";
  public long prevUpdateTime=0;
  public long updateTime=0;
  public long startUpdatesTime=0;
  private LocationManager mLocationManager=null;// same instance for startListening and stopListening !
  //private Model.LocationReceiver mLocationReceiver = new Model.LocationReceiver();
  private MyRegistry mRg = MyRegistry.getInstance();
  private TrackStorage mTrackStorage=null;//=Model.getInstance().getTrackStorage(); causes loop
  private TrackPointListener mListener=null;

  public TrackListener(TrackStorage aTrackStorage) {
    mTrackStorage=aTrackStorage;
  }

  public boolean isOn() { return mOn; }
  public void setOn() { mOn=true; }
  public void setOff() { mOn=false; }

  public void clearCounter() { mCounter=0; }
  public void incCounter() { mCounter+=1; }
  public int getCounter() { return mCounter; }

  public void startListening(Context ct) {
    long minTimeMs=1000*mRg.getInt("gpsMinDelayS");//U.minFixDelayS;
    float minDistanceM=mRg.getInt("gpsMinDistance");//0;
    try {
      mLocationManager = (LocationManager) ct.getSystemService(Context.LOCATION_SERVICE);
      mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, minDistanceM,
              this);
      prevUpdateTime=updateTime=startUpdatesTime=U.getTimeStamp();
    }
    catch (SecurityException e) { throw new U.RunException(e.getMessage()); }
  }

  public void stopListening() {
    try {
      if (null == mLocationManager) return;
      mLocationManager.removeUpdates(this);
      mLocationManager = null;
    }
    catch (SecurityException e) { throw new U.RunException(e.getMessage()); }
  }

  public void onLocationChanged(Location loc) {
    if (U.DEBUG) Log.i(U.TAG, "LocationReceiver:"+"got a location " + loc.toString());
    incCounter();
    prevUpdateTime=updateTime;
    updateTime = U.getTimeStamp();
    if (prevUpdateTime > 0) {
      long delay = updateTime - prevUpdateTime;
      if (delay - mRg.getInt("gpsMinDelayS") > mRg.getInt("gpsTimeoutS")) {
        mTrackStorage.saveNote("delay=" + Long.toString(delay) + "s", "");
      }
    }
    onPointavailable(loc);
  }

  public void onStatusChanged(String provider, int status, Bundle extras) {
    if ( ! provider.equals(LocationManager.GPS_PROVIDER)) return;
    if (U.DEBUG) Log.i(U.TAG, "got new GPS status:"+String.valueOf(status));
  }

  @Override
  public void onProviderEnabled(String provider) {}

  @Override
  public void onProviderDisabled(String provider) {}

  private void onPointavailable(Location loc) {
    Trackpoint p=new Trackpoint(loc);
    p=mTrackStorage.simplySave(p);// may set newSegment
    Model.getInstance().getJSbridge().consumeTrackpoint(p);
    if (null != mListener) mListener.onTrackpointAvailable(p);
  }

  public static interface TrackPointListener {
    public void onTrackpointAvailable(Trackpoint p);
  }

  public void attachListener(TrackPointListener l) { mListener=l; }

  public void removeListener(TrackPointListener l) {
    mListener=null;
    //if (mListener == null) return;
    //if (mListener != l) throw new U.RunException("Unregistering of unknown listener");
    //mListener=null;
  }
}
