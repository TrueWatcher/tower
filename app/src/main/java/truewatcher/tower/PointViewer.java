package truewatcher.tower;

import android.widget.TextView;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PointViewer extends PointIndicator implements PointReceiver {

  private MyRegistry mRegistry=MyRegistry.getInstance();
  private WebView wvWebView;
  private String mPage;
  
  public PointViewer(TextView twP, TextView twD, WebView wvW) {    
    super(twP, twD);
    wvWebView=wvW;
    wvWebView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        twData.setText("Blocked loading "+url);
        return true;// false will do loading
      }
    });
    WebSettings webSettings = wvWebView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setAllowContentAccess(false);
    webSettings.setAllowFileAccessFromFileURLs(false);
  }
  
  @Override
  public void onPointavailable(Point p) { 
    showMap(p);
  }
  
  public void showMap(Point p) {
    if( ! p.hasCoords()) {
      if (U.DEBUG) Log.d(U.TAG,"PointViewer:"+"empty point, no map shown");
      return;
    }
    showMap();
  }
  
  public void showMap() {
    mPage=choosePage(mRegistry.get("mapProvider"));
    if (mPage == "") {
      addProgress("Wrong mapProvider");
      return;
    }
    JSbridge jsb=Model.getInstance().getJSbridge();
    wvWebView.addJavascriptInterface(jsb, "JSbridge");
    wvWebView.loadUrl(mPage);
  }
  
  public void redraw() {
    JSbridge jsb=Model.getInstance().getJSbridge();
    if (jsb.importLatLon().contains("null")) {// no map center
      showWallpaper();
      return;
    }
    hideProgress();
    hideData();
    //addData("redraw");
    // mPage is re-created with this object
    showMap();
  }
  
  private String choosePage(String mapProvider) {
    final String yandexPage="file:///android_asset/webMaps/ya.html";
    //final String yastaticPage="file:///android_asset/webMaps/yastatic.html";;
    final String leafletjsPage="file:///android_asset/webMaps/leafletjs.html";
    //final String redirectPage="http://fs.posmotrel.net/jsredir.html";
    String[] pt=mapProvider.split(" ");
    if (pt.length != 2) throw new U.RunException("Wrong mapProvider="+mapProvider);
    if (pt[0].equals("osm") || pt[0].equals("google") ) { return leafletjsPage; }
    else if (pt[0].equals("yandex")) { return yandexPage; }
    return "";
  }

  private void showWallpaper() {
    final String wallpaperPage="file:///android_asset/webMaps/wallpaper.html";
    wvWebView.loadUrl(wallpaperPage);
  }
}
