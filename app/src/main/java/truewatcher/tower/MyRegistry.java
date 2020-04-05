package truewatcher.tower;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

// A Singleton registry to keep all parameters
public class MyRegistry {
	private static MyRegistry sMe;
	private static Map<String,String> sMap;
	
	public static void initMap() throws JSONException {
	  String d=getDefaultsString();
    Map<String,String> defaults=JsonHelper.toMapSS(new JSONObject(d));
	  sMap=new HashMap<String,String>(defaults);
	}
	
	public static String toJson() {
	  return JsonHelper.MapssToJSONString(sMap);
	}
	
	public static MyRegistry getInstance() {
		if(sMe == null) {
		  sMe=new MyRegistry();
		  try {
        MyRegistry.initMap();
      }
		  catch (JSONException e) {
        Log.e(U.TAG,"MyRegistry:"+e.toString());
      }
		}
		return sMe;
	}
  
  public String get(String key) {
    if ( ! sMap.keySet().contains(key)) throw new U.RunException("Unknown key="+key);
    return sMap.get(key); 
  }
  
  public int getInt(String key) {
    if ( ! sMap.keySet().contains(key)) throw new U.RunException("Unknown key="+key);
    return Integer.parseInt(sMap.get(key));     
  }
  
  public boolean getBool(String key) {
    if ( ! sMap.keySet().contains(key)) throw new U.RunException("Unknown key="+key);
    return Boolean.parseBoolean(sMap.get(key));     
  }
  
  public String getScrambled(String key) {
    int[] transpose={2,3,5,6,8,9,13,19,25,31};
    char[] ca=sMap.get(key).trim().toCharArray();
    int l=ca.length;
    int lHalf=(int) Math.floor(l/2.);
    char c;
    int sourceIndex,targetIndex;

    if (l < 3) return new String(ca);
    for (int i=0; i < transpose.length; i+=1) {
      sourceIndex=transpose[i];
      if (sourceIndex >= lHalf) break;
      targetIndex=l-sourceIndex;
      c=ca[sourceIndex];
      ca[sourceIndex]=ca[targetIndex];
      ca[targetIndex]=c;
    }
    return new String(ca);
  }
  
  public void set(String key, String val) {
    if ( ! sMap.keySet().contains(key)) throw new U.RunException("Unknown key="+key);
    sMap.put(key,val); 
  }
  
  public void set(String key, boolean val) {
    if ( ! sMap.keySet().contains(key)) throw new U.RunException("Unknown key="+key);
    sMap.put(key, String.valueOf(val)); 
  }
  
  public void set(String key, int val) {
    if ( ! sMap.keySet().contains(key)) throw new U.RunException("Unknown key="+key);
    sMap.put(key, String.valueOf(val)); 
  }
  
  public void set(String key, Object val) {
    if ( ! sMap.keySet().contains(key)) throw new U.RunException("Unknown key="+key);
    sMap.put(key,val.toString()); 
  }
  
  public void setInt(String key, int val) {
    if ( ! sMap.keySet().contains(key)) throw new U.RunException("Unknown key="+key);
    sMap.put(key, String.valueOf(val)); 
  }
    
  public void setBool(String key, boolean val) {
    if ( ! sMap.keySet().contains(key)) throw new U.RunException("Unknown key="+key);
    sMap.put(key, String.valueOf(val)); 
  }
  
  public boolean keyExists(String key) {
    return sMap.keySet().contains(key);
  }
  
  private static String getDefaultsString() {
    String defs="{\"cellResolver\":\"mylnikov\",\"mapProvider\":\"osm map\",\"mapZoom\":\"17\",\"maxPoints\":\"30\","
    + "\"useTrash\":\"false\",\"gpsAcceptableAccuracy\":\"8\",\"gpsMaxFixCount\":\"10\","
    + "\"myFile\":\"current.csv\","    
    + "\"yandexMapKey\":\"\", \"yandexLocatorKey\":\"\", \"isKeylessDistro\":\"false\","
    + "\"gpsMinDistance\":\"12\",\"gpsMinDelayS\":\"10\",\"gpsTimeoutS\":\"120\","
    + "\"enableTrack\":\"true\",\"shouldCenterMapOnTrack\":\"true\",\"useTowerFolder\":\"false\""
    + "}";
    return defs;
  }

  public static final String[] INT_KEYS = new String[] {
          "mapZoom", "maxPoints", "gpsMinDistance", "gpsMinDelayS"} ;

  public static final String[] APIS = new String[] { "yandexMapKey","yandexLocatorKey" };
  
  public void readFromShared(Context context) {
    String k;
    String v;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    for (Map.Entry<String,String> e : MyRegistry.sMap.entrySet()) {
      k=e.getKey();
      if ( prefs.contains(k) ) {
        v=String.valueOf(prefs.getAll().get(k));
        this.set(k, U.enforceInt(MyRegistry.INT_KEYS, k, v));
      }
    }    
  }
  
  public void saveToShared(Context context, String key) {
    if ( ! sMap.keySet().contains(key)) throw new U.RunException("Unknown key="+key);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(key, sMap.get(key)).commit();          
  }

  private void syncSecret(Context context, String key, String assetFileName) {
    String s="";

    if (sMap.get(key).length() > 3) return; // already synced
    // look in BuildConfig
    if (U.classHasField(BuildConfig.class, key)) {
      try {
        this.set(key, Class.forName("BuildConfig").getField(key));
        this.set(key, this.getScrambled(key));
        this.saveToShared(context, key);
        if (U.DEBUG) Log.d(U.TAG, "MyRegistry:"+" found a key in buildconfig: "+key);
        return;
      }
      catch (ClassNotFoundException e) { throw new U.RunException("This should not happen 1"); }
      catch (NoSuchFieldException e) { throw new U.RunException("This should not happen 2"); }
    }

    // look in assets
    try {
      s=U.readAsset(context, assetFileName).trim();
      this.set(key,s);
      this.saveToShared(context, key);
    }
    catch (IOException e) {
      if (U.DEBUG) Log.d(U.TAG, "MyRegistry:"+"Missing "+assetFileName);
    }
  }

  public void syncSecrets(Context context) {
    syncSecret(context, "yandexMapKey", "_yandexmap.txt");
    syncSecret(context, "yandexLocatorKey", "_yandexlocator.txt");
    if (notAllKeys()) {
      this.set("isKeylessDistro", true);
      this.saveToShared(context, "isKeylessDistro");
      if (U.DEBUG) Log.d(U.TAG, "MyRegistry:"+"This is a distro without API keys");
    }
  }

  public boolean noAnyKeys() {
    int i=0;
    String k;
    for (i=0; i < APIS.length; i+=1) {
      if ( ! sMap.get(APIS[i]).isEmpty()) return false;
    }
    return true;
  }

  public boolean notAllKeys() {
    int i=0;
    String k;
    for (i=0; i < APIS.length; i+=1) {
      if (sMap.get(APIS[i]).isEmpty()) return true;
    }
    return false;
  }
}
