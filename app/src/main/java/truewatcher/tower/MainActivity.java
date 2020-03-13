package truewatcher.tower;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

public class MainActivity extends SingleFragmentActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      Intent si=new Intent(this,PreferencesActivity.class);
      startActivity(si);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public static class MainPageFragment extends PermissionAwareFragment
          implements TrackListener.TrackPointListener {
    private MyRegistry mRegistry=MyRegistry.getInstance();
    private MapViewer mMapViewer;
    private Model mModel = Model.getInstance();
    private CellInformer mCellInformer = mModel.getCellInformer();;
    private GpsInformer mGpsInformer = mModel.getGpsInformer();;
    private PointList mPointList = mModel.getPointList();
    private JSbridge mJSbridge = mModel.getJSbridge();
    private TrackListener mTrackListener = mModel.getTrackListener();
    private U.Summary mReadPoints=null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      if (U.DEBUG) Log.i(U.TAG,"mainFragment:onCreate");
      setHasOptionsMenu(true);

      //if (U.DEBUG) U.clearPrefs(getActivity()); // DEBUG

      mRegistry.readFromShared(getActivity());
      mRegistry.syncSecrets(getActivity());
      mPointList.adoptMax(mRegistry.getInt("maxPoints"));
      loadStoredPoints();
      loadCurrentTrack();
      mCellInformer.setFragment(this);
      mGpsInformer.setFragment(this);
    }

    private void loadStoredPoints() {
      mReadPoints=null;
      if ( ! mPointList.isEmpty()) return;
      mModel.getStorageHelper().init(this.getActivity(), mRegistry.get("myFile"));
      try {
        mReadPoints=mPointList.load();
        if (U.DEBUG) Log.d(U.TAG,"MainPageFragment:"+ "Loaded "+mReadPoints.adopted+" points");
      }
      catch (Exception e) {
        Log.e(U.TAG,"MainPageFragment:"+e.getMessage());
      }
    }

    private void loadCurrentTrack() {
      try {
        TrackStorage ts=mModel.getTrackStorage();
        ts.initTargetDir(getActivity());
        if ( ! mRegistry.getBool("enableTrack")) return;
        String buf=ts.trackCsv2LatLonString();
        mJSbridge.replaceCurrentTrackLatLonJson(buf);
      }
      catch (Exception e) {
        Log.e(U.TAG,"MainPageFragment:"+e.getMessage());
      }
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      if (U.DEBUG) Log.i(U.TAG,"mainFragment:onCreateView");
      View v = inflater.inflate(R.layout.fragment_main, container, false);
      TextView mTwA = (TextView) v.findViewById(R.id.twA);
      TextView mTwB = (TextView) v.findViewById(R.id.twB);
      WebView mWebView = (WebView) v.findViewById(R.id.wvWebView);
      mMapViewer =new MapViewer(mTwA, mTwB, mWebView);
      //mMapViewer.redraw(); causes empty webView
      mMapViewer.hideIndicator();
      mMapViewer.showMap();
      return v;
    }

    @Override
    public void onTrackpointAvailable(Trackpoint p) {
      if ( ! mRegistry.getBool("enableTrack")) return;
      mMapViewer.redraw();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.main_fragment, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_cell) {
        mCellInformer.go(mMapViewer, mMapViewer);
        return true;
      }
      if (id == R.id.action_gps) {
        mGpsInformer.go(mMapViewer, mMapViewer);
        return true;
      }
      if (id == R.id.action_add) {
        Intent si=new Intent(this.getActivity(), AddPointActivity.class);
        startActivity(si);
        return true;
      }
      if (id == R.id.action_waypoint) {
        int nearestId=-1;
        try { nearestId=findPointNearCursor(); }
        catch (U.DataException e) {
          mMapViewer.showProgress(e.getMessage());
          return true;
        }
        Intent si=new Intent(this.getActivity(), EditPointActivity.class);
        si.putExtra("id", nearestId);
        si.putExtra("caller", EditPointActivity.EditPointFragment.MAP);
        startActivity(si);
        return true;
      }
      if (id == R.id.action_list) {
        Intent si=new Intent(this.getActivity(), ListActivity.class);
        startActivity(si);
        return true;
      }
      if (id == R.id.action_track) {
        Intent si=new Intent(this.getActivity(), TrackActivity.class);
        startActivity(si);
        return true;
      }
      return super.onOptionsItemSelected(item);
    }

    private int findPointNearCursor() throws U.DataException {
      if (mPointList.getSize() == 0) {
        throw new U.DataException("No stored points");
      }
      String ll=mJSbridge.importLatLon();
      if (ll.contains("null")) {// no map center
        throw new U.DataException("No active map");
      }
      String[] latLon=TextUtils.split(ll,",");
      Point cursor=new Point("mark",latLon[0],latLon[1]);
      int found=mPointList.findNearest(cursor);
      if (found < 0) throw new U.DataException("No stored points with coords");
      return found;
    }

    @Override
    public void onResume() {
      super.onResume();
      if (U.DEBUG) Log.i(U.TAG,"mainFragment:onResume");
      mCellInformer.setFragment(this);
      mGpsInformer.setFragment(this);
      mMapViewer.clearIndicator();
      if (mModel.getJSbridge().isDirty() > 0) {
        mMapViewer.hideIndicator();
        mMapViewer.redraw();
      }
      if (mModel.getJSbridge().hasNoCenter() && mRegistry.noAnyKeys()) {
        mMapViewer.addProgress(getString(R.string.keyless_warning),"\n");
      }
      if (mReadPoints != null) {
        mMapViewer.addProgress(mReadPoints.act+" "+mReadPoints.adopted+" points (of "+mReadPoints.found+") from "
          +mReadPoints.fileName, "\n");
        mReadPoints=null;
      }
      mTrackListener.attachListener(this);
    }

    @Override
    public void onPause() {
      super.onPause();
      mTrackListener.removeListener(this);
    }
    
    @Override
    public void onDestroy() {
      super.onDestroy();
      if (U.DEBUG) Log.i(U.TAG,"mainFragment:onDestroy");
      mCellInformer.clearFragment();
      mGpsInformer.clearFragment();
    }
    
  }// end MainPageFragment

  @Override
  protected android.support.v4.app.Fragment createFragment() { return new MainPageFragment(); }
}
