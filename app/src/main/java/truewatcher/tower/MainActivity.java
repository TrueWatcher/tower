package truewatcher.tower;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

interface PermissionChecker {
  public void genericRequestPermission(String permCode, int reqCode, PermissionReceiver receiver);
}

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
  
  @Override
  public void onRequestPermissionsResult(int reqCode, String[] permissions, int[] grantResults) {
    if (U.DEBUG) Log.d(U.TAG, "Calling super.onResult from the activity");
    super.onRequestPermissionsResult(reqCode, permissions, grantResults);
  }

  @TargetApi(23) 
  public static class MainPageFragment extends Fragment implements PermissionChecker {
    private MyRegistry mRegistry=MyRegistry.getInstance();
    private TextView mTwA;
    private TextView mTwB;    
    private WebView mWebView;
    private PointViewer mPv;
    private SparseArray<PermissionReceiver> mPermissionReceivers =
            new SparseArray<PermissionReceiver>();
    private Model mModel = Model.getInstance();
    private CellInformer mCellInformer = mModel.getCellInformer();;
    private GpsInformer mGpsInformer = mModel.getGpsInformer();;
    private PointList mPointList = mModel.getPointList();
    private JSbridge mJSbridge = mModel.getJSbridge();
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

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      if (U.DEBUG) Log.i(U.TAG,"mainFragment:onCreateView");
      View v = inflater.inflate(R.layout.fragment_main, container, false);
      mTwA = (TextView) v.findViewById(R.id.twA);
      mTwA.setText("");
      mTwB = (TextView) v.findViewById(R.id.twB);
      mTwB.setText("");
      mWebView = (WebView) v.findViewById(R.id.wvWebView);
      mPv=new PointViewer(mTwA, mTwB, mWebView);      
      mPv.redraw();
      return v;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.main_fragment, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_cell) {
        mCellInformer.go(mPv,mPv);
        return true;
      }
      if (id == R.id.action_gps) {
        mGpsInformer.go(mPv,mPv);
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
          mTwA.setText(e.getMessage());
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
      return mPointList.findNearest(cursor);
    }
    
    @TargetApi(23)
    public void genericRequestPermission(String permCode, int reqCode, PermissionReceiver receiver) {
      mPermissionReceivers.put(reqCode, receiver);
      if (U.DEBUG) Log.d(U.TAG, "Requesting user...");
      requestPermissions(new String[]{permCode}, reqCode);   
    }
    
    @Override
    public void onRequestPermissionsResult(int reqCode, String[] permissions, int[] grantResults) {
      boolean isGranted = ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED );
      if (U.DEBUG) Log.d(U.TAG,"grantResults length="+grantResults.length);
      mPermissionReceivers.get(reqCode).receivePermission(isGranted);
    }

    @Override
    public void onResume() {
      super.onResume();
      if (U.DEBUG) Log.i(U.TAG,"mainFragment:onResume");
      mCellInformer.setFragment(this);
      mGpsInformer.setFragment(this);
      mTwA.setText("");
      mTwB.setText("");
      if (mModel.getJSbridge().importLatLon().contains("null") && mRegistry.noAnyKeys()) {
        mPv.addProgress(getString(R.string.keyless_warning),"\n");
      }
      if (mReadPoints != null) {
        mPv.addProgress(mReadPoints.act+" "+mReadPoints.adopted+" points (of "+mReadPoints.found+") from "
          +mReadPoints.fileName, "\n");
        mReadPoints=null;
      }
      if (mModel.getJSbridge().isDirty()) {
        mPv.redraw();
        mModel.getJSbridge().clearDirty();
      }
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
