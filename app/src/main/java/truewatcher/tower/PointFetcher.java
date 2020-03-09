package truewatcher.tower;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

interface PointReceiver {
  public void onPointavailable(Point p);
}

public abstract class PointFetcher implements PermissionReceiver {
  protected FragmentActivity mActivity;
  protected PermissionAwareFragment mFragm;
  protected PointIndicator mPi;
  protected PointReceiver mPointReceiver;
  protected String mStatus="not run";
  protected Point mPoint=null;
  protected boolean mToUpdateLocation=true;
  
  abstract String getPermissionType();
  abstract int getPermissionCode();
  
  public String getStatus() { return mStatus; }
  
  public Point getPoint() { return mPoint; }
  
  public void setFragment(PermissionAwareFragment f) {
    mFragm=f;
    mActivity=f.getActivity();
  }
  
  public void clearFragment() {
    mFragm=null;
    mActivity=null;
  }
  
  protected boolean tryGiveMockLocation() { return false; }
  
  public void go(PointIndicator pi, PointReceiver pr) {
    mPi=pi;
    mPointReceiver=pr;
    if (tryGiveMockLocation()) return;
    mPi.initProgress();
    //mPi.addProgress("Checking permission...");
    if (U.DEBUG) Log.d(U.TAG, "Checking permission...");
    boolean hasLoctnPerm=checkLocalPermission();
    if ( ! hasLoctnPerm) {
      if (U.DEBUG) Log.d(U.TAG, "negative, asking user...");
      //mPi.addProgress("negative, asking user...");
      requestPermission();
      return;
    }
    if (U.DEBUG) Log.d(U.TAG, "positive");
    //mPi.addProgress("positive");
    afterLocationPermissionOk();
  }
  
  @TargetApi(23)
  private boolean checkLocalPermission() {
    if (mActivity == null) {
      Log.e(U.TAG, "checkLocalPermission:"+"No activity attached");
      return false;
    }
    int cl=mActivity.checkSelfPermission(getPermissionType());
    if (U.DEBUG) Log.d(U.TAG,"cl="+cl+"/"+PackageManager.PERMISSION_GRANTED);
    return (cl == PackageManager.PERMISSION_GRANTED);
  }
  
  private void requestPermission() {
    mFragm.genericRequestPermission(getPermissionType(), getPermissionCode(), this);
  } 
  
  public void receivePermission(int reqCode, boolean isGranted) {
    if ( ! isGranted) {
      if (U.DEBUG) Log.d(U.TAG, "denied");
      mPi.addProgress("denied");
      mStatus="denied";
      return;
    }
    mPi.addProgress("granted");
    afterLocationPermissionOk();
  }
  
  abstract void afterLocationPermissionOk();
  
  protected void onPointavailable(Point p) {
    if ( p.hasCoords() ) mPi.hideProgress();// if it's an unresolved cell - keep progress visible
    p.setCurrentTime();
    Model mm = Model.getInstance();
    if (p.getType().equals("cell")) mm.lastCell=p;
    else if (p.getType().equals("gps")) mm.lastGps=p;
    if (mToUpdateLocation && p.hasCoords()) {
      mm.lastPosition=p;
      mm.getPointList().setProximityOrigin(p);
      mm.getJSbridge().consumeLocation(p);
    }
    mPointReceiver.onPointavailable(p);
  }
}
