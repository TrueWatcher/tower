package truewatcher.tower;

import android.widget.TextView;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MapViewer extends PointIndicator implements PointReceiver {

  private MyRegistry mRegistry=MyRegistry.getInstance();
  private WebView wvWebView;
  private String mPageURI;
  private JSbridge mJSbridge = Model.getInstance().getJSbridge();
  
  public MapViewer(TextView tvP, TextView tvD, WebView wvW) {
    super(tvP, tvD);
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
    redraw();
  }

  public void redraw() {
    if (mJSbridge.hasNoCenter()) {
      if (U.DEBUG) Log.d(U.TAG,"Show wallpaper from redraw");
      showWallpaper();
      return;
    }
    int c=mJSbridge.isDirty();
    switch (c) {
      case 3:
        showMap();
        break;
      case 2:
        reloadData();
        break;
      case 1:
        reloadTrack();
        break;
      case 0:
        break;
      default:
        throw new U.RunException("Unknown JSbribge dirty="+Integer.toString(c));
    }
  }

  public void showMap() {
    if (mJSbridge.hasNoCenter()) {
      if (U.DEBUG) Log.d(U.TAG,"Show wallpaper from showMap");
      showWallpaper();
      return;
    }
    mPageURI=choosePage(mRegistry.get("mapProvider"));
    wvWebView.addJavascriptInterface(mJSbridge, "JSbridge");
    wvWebView.loadUrl(mPageURI);
    mJSbridge.clearDirty(3);
  }
  
  private String choosePage(String mapProvider) {
    final String yandexPage="file:///android_asset/webMaps/ya.html";
    final String leafletjsPage="file:///android_asset/webMaps/leafletjs.html";
    String[] pt=mapProvider.split(" ");
    if (pt.length != 2) throw new U.RunException("Wrong mapProvider="+mapProvider);
    if ( isLeaflet(pt[0]) ) { return leafletjsPage; }
    else if (pt[0].equals("yandex")) { return yandexPage; }
    throw new U.RunException("Wrong mapProvider="+mapProvider);
  }

  private boolean isLeaflet(String provider) {
    final String[] known=new String[] {"osm","opentopo","blank","google"};
    return U.arrayContains(known,provider);
  }

  private void showWallpaper() {
    final String wallpaperPage="file:///android_asset/webMaps/wallpaper.html";
    wvWebView.loadUrl(wallpaperPage);
  }

  private void reloadTrack() {
    String reloadTrack="(function() { window.dispatchEvent(onTrackreloadEvent); })();";
    wvWebView.evaluateJavascript(reloadTrack,null);
    mJSbridge.clearDirty(1);
  }

  private void reloadData() {
    String reloadData="(function() { window.dispatchEvent(onDatareloadEvent); })();";
    wvWebView.evaluateJavascript(reloadData,null);
    mJSbridge.clearDirty(2);
  }

}
