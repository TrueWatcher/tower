package truewatcher.signaltrackwriter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import java.io.IOException;

interface PermissionChecker {
  public void genericRequestPermission(String permCode, int reqCode, PermissionReceiver receiver);
}

interface PermissionReceiver {
  public void receivePermission(int reqCode, boolean isGranted);
}

public class MainActivity extends SingleFragmentActivity {

  @Override
  public void onRequestPermissionsResult(int reqCode, String[] permissions, int[] grantResults) {
    Log.d(U.TAG, "Calling super.onResult from the activity");
    super.onRequestPermissionsResult(reqCode, permissions, grantResults);
  }

  public static class MainPageFragment extends Fragment
          implements PermissionChecker, PermissionReceiver {

    private SparseArray<PermissionReceiver> mPermissionReceivers =
            new SparseArray<PermissionReceiver>();
    private MyRegistry mRg=MyRegistry.getInstance();
    private TrackStorage mTrackStorage=Model.getInstance().getTrackStorage();
    private TrackListener mTrackListener=Model.getInstance().getTrackListener();
    private CellInformer mCellInformer=Model.getInstance().getCellInformer();
    private TextView tvState, tvData, tvCount, tvPrevInterval, tvTime;
    private Button bRefresh, bOn, bOnSegm, bOff, bSettings;
    private TableLayout tlTable1;
    private DataWatcher mDataWatcher=new DataWatcher();

    //@TargetApi(23)
    public void genericRequestPermission(String permCode, int reqCode, PermissionReceiver receiver) {
      mPermissionReceivers.put(reqCode, receiver);
      Log.d(U.TAG, "Requesting user...");
      requestPermissions(new String[]{permCode}, reqCode);
    }

    @Override
    public void onRequestPermissionsResult(int reqCode, String[] permissions, int[] grantResults) {
      boolean isGranted = ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED );
      Log.d(U.TAG,"grantResults length="+grantResults.length);
      mPermissionReceivers.get(reqCode).receivePermission(reqCode,isGranted);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.fragment_main, menu);
      super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_delete_last_segment) {
        deleteLastSegment();
        return true;
      }
      if (id == R.id.action_export_gpx) {
        exportGpx();
        return true;
      }
      if (id == R.id.action_settings) {
        Intent si=new Intent(getActivity(),SettingsActivity.class);
        startActivity(si);
        return true;
      }
      return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.i(U.TAG,"mainFragment:onCreate");
      setHasOptionsMenu(true);
      mRg.readFromShared(getActivity());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      Log.i(U.TAG,"mainFragment:onCreateView");
      View v = inflater.inflate(R.layout.fragment_main, container, false);
      tvState = (TextView) v.findViewById(R.id.tvState);
      tvData = (TextView) v.findViewById(R.id.tvData);
      tvCount = (TextView) v.findViewById(R.id.tvCount);
      tvPrevInterval = (TextView) v.findViewById(R.id.tvPrevInterval);
      tvTime = (TextView) v.findViewById(R.id.tvTime);
      bOn = (Button) v.findViewById(R.id.bOn);
      bOnSegm = (Button) v.findViewById(R.id.bOnSegm);
      bOff = (Button) v.findViewById(R.id.bOff);
      bOn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) { runAll(); }
      });
      bOnSegm.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) { mTrackStorage.demandNewSegment(); runAll(); }
      });
      bOff.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) { stopAll(); }
      });
      tlTable1 = (TableLayout) v.findViewById(R.id.tlTable1);
      return v;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onResume() {
      super.onResume();
      Log.i(U.TAG,"mainFragment:onResume");
      if (mRg.getBool("useTowerFolder") && ! checkStoragePermission()) {
        tvState.setText("No storage permission, asking user");
        askStoragePermission();
        return;
      }
      if (mTrackListener.isOn()) {
        mDataWatcher.run();
        Log.i(U.TAG,"mainFragment:onResume"+"restarting dataWatcher");
      }
      else {
        printStorageInfo();
      }
      adjustVisibility(mTrackListener.isOn());
    }

    public void printStorageInfo() {
      try {
        mTrackStorage.initTargetDir(getActivity());
        String storageIbfo=displayStorageStat(mTrackStorage.statStored());
        tvState.setText("IDLE");
        tvData.setText(storageIbfo);
      }
      catch (IOException e) {
        tvState.setText("IOException:"+e.getMessage());
        Log.d(U.TAG, "IOException:"+e.getMessage());
      }
      catch (U.DataException e) {
        tvState.setText("DataException:"+e.getMessage());
        Log.d(U.TAG, "DataException:"+e.getMessage());
      }
      catch (U.FileException e) {
        tvState.setText("FileException:"+e.getMessage());
        Log.d(U.TAG, "FileException:"+e.getMessage());
      }
    }

    @Override
    public void onPause() {
      super.onPause();
      if (U.DEBUG) Log.i(U.TAG,"mainFragment:onPause");
      mDataWatcher.stop();
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      if (U.DEBUG) Log.i(U.TAG,"mainFragment:onDestroy");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void runAll() {
      if (mTrackListener.isOn()) {
        tvState.setText("Service is already started");
        return;
      }
      try {
        if ( ! checkLocationPermission()) {
          tvState.setText("No location permission, asking user");
          askLocationPermission();
          return;
        }
        checkGps();
        startMyService();
        mTrackStorage.initTargetDir(getActivity());
        String storageIbfo=displayStorageStat(mTrackStorage.statStored());
        mCellInformer.bindAxtivity(getActivity());
        mTrackListener.clearCounter();
        mTrackListener.startListening(getActivity());
        mTrackListener.setOn();
        tvState.setText("STARTED");
        tvData.setText(storageIbfo);
        adjustVisibility(true);
        mDataWatcher.run();
      }
      catch (U.UserException e) {
        tvState.setText("UserException:"+e.getMessage());
        Log.e(U.TAG, "UserException:"+e.getMessage());
      }
      catch (IOException e) {
        tvState.setText("IOException:"+e.getMessage());
        Log.e(U.TAG, "IOException:"+e.getMessage());
      }
      catch (U.DataException e) {
        tvState.setText("DataException:"+e.getMessage());
        Log.e(U.TAG, "DataException:"+e.getMessage());
      }
      catch (U.FileException e) {
        tvState.setText("FileException:"+e.getMessage());
        Log.e(U.TAG, "FileException:"+e.getMessage());
      }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkLocationPermission() {
      int cl=getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
      if (U.DEBUG) Log.d(U.TAG,"cl="+cl+"/"+PackageManager.PERMISSION_GRANTED);
      return (cl == PackageManager.PERMISSION_GRANTED);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkStoragePermission() {
      int cl=getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
      if (U.DEBUG) Log.d(U.TAG,"cl="+cl+"/"+PackageManager.PERMISSION_GRANTED);
      return (cl == PackageManager.PERMISSION_GRANTED);
    }

    private void askLocationPermission() {
      genericRequestPermission(Manifest.permission.ACCESS_FINE_LOCATION, 1, this);
    }

    private void askStoragePermission() {
      genericRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 2, this);
    }

    public void receivePermission(int reqCode, boolean isGranted) {
      if ( ! isGranted) {
        if (U.DEBUG) Log.d(U.TAG, "permission denied");
        if (reqCode == 2) {
          mRg.setBool("useTowerFolder",false);
          mRg.saveToShared(getActivity(), "useTowerFolder");
          tvState.setText("Permission denied, falling back to native folder");
        }
      }
      else {
        if (U.DEBUG) Log.d(U.TAG,"permission granted");
        tvState.setText("Permission granted, try to start again");
      }
    }

    private void checkGps() throws U.UserException {
      LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
      boolean isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
      if ( ! isGpsEnabled) throw new U.UserException("GPS is disabled");
    }

    private String displayStorageStat(U.Summary2 s) {
      String r=String.format("%s:\n%d records, %d trackpoints in %d segments: \n%s",
              s.fileName, s.found-2, s.adopted, s.segments, s.segMap);
      return r;
    }

    private void startMyService() {
      Intent si=new Intent(getActivity(), ForegroundService.class);
      getActivity().startService(si);
    }

    private void stopAll() {
      mTrackListener.stopListening();
      stopMyService();
      mTrackListener.setOff();
      mDataWatcher.stop();
      adjustVisibility(false);
      printStorageInfo();
      //tvState.setText(tvState.getText().toString().concat("\nIDLE"));
    }

    private void stopMyService() {
      Intent si=new Intent(getActivity(), ForegroundService.class);
      getActivity().stopService(si);
    }

    private void deleteLastSegment() {
      if (mTrackListener.isOn()) {
        tvState.setText("Stop recording first");
        return;
      }
      try {
        mTrackStorage.deleteLastSegment();
        String storageIbfo=displayStorageStat(mTrackStorage.statStored());
        tvState.setText("IDLE");
        tvData.setText(storageIbfo);
      }
      catch (IOException e) {
        tvState.setText("IOException:"+e.getMessage());
        Log.e(U.TAG, "IOException:"+e.getMessage());
      }
      catch (U.DataException e) {
        tvState.setText("DataException:"+e.getMessage());
        Log.e(U.TAG, "DataException:"+e.getMessage());
      }
      catch (U.FileException e) {
        tvState.setText("FileException:"+e.getMessage());
        Log.e(U.TAG, "FileException:"+e.getMessage());
      }
    }

    private void exportGpx() {
      if (mTrackListener.isOn()) {
        tvState.setText("Stop recording first");
        return;
      }
      try {
        String name=Trackpoint.getDate().replace(' ','_').replace(':','-');
        U.Summary res=mTrackStorage.trackCsv2Gpx(name);
        String info=String.format("%s %d points (of %d, %d segment) to %s",
                res.act, res.adopted, res.found, res.segments, res.fileName);
        tvState.setText("IDLE");
        tvData.setText(info);
      }
      catch (IOException e) {
        tvState.setText("IOException:"+e.getMessage());
        Log.e(U.TAG, "IOException:"+e.getMessage());
      }
      catch (U.DataException e) {
        tvState.setText("DataException:"+e.getMessage());
        Log.e(U.TAG, "DataException:"+e.getMessage());
      }
      catch (U.FileException e) {
        tvState.setText("FileException:"+e.getMessage());
        Log.e(U.TAG, "FileException:"+e.getMessage());
      }
    }

    private void adjustVisibility(boolean isOn) {
      if (isOn) {
        tlTable1.setVisibility(View.VISIBLE);
        bOff.setVisibility(View.VISIBLE);
        bOn.setVisibility(View.GONE);
        bOnSegm.setVisibility(View.GONE);
      }
      else {
        tlTable1.setVisibility(View.GONE);
        bOff.setVisibility(View.GONE);
        bOn.setVisibility(View.VISIBLE);
        bOnSegm.setVisibility(View.VISIBLE);
      }
    }

    private class DataWatcher implements Runnable {
      private Handler mWatchHandler=new Handler();
      private int mWatchIntervalMS=2000;

      @Override
      public void run() {
        long waitingS=U.getTimeStamp() - mTrackListener.updateTime;
        long prevUpdateIntervalS=mTrackListener.updateTime - mTrackListener.prevUpdateTime;
        tvCount.setText(Integer.toString(mTrackListener.getCounter()));
        tvPrevInterval.setText(Long.toString(prevUpdateIntervalS));
        tvTime.setText(Long.toString(waitingS));
        mWatchHandler.postDelayed(this, mWatchIntervalMS);
      }

      public void stop() {
        mWatchHandler.removeCallbacks(this);
      }
    }

  }// end MainPageFragment

  @Override
  protected Fragment createFragment() { return new MainPageFragment(); }

}
