package truewatcher.tower;

import org.json.JSONArray;
import org.json.JSONException;
import android.util.Log;

public class JSbridge {
  private MyRegistry mRegistry=MyRegistry.getInstance();
  private String mZoom;
  private String mCenterLon;
  private String mCenterLat;
  private PointList mPointList;
  private int mDirty=3;// load map on first use
  private String mViewTrackLatLonJson="[]";
  private String mCurrentTrackLatLonJson="[]";
  
  @android.webkit.JavascriptInterface
  public String importLonLat() { return mCenterLon+","+mCenterLat; }
  
  @android.webkit.JavascriptInterface
  public String importLatLon() { return mCenterLat+","+mCenterLon; }
  
  public void exportLatLon(String lat,String lon) { 
    mCenterLon=lon;
    mCenterLat=lat;
  }

  public void exportLatLon(LatLon p) {
    if ( ! p.hasCoords()) return;
    mCenterLon=p.lon;
    mCenterLat=p.lat;
  }
  
  @android.webkit.JavascriptInterface
  public void exportCenterLatLon(String lat,String lon) { 
    mCenterLon=lon;
    mCenterLat=lat;
  }
  
  public String importCenterLatLon() { 
    if (mCenterLat == null || mCenterLon == null) return "";
    return mCenterLat+","+mCenterLon;
  }
  
  @android.webkit.JavascriptInterface
  public String importZoom() { 
    if (mZoom == null) mZoom=mRegistry.get("mapZoom");
    return mZoom;
  }
  
  @android.webkit.JavascriptInterface
  public void saveZoom(String z) { mZoom=z; }
  
  public void exportZoom(String z) {
    mZoom=z;
    setDirty(2);
  }
  
  @android.webkit.JavascriptInterface
  public String importMapType() { 
    return mRegistry.get("mapProvider");
  }
  
  @android.webkit.JavascriptInterface
  public String getKey() { 
    boolean isYandex=mRegistry.get("mapProvider").indexOf("yandex") == 0;
    if (isYandex) {
      //Log.d(U.TAG,"JSbridge:"+"map key:"+MyRegistry.getInstance().getScrambled("yandexMapKey"));
      return mRegistry.getScrambled("yandexMapKey");
    }
    return "";
  }
  
  @android.webkit.JavascriptInterface
  public String getNamelessMarker() {
    Point loc=Model.getInstance().lastPosition;
    if (loc == null || ! loc.hasCoords()) return (new JSONArray()).toString();
    return loc.makeJsonPresentation(0).toString();
  }

  @android.webkit.JavascriptInterface
  public String importViewTrackLatLonJson() { return mViewTrackLatLonJson; }

  public void addViewTrackLatLonJson(String json) {
    mViewTrackLatLonJson=U.joinJsonArrays(mViewTrackLatLonJson,json);
    setDirty(2);
  }

  @android.webkit.JavascriptInterface
  public String importCurrentTrackLatLonJson() { return mCurrentTrackLatLonJson; }

  public void replaceCurrentTrackLatLonJson(String json) {
    mCurrentTrackLatLonJson=json;
    setDirty(1);
  }

  public void consumeLocation(Point p) {
    if (p == null || ! p.hasCoords()) return;
    exportLatLon(p.lat,p.lon);
    setDirty(2);
  }

  public void consumeTrackpoint(Trackpoint p) {
    if ( ! mRegistry.getBool("enableTrack")) return;
    if (p == null || ! p.getType().equals("T")) return;
    String ll=p.makeJsonPresentation().toString();
    if (U.DEBUG) Log.d(U.TAG, "consumeTrackpoint:" + "Adding:" + ll);
    mCurrentTrackLatLonJson = StorageHelper.append2LatLonString(ll, p.isNewSegment(), mCurrentTrackLatLonJson);
    //Log.d(U.TAG, "consumeTrackpoint:" + "Result:" + mCurrentTrackLatLonJson);
    if (mRegistry.getBool("shouldCenterMapOnTrack")) {
      exportLatLon(p);
    }
    setDirty(1);
  }

  @android.webkit.JavascriptInterface
  public boolean importViewCurrentTrack() { return mRegistry.getBool("enableTrack"); }

  @android.webkit.JavascriptInterface
  public boolean importFollowCurrentTrack() { return mRegistry.getBool("shouldCenterMapOnTrack"); }
  
  public void setPointList(PointList pl) { mPointList=pl; }

  public void onPoinlistmodified() { setDirty(2); }
  
  @android.webkit.JavascriptInterface
  public String getMarkers() { 
    return mPointList.makeJsonPresentation();
  }
  
  public int isDirty() { return mDirty; }

  public void setDirty(int level) {
    // 0 - clean, 1 - track, 2 - data, 3 - URI
    if (level > mDirty) mDirty=level;
  }
  public void clearDirty(int level) {
    if (level < mDirty) throw new U.RunException("Wrong clearDirty level="+level+", required="+mDirty);
    mDirty=0;
  }

  public boolean hasNoCenter() { return ( mCenterLat == null || mCenterLat.isEmpty() ); }

}
