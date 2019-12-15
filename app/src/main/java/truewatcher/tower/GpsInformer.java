package truewatcher.tower;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

public class GpsInformer extends PointFetcher {
  
  private LocationManager mLocnManager;
  private int mFixCount=0;
  private int mGpsAcceptableAccuracy=MyRegistry.getInstance().getInt("gpsAcceptableAccuracy");
  private int mGpsMaxFixCount=MyRegistry.getInstance().getInt("gpsMaxFixCount");
  
  @Override
  protected String getPermissionType() { return Manifest.permission.ACCESS_FINE_LOCATION; }
  
  @Override
  protected int getPermissionCode() { return 2; }
  
  /* mock gps location seems to be off limits now :(
  @Override
  protected boolean tryGiveMockLocation() {
    if (mStatus.equals("enabled")) {
      giveMockLocation();
      return true;
    }
    return false;
  }*/
  
  @Override
  public void afterLocationPermissionOk() {
    mPi.addProgress("checking GPS...");
    mLocnManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
    boolean isGpsEnabled = mLocnManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    if ( ! isGpsEnabled) {
      mStatus="disabled";
      mPi.addProgress(mStatus);
      return;
    }
    mStatus="enabled";
    mPi.addProgress("Ok, connecting");
    try {
      //the permission is checked in PointFetcher; just to make Lint happy
      mLocnManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    }
    catch (SecurityException e) { throw new U.RunException(e.getMessage()); }
  }


  private LocationListener mLocationListener = new LocationListener() {

    @Override
    public void onLocationChanged(Location loc) {
      if (U.DEBUG) Log.i(U.TAG, "mLocationListener:"+"got a location " + loc.toString());
      mFixCount += 1;
      mPi.showData(locToDisplay(loc, mFixCount));
      mStatus = isAcceptable(loc, mFixCount);
      if (!mStatus.equals("overcount") && !mStatus.equals("converged")) return;
      mFixCount = 0;
      mPoint = new Point("gps", loc);
      mLocnManager.removeUpdates(this);
      mPi.addProgress(mStatus);
      onPointavailable(mPoint);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
      if ( ! provider.equals(LocationManager.GPS_PROVIDER)) return;
      if (U.DEBUG) Log.i(U.TAG, "got new GPS status:"+String.valueOf(status));
      //mPi.showData( "GPS status:" + String.valueOf(status));
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
  };

  private String locToDisplay(Location loc, int count) {
    String s="";
    s+=String.valueOf(count)+".";
    s+="lat="+U.truncate(loc.getLatitude(), 10)+", lon="+U.truncate(loc.getLongitude(), 10);
    if (loc.hasAltitude()) s+=", alt="+U.truncate(loc.getAltitude(), 6);
    if (loc.hasAccuracy()) s+=" Accuracy="+String.valueOf(loc.getAccuracy());
    return s;
  }
  
  private String isAcceptable(Location loc, int count) {
    String s="not ready";
    if (count >= mGpsMaxFixCount) { s="overcount"; }
    else if (loc.hasAccuracy() && loc.getAccuracy() <= mGpsAcceptableAccuracy) { s="converged"; }
    return s;
  }

  /*
  private void giveMockLocation() {
    // https://stackoverflow.com/questions/38251741/how-to-set-android-mock-gps-location
    
    //LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    //Criteria criteria = new Criteria();
    //criteria.setAccuracy( Criteria.ACCURACY_FINE );
    String mocLocationProvider = LocationManager.GPS_PROVIDER;//lm.getBestProvider( criteria, true );

    if ( mocLocationProvider == null ) {
        mPi.addProgress("No location provider found!");
        return;
    }
    mLocnManager.addTestProvider(mocLocationProvider, false, false,
            false, false, true, true, true, 0, 5);
    mLocnManager.setTestProviderEnabled(mocLocationProvider, true);

    Location mockLocation = new Location(mocLocationProvider); // a string
    mockLocation.setLatitude(-26.902038);  // double
    mockLocation.setLongitude(-48.671337);
    mockLocation.setAltitude(234.0);
    mockLocation.setTime(System.currentTimeMillis());
    mockLocation.setAccuracy(3);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
    }
    mLocnManager.setTestProviderLocation(mocLocationProvider, mockLocation);
    if (U.DEBUG) Log.d(U.TAG,"Giving mock gps location");
  }*/
}
