package truewatcher.tower;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

interface CellResolver {
  public String makeResolverUri(JSONObject celldata);
  public String makeResolverData(JSONObject celldata) throws U.DataException;
  public AsyncTask<String, Void, String> getRequestTask(HttpReceiver receiver);
  public JSONObject getResolvedData(String response) throws U.DataException;
}

public class CellResolverFactory {
  public static CellResolver getResolver(String which) {
    if (which.equals("mylnikov")) return new MylnikovResolver();
    if (which.equals("yandex")) return new YandexResolver();
    Log.e(U.TAG,"CellInformer:"+"Wrong WHICH="+which);
    return null;
  }
  
  private static class MylnikovResolver implements CellResolver {
    
    public String makeResolverUri(JSONObject cellData) {     
      String resolverServiceBase="api.mylnikov.org/geolocation/cell";
      String lacTac = cellData.has("TAC" ) ? "TAC" : "LAC";
      // https://api.mylnikov.org/geolocation/cell?v=1.1&mcc=250&mnc=02&cellid=200719106&lac=7840
      //long cid=cellData.optLong("CID");
      //cid=cid & 0xffff;
      //String scid = String.valueOf(cid);
      String resolverUri = Uri.parse(U.H+resolverServiceBase)
        .buildUpon()
        .appendQueryParameter("v", "1.1")
        .appendQueryParameter("data", "open")
        .appendQueryParameter("mcc", cellData.optString("MCC"))
        .appendQueryParameter("mnc", cellData.optString("MNC"))
        .appendQueryParameter("lac", cellData.optString(lacTac))
        .appendQueryParameter("cellid", cellData.optString("CID"))
        .build().toString();
      return resolverUri;
    }
    
    public String makeResolverData(JSONObject celldata) { return ""; }
    
    public AsyncTask<String, Void, String> getRequestTask(HttpReceiver receiver) {
      return new HttpGetRequest(receiver);
    }
    
    public JSONObject getResolvedData(String response) throws U.DataException {
      JSONObject rd=new JSONObject();
      if (response == null || response.length() == 0) { throw new U.DataException("No response"); }
      try {
        rd=new JSONObject(response);
        int respCode=rd.optInt("result");
        if (respCode != 200) { throw new U.DataException("Resolver failure, code="+respCode); }
        rd=rd.getJSONObject("data");
      }
      catch (JSONException e) { throw new U.DataException("Unparseble response"); }
      String lat=String.valueOf(rd.opt("lat"));
      String lon=String.valueOf(rd.opt("lon"));
      if (lat.indexOf(".") < 0 || lon.indexOf(".") < 0) {
        throw new U.DataException("Wrong lat or lon:"+lat+"/"+lon);
      }
      return rd;
    }
  }
  
  private static class YandexResolver implements CellResolver {
    
    public String makeResolverUri(JSONObject cellData) {
      return U.H+"api.lbs.yandex.net/geolocation";
    }
    
    public String makeResolverData(JSONObject cellData) throws U.DataException {
      String key=MyRegistry.getInstance().getScrambled("yandexLocatorKey");
      //Log.d(U.TAG, "CellResolverFactory"+"locator key:"+MyRegistry.getInstance().getScrambled("yandexLocatorKey"));
      if (key.isEmpty()) { throw new U.DataException("missing API key"); }
      String r="json={";
      r+="\"common\":{\"version\":\"1.0\", \"api_key\":\""+key+"\"}";
      r+=", ";
      JSONObject data=new JSONObject();
      String lacTac = cellData.has("TAC" ) ? "TAC" : "LAC";
      try {
        data.put("countrycode",cellData.optInt("MCC"));
        data.put("operatorid",cellData.optInt("MNC"));
        data.put("lac",cellData.optLong(lacTac));
        data.put("cellid",cellData.optLong("CID"));
      }
      catch (JSONException e) {
        Log.e(U.TAG, "makeResolverData:"+e.getMessage());
      }
      r+="\"gsm_cells\":["+data.toString()+"]";
      r+="}";    
      return r;
    }
    
    public AsyncTask<String, Void, String> getRequestTask(HttpReceiver receiver) {
      return new HttpPostRequest(receiver);
    }
    
    public JSONObject getResolvedData(String response) throws U.DataException {
      JSONObject rd=new JSONObject();
      JSONObject res=new JSONObject();
      if (response == null || response.length() == 0) { throw new U.DataException("No response"); }
      try {
        rd=new JSONObject(response);
        String error=rd.optString("error");
        if (error != null && ! error.isEmpty()) { throw new U.DataException("Resolver failure, error="+error); }
        rd=rd.getJSONObject("position");
      }
      catch (JSONException e) { throw new U.DataException("Unparseble response"); }
      String lat=String.valueOf(rd.opt("latitude"));
      String lon=String.valueOf(rd.opt("longitude"));
      if (lat.indexOf(".") < 0 || lon.indexOf(".") < 0) {
        throw new U.DataException("Wrong lat or lon:"+lat+"/"+lon);
      }
      String range=String.valueOf(rd.opt("precision"));
      try {
        res.put("lat",lat);
        res.put("lon",lon);
        if ( range != null && ! range.isEmpty()) res.put("range",range);
      }
      catch (JSONException e) { throw new U.DataException("Unrecodable response"); }
      return res;
    }     
  }
}
