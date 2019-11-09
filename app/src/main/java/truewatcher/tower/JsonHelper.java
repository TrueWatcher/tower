package truewatcher.tower;

//@link https://gist.githubusercontent.com/codebutler/2339666/raw/f036bc29033bdd6478956f3d3bbeef16acb0ecd3/JsonHelper.java
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

@SuppressWarnings("rawtypes")
public class JsonHelper {
  public static Object toJSON(Object object) throws JSONException {
    if (object instanceof Map) {
      JSONObject json = new JSONObject();
      Map map = (Map) object;
      for (Object key : map.keySet()) {
        json.put(key.toString(), toJSON(map.get(key)));
      }
      return json;
    } else if (object instanceof Iterable) {
      JSONArray json = new JSONArray();
      for (Object value : ((Iterable)object)) {
        json.put(value);
      }
      return json;
    } else {
      return object;
    }
  }

  public static boolean isEmptyObject(JSONObject object) {
    return object.names() == null;
  }

  public static Map<String, Object> getMap(JSONObject object, String key) throws JSONException {
    return toMap(object.getJSONObject(key));
  }

  public static Map<String, Object> toMap(JSONObject object) throws JSONException {
    Map<String, Object> map = new HashMap<String, Object>();
    Iterator keys = object.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      map.put(key, fromJson(object.get(key)));
    }
    return map;
  }
  
  @SuppressWarnings("unchecked")
  public static List toList(JSONArray array) throws JSONException {
    List list = new ArrayList();
    for (int i = 0; i < array.length(); i++) {
      list.add(fromJson(array.get(i)));
    }
    return list;
  }

  private static Object fromJson(Object json) throws JSONException {
    if (json == JSONObject.NULL) {
      return null;
    } else if (json instanceof JSONObject) {
      return toMap((JSONObject) json);
    } else if (json instanceof JSONArray) {
      return toList((JSONArray) json);
    } else {
      return json;
    }
  }
  
  public static Map<String,String> toMapSS(JSONObject object) throws JSONException {
    Map<String, Object> mapSO=JsonHelper.toMap(object);
    Map<String, String> mapSS = new HashMap<String, String>();
    for (Map.Entry<String, Object> entry : mapSO.entrySet()) {
      mapSS.put( entry.getKey(), Objects.toString(entry.getValue(),"<not a string>") );
    }
    return mapSS;    
  }
  
  public static String MapssToJSONString(Map<String, String> mapSS) {
    return new JSONObject(mapSS).toString();
  }
  
  public static String filterQuotes(String js) {
    String r = js.replace("\"","");
    r = r.replaceAll("[{}]","");
    return r;
  }
}
