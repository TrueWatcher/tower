package truewatcher.tower;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import android.os.AsyncTask;
import android.util.Log;

//@link  https://stackoverflow.com/questions/9767952/how-to-add-parameters-to-httpurlconnection-using-post-using-namevaluepair
public class HttpPostRequest extends AsyncTask<String, Void, String> {
  private HttpReceiver receiver;

  public HttpPostRequest(HttpReceiver rec) {
    receiver=rec;
  }
  public static String makePostDataString( Map<String, String> params ) throws UnsupportedEncodingException{
    StringBuilder result = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> entry : params.entrySet()){
      if (first) { first = false; }
      else { result.append("&"); }
      result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
      result.append("=");
      result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
    }
    return result.toString();
  }

  @Override 
  protected String doInBackground(String... params) {

    String stringUrl = params[0];
    String postDataString = params[1];
    String response = "";
    URL url;

    try {
      url = new URL(stringUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setReadTimeout(15000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("POST");
      conn.setDoInput(true);
      conn.setDoOutput(true);

      OutputStream os = conn.getOutputStream();
      BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(os, "UTF-8"));
      writer.write(postDataString);
      writer.flush();
      writer.close();
      os.close();
      int responseCode=conn.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        String line;
        BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line=br.readLine()) != null) { response+=line; }
      }
      else { response=""; }
    }
    catch (Exception e) {
      e.printStackTrace();
      Log.e(U.TAG,"HttpPostRequest:"+e.getMessage());
    }

    return response;
  }

  protected void onPostExecute(String result){
    super.onPostExecute(result);
    receiver.onHttpReceived(result);
  }
  
  

}
