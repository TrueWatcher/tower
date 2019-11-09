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
  private boolean mDirty=false;
  
  @android.webkit.JavascriptInterface
  public String importLonLat() { return mCenterLon+","+mCenterLat; }
  
  @android.webkit.JavascriptInterface
  public String importLatLon() { return mCenterLat+","+mCenterLon; }
  
  public void exportLatLon(String lat,String lon) { 
    mCenterLon=lon;
    mCenterLat=lat;
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
  
  public void exportZoom(String z) { mZoom=z; }
  
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
    String empty=(new JSONArray()).toString();
    JSONArray jo;
    Point loc=Model.getInstance().lastPosition;
    if (loc == null || ! loc.hasCoords()) return empty;
    String[] ar={loc.getType(), loc.lat, loc.lon};
    try {
      jo=new JSONArray(ar);
      return jo.toString();
    }
    catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }  
    return empty;
  }
  
  public void setPointList(PointList pl) { mPointList=pl; }
  
  @android.webkit.JavascriptInterface
  public String getMarkers() { 
    return mPointList.makeJsonPresentation();
  }
  
  public boolean isDirty() { return mDirty; }
  public void setDirty() { mDirty=true; }
  public void clearDirty() { mDirty=false; }
  
}
