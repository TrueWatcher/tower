package truewatcher.signaltrackwriter;

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
  private static Map<String, String> sMap;

  public static void initMap() throws JSONException {
    String d = getDefaultsString();
    Map<String, String> defaults = JsonHelper.toMapSS(new JSONObject(d));
    sMap = new HashMap<String, String>(defaults);
  }

  public static String toJson() {
    return JsonHelper.MapssToJSONString(sMap);
  }

  public static MyRegistry getInstance() {
    if (sMe == null) {
      sMe = new MyRegistry();
      try {
        MyRegistry.initMap();
      }
      catch (JSONException e) {
        Log.e(U.TAG, "MyRegistry:" + e.toString());
      }
    }
    return sMe;
  }

  public String get(String key) {
    if (!sMap.keySet().contains(key)) throw new U.RunException("Unknown key=" + key);
    return sMap.get(key);
  }

  public int getInt(String key) {
    if (!sMap.keySet().contains(key)) throw new U.RunException("Unknown key=" + key);
    String s=sMap.get(key);
    if (s.isEmpty()) return 0;
    return Integer.parseInt(s);
  }

  public boolean getBool(String key) {
    if (!sMap.keySet().contains(key)) throw new U.RunException("Unknown key=" + key);
    return Boolean.parseBoolean(sMap.get(key));
  }

  public void set(String key, String val) {
    if (!sMap.keySet().contains(key)) throw new U.RunException("Unknown key=" + key);
    sMap.put(key, val);
  }

  public void set(String key, boolean val) {
    if (!sMap.keySet().contains(key)) throw new U.RunException("Unknown key=" + key);
    sMap.put(key, String.valueOf(val));
  }

  public void set(String key, int val) {
    if (!sMap.keySet().contains(key)) throw new U.RunException("Unknown key=" + key);
    sMap.put(key, String.valueOf(val));
  }

  public void set(String key, Object val) {
    if (!sMap.keySet().contains(key)) throw new U.RunException("Unknown key=" + key);
    sMap.put(key, val.toString());
  }

  public void setInt(String key, int val) {
    if (!sMap.keySet().contains(key)) throw new U.RunException("Unknown key=" + key);
    sMap.put(key, String.valueOf(val));
  }

  public void setBool(String key, boolean val) {
    if (!sMap.keySet().contains(key)) throw new U.RunException("Unknown key=" + key);
    sMap.put(key, String.valueOf(val));
  }

  public boolean keyExists(String key) {
    return sMap.keySet().contains(key);
  }

  private static String getDefaultsString() {
    String defs = "{\"gpsMinDistance\":\"8\",\"gpsMinDelayS\":\"5\",\"trackShouldWrite\":\"true\","
            + "\"gpsTimeoutS\":\"120\",\"gpsAcceptableAccuracy\":\"8\",\"useTowerFolder\":false\"\""
            + "}";
    return defs;
  }

  public void readFromShared(Context context) {
    String k;
    String v;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    for (Map.Entry<String, String> e : MyRegistry.sMap.entrySet()) {
      k = e.getKey();
      if (prefs.contains(k)) {
        v = String.valueOf(prefs.getAll().get(k));
        this.set(k, v);
      }
    }
  }

  public void saveToShared(Context context, String key) {
    if (!sMap.keySet().contains(key)) throw new U.RunException("Unknown key=" + key);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(key, sMap.get(key)).commit();
  }

}
