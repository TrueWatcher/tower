package truewatcher.tower;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
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
          implements TrackListener.TrackPointListener,PermissionReceiver {
    private MyRegistry mRegistry=MyRegistry.getInstance();
    private MapViewer mMapViewer;
    private Model mModel = Model.getInstance();
    private CellPointFetcher mCellPointFetcher = mModel.getCellPointFetcher();;
    private GpsPointFetcher mGpsPointFetcher = mModel.getGpsPointFetcher();;
    private PointList mPointList = mModel.getPointList();
    private StorageHelper mStorageHelper = mModel.getStorageHelper();
    private JSbridge mJSbridge = mModel.getJSbridge();
    private TrackListener mTrackListener = mModel.getTrackListener();
    private U.Summary[] mReadData=null;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      if (U.DEBUG) Log.i(U.TAG,"mainFragment:onCreate");
      setHasOptionsMenu(true);

      mRegistry=MyRegistry.getInstance(getActivity());
      //if (U.DEBUG) U.clearPrefs(getActivity()); // DEBUG
      //U.clearPrefs(getActivity()); // DEBUG
      //mRegistry.readFromShared(getActivity());
      try {
        mRegistry.readFromShared();
        mRegistry.syncSecrets();
      } catch (U.DataException e) {
        throw new RuntimeException(e);
      }
      mCellPointFetcher.setFragment(this);
      mGpsPointFetcher.setFragment(this);
      U.useDayNightTheme(mRegistry.get("theme"));
      U.setMsgColorDayNight(getActivity());
      boolean needsStoragePermission = ( mRegistry.getBool("useMediaFolder") && ( Build.VERSION.SDK_INT < 30 ));
      if (needsStoragePermission && ! checkStoragePermission()) {
        //mV.alert("No storage permission, asking user");
        askStoragePermission(this);
        return;
      }
      /*if (! checkLocationPermission()) {
        askLocationPermission(this);
        return;
      }*/
      mReadData = mModel.loadData(getActivity(), mRegistry);
    }

    @Override
    public void receivePermission(int reqCode, boolean isGranted) {
      if ( ! isGranted) {
        if (U.DEBUG) Log.d(U.TAG, "permission denied for code ="+reqCode);
        if (reqCode == 2) { // STORAGE for older OS
          mRegistry.setBool("useMediaFolder",false);
          mRegistry.saveToShared("useMediaFolder");
          //mV.alert("Permission denied, falling back to native folder");
        }
      }
      else {
        if (U.DEBUG) Log.d(U.TAG,"permission granted for code ="+reqCode);
        //mV.alert("Permission granted, try to start again");
      }

      mReadData = mModel.loadData(this.getActivity(), mRegistry);
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
        mCellPointFetcher.go(mMapViewer, mMapViewer);
        return true;
      }
      if (id == R.id.action_gps) {
        mGpsPointFetcher.go(mMapViewer, mMapViewer);
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
      U.useDayNightTheme(mRegistry.get("theme"));
      U.setMsgColorDayNight(getActivity());
      mCellPointFetcher.setFragment(this);
      mGpsPointFetcher.setFragment(this);
      mMapViewer.clearIndicator();
      if (mModel.getJSbridge().isDirty() > 0) {
        mMapViewer.hideIndicator();
        mMapViewer.redraw();
      }
      if (mModel.getJSbridge().hasNoCenter() && mRegistry.noAnyKeys()) {
        mMapViewer.addProgress(getString(R.string.keyless_warning),"\n");
      }
      if (mReadData != null && mReadData[0] != null && mReadData[0].found > 0 ) {
        String points="%s %d points (of %s) from %s";
        mMapViewer.addProgress(String.format(points,
                mReadData[0].act,mReadData[0].adopted,mReadData[0].found,mReadData[0].fileName
        ), "\n");
        mReadData[0]=null;
      }
      if (mReadData != null && mReadData[1] != null && mReadData[1].adopted > 0) {
        String trkpoints="%s %d trackpoints (%d segments) from %s";
        mMapViewer.addProgress(String.format(trkpoints,
           mReadData[1].act,mReadData[1].adopted,mReadData[1].segments,mReadData[1].fileName
        ), "\n");
        mReadData[1]=null;
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
      mCellPointFetcher.clearFragment();
      mGpsPointFetcher.clearFragment();
    }

  }// end MainPageFragment

  @Override
  protected android.support.v4.app.Fragment createFragment() { return new MainPageFragment(); }
}
