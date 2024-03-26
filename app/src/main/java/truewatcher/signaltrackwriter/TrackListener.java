package truewatcher.signaltrackwriter;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

public class TrackListener implements LocationListener,CellDataReceiver {

  private Context mActivity;
  private boolean mOn=false;
  private int mCounter=0; // GPS fixes
  private int mCounter2=0; // new points
  public String status=" new ";
  public long prevUpdateTime=0;
  public long updateTime=0;
  public long startUpdatesTime=0;
  private Trackpoint mPrevTrackpoint=new Trackpoint("0","0");
  private Trackpoint mTp=new Trackpoint("0","0");
  private LocationManager mLocationManager=null;// same instance for startListening and stopListening !
  private CellInformer mCellInformer = new CellInformer();
  private MyRegistry mRg = MyRegistry.getInstance();
  private TrackStorage mTrackStorage=null;//=Model.getInstance().getTrackStorage(); causes loop

  public TrackListener(TrackStorage aTrackStorage) {
    mTrackStorage=aTrackStorage;
  }

  public boolean isOn() { return mOn; }
  public void setOn() { mOn=true; }
  public void setOff() { mOn=false; }

  public void clearCounter() { mCounter=0; }
  public void incCounter() { mCounter+=1; }
  public int getCounter() { return mCounter; }
  public void clearCounter2() { mCounter2=0; }
  public void incCounter2() { mCounter2+=1; }
  public int getCounter2() { return mCounter2; }

  public void startListening(Context ct) {
    long minTimeMs=1000*mRg.getInt("gpsMinDelayS");//U.minFixDelayS;
    float minDistanceM=0;//mRg.getInt("gpsMinDistance");// get updates by timer, check signal
    mActivity = ct;
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

  @RequiresApi(api = Build.VERSION_CODES.Q)
  public void onLocationChanged(Location loc) {
    if (U.DEBUG) Log.i(U.TAG, "LocationReceiver:"+"got a location " + loc.toString());
    incCounter();// counting GPS fixes
    prevUpdateTime=updateTime;
    updateTime = U.getTimeStamp();
    if (prevUpdateTime > 0) {
      long delay = updateTime - prevUpdateTime;
      if (delay - U.minFixDelayS > mRg.getInt("gpsTimeoutS")) {
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

  @RequiresApi(api = Build.VERSION_CODES.Q)
  private void onPointavailable(Location loc) {
    mTp = new Trackpoint(loc);
    mCellInformer.bindActivity((FragmentActivity) mActivity);
    mCellInformer.requestCellInfos(this);
  }

  public void onCellDataObtained(List<JSONObject> cellData) {
    onCellDataObtained(cellData.get(0));
  }

  public void onCellDataObtained(JSONObject cellData) {
    //JSONObject cellData = Model.getInstance().getCellInformer().getInfo();
    mTp.addCell(cellData);
    if (U.DEBUG) Log.i(U.TAG, "got a fix");
    if (farEnough(mTp, mPrevTrackpoint)) {
      mPrevTrackpoint = mTp;
      mTrackStorage.simplySave(mTp);
      incCounter2(); // counting new points
      if (U.DEBUG) Log.i(U.TAG, "new point "+String.valueOf(mTrackStorage.getLastId()));
      return;
    }
    if (signalChanged(mTp, mPrevTrackpoint)) {
      mPrevTrackpoint = mTp;
      mTrackStorage.updateLast(mTp);
      if (U.DEBUG) Log.i(U.TAG, "update "+String.valueOf(mTrackStorage.getLastId())+":"
          +String.valueOf(mTp.data)+", "+mTp.data1);
      return;
    }
    if (U.DEBUG) Log.i(U.TAG, "noop");
    return;
  }

  private boolean farEnough(LatLon x, LatLon y) {
    int delta = (int)  Math.floor(U.proximityM(x,y));
    if (U.DEBUG) Log.i(U.TAG, "delta:"+String.valueOf(delta ));
    return (delta >= mRg.getInt("gpsMinDistance"));
  }

  private boolean signalChanged(Trackpoint x, Trackpoint y) {
    //return true;
    if ( ! x.data1.equals(y.data1)) return true; // cell handover
    int minDbmDelta=5;
    try {
      if (Math.abs(extractStrength(x.data) -  extractStrength(y.data)) >= minDbmDelta) return true;
    }
    catch (U.DataException e) {
      Log.e(U.TAG, "Cannot extract signal strength:"+e.getMessage());
    }
    return false;
  }

  private int extractStrength(String sd) throws U.DataException {
    final String FIELD = "dBm";
    JSONObject signal = new JSONObject();
    int ss = 0;
    try {
      signal = new JSONObject(sd);
      if ( ! signal.has(FIELD) ) { throw new U.DataException("No "+FIELD+" field"); }
      ss = Integer.parseInt( signal.optString(FIELD));
    }
    catch (JSONException e) { throw new U.DataException( "Malformed JSON:"+ e.getMessage() ); }
    catch (NumberFormatException e) { throw new U.DataException( "Malformed strength:"+ e.getMessage() ); }
    return ss;
  }

}
