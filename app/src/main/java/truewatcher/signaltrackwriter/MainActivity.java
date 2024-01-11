package truewatcher.signaltrackwriter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import java.io.IOException;

public class MainActivity extends SingleFragmentActivity {

  public static class TrackPageFragment extends PermissionAwareFragment
          implements PermissionReceiver {

    private MyRegistry mRg=MyRegistry.getInstance();
    private TrackStorage mTrackStorage=Model.getInstance().getTrackStorage();
    private TrackListener mTrackListener=Model.getInstance().getTrackListener();
    private TrackPageFragment.Viewer mV;
    private DataWatcher mDataWatcher=new DataWatcher();
    //private JSbridge mJSbridge=Model.getInstance().getJSbridge();

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
      if (id == R.id.action_renumber) {
        renumber();
        return true;
      }
      if (id == R.id.action_settings) {
        Intent si=new Intent(getActivity(),SettingsActivity.class);
        startActivity(si);
        return true;
      }
      return super.onOptionsItemSelected(item);
    }

    /*@Override
    public boolean onOptionsItemSelected_(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_back) {
        getActivity().finish();
        return true;
      }
      if (id == R.id.action_fit_to_map) {
        if (tryFitTrackToMap()) getActivity().finish();// show map
        return true;
      }
      if (id == R.id.action_delete_last_segment) {
        deleteLastSegment();
        mJSbridge.setDirty(1);
        return true;
      }
      if (id == R.id.action_settings) {
        Intent si=new Intent(getActivity(),PreferencesActivity.class);
        startActivity(si);
        return true;
      }
      return super.onOptionsItemSelected(item);
    }*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      if (U.DEBUG) Log.d(U.TAG,"trackFragment:onCreate");
      setHasOptionsMenu(true);
      mRg.readFromShared(getActivity());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      if (U.DEBUG) Log.d(U.TAG,"trackFragment:onCreateView");
      View v = inflater.inflate(R.layout.fragment_main, container, false);
      mV=new Viewer(v);
      mV.setListeners(this, mTrackStorage);
      return v;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onResume() {
      super.onResume();
      if (U.DEBUG) Log.d(U.TAG,"trackFragment:onResume");
      if (mRg.getBool("useTowerFolder") && ! checkStoragePermission()) {
        mV.alert("No storage permission, asking user");
        askStoragePermission();
        return;
      }
      if (mTrackListener.isOn()) {
        mDataWatcher.run();
        if (U.DEBUG) Log.d(U.TAG,"trackFragment:onResume"+"restarting dataWatcher");
      }
      else {
        printStorageInfo();
      }
      mV.adjustVisibility(mTrackListener.isOn());
    }

    public void printStorageInfo() {
      try {
        mTrackStorage.initTargetDir(getActivity());
        mV.alert("IDLE");
        mV.displayStorageStat(mTrackStorage.statStored());
      }
      catch (IOException e) {
        mV.alert("IOException:"+e.getMessage());
        Log.e(U.TAG, "IOException:"+e.getMessage());
      }
      catch (U.DataException e) {
        mV.alert("DataException:"+e.getMessage());
        Log.e(U.TAG, "DataException:"+e.getMessage());
      }
      catch (U.FileException e) {
        mV.alert("FileException:"+e.getMessage());
        Log.e(U.TAG, "FileException:"+e.getMessage());
      }
    }

    @Override
    public void onPause() {
      super.onPause();
      if (U.DEBUG) Log.d(U.TAG,"trackFragment:onPause");
      mDataWatcher.stop();
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      if (U.DEBUG) Log.d(U.TAG,"trackFragment:onDestroy");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void runAll() {
      if (mTrackListener.isOn()) {
        mV.alert("Service is already started");
        return;
      }
      try {
        if ( ! checkLocationPermission()) {
          mV.alert("No location permission, asking user");
          askLocationPermission();
          return;
        }
        checkGps();
        startMyService();
        mTrackStorage.initTargetDir(getActivity());// see MainPageFragment.loadCurrentTrack
        mV.displayStorageStat(mTrackStorage.statStored());
        mTrackListener.clearCounter();
        mTrackListener.clearCounter2();
        mTrackListener.startListening(getActivity());
        mTrackListener.setOn();
        mV.alert("STARTED");
        mV.adjustVisibility(true);
        //if (U.DEBUG) Log.i(U.TAG, "TrackActivity:"+"10");
        mDataWatcher.run();
      }
      catch (U.UserException e) {
        mV.alert("UserException:"+e.getMessage());
        Log.e(U.TAG, "UserException:"+e.getMessage());
      }
      catch (IOException e) {
        mV.alert("IOException:"+e.getMessage());
        Log.e(U.TAG, "IOException:"+e.getMessage());
      }
      catch (U.DataException e) {
        mV.alert("DataException:"+e.getMessage());
        Log.e(U.TAG, "DataException:"+e.getMessage());
      }
      catch (U.FileException e) {
        mV.alert("FileException:"+e.getMessage());
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

    @Override
    public void receivePermission(int reqCode, boolean isGranted) {
      if ( ! isGranted) {
        if (U.DEBUG) Log.d(U.TAG, "permission denied");
        if (reqCode == 2) {
          mRg.setBool("useTowerFolder",false);
          mRg.saveToShared(getActivity(), "useTowerFolder");
          mV.alert("Permission denied, falling back to native folder");
        }
      }
      else {
        if (U.DEBUG) Log.d(U.TAG,"permission granted");
        mV.alert("Permission granted, try to start again");
      }
    }

    private void checkGps() throws U.UserException {
      LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
      boolean isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
      if ( ! isGpsEnabled) throw new U.UserException("GPS is disabled");
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
      mV.adjustVisibility(false);
      printStorageInfo();
    }

    private void stopMyService() {
      Intent si=new Intent(getActivity(), ForegroundService.class);
      getActivity().stopService(si);
    }

    private void deleteLastSegment() {
      if (mTrackListener.isOn()) {
        mV.alert("Stop recording first");
        return;
      }
      try {
        mTrackStorage.deleteLastSegment();
        //String buf = mTrackStorage.trackCsv2LatLonString();
        //mJSbridge.replaceCurrentTrackLatLonJson(buf);
        mV.alert("IDLE");
        mV.displayStorageStat(mTrackStorage.statStored());;
      }
      catch (IOException e) {
        mV.alert("IOException:"+e.getMessage());
        Log.e(U.TAG, "IOException:"+e.getMessage());
      }
      catch (U.DataException e) {
        mV.alert("DataException:"+e.getMessage());
        Log.e(U.TAG, "DataException:"+e.getMessage());
      }
      catch (U.FileException e) {
        mV.alert("FileException:"+e.getMessage());
        Log.e(U.TAG, "FileException:"+e.getMessage());
      }
    }

    private class DataWatcher implements Runnable {
      private Handler mWatchHandler=new Handler();
      private int mWatchIntervalMS=2000;

      @Override
      public void run() {
        long waitingS=U.getTimeStamp() - mTrackListener.updateTime;
        long prevUpdateIntervalS=mTrackListener.updateTime - mTrackListener.prevUpdateTime;
        mV.row( mTrackListener.getCounter(), mTrackListener.getCounter2(), prevUpdateIntervalS, waitingS );
        //if (U.DEBUG) Log.i(U.TAG, "TrackActivity:"+"DataWatcher:"+"run()");
        mWatchHandler.postDelayed(this, mWatchIntervalMS);
      }

      public void stop() {
        mWatchHandler.removeCallbacks(this);
      }
    }

    private void exportGpx() {
      if (mTrackListener.isOn()) {
        mV.alert("Stop recording first");
        return;
      }
      try {
        String name=Trackpoint.getDate().replace(' ','_').replace(':','-');
        U.Summary res=mTrackStorage.trackCsv2Gpx(name);
        String info=String.format("%s %d points (of %d, %d segment) to %s",
                res.act, res.adopted, res.found, res.segments, res.fileName);
        mV.alert("IDLE");
        mV.data(info);
      }
      catch (IOException e) {
        mV.alert("IOException:"+e.getMessage());
        Log.e(U.TAG, "IOException:"+e.getMessage());
      }
      catch (U.DataException e) {
        mV.alert("DataException:"+e.getMessage());
        Log.e(U.TAG, "DataException:"+e.getMessage());
      }
      catch (U.FileException e) {
        mV.alert("FileException:"+e.getMessage());
        Log.e(U.TAG, "FileException:"+e.getMessage());
      }
    }

    private void renumber() {
      if (mTrackListener.isOn()) {
        mV.alert("Stop recording first");
        return;
      }
      try {
        int maxTrackpointNumber=mTrackStorage.renumber();
        String info=String.format("renumbered trackpoints up to %s",maxTrackpointNumber);
        mV.alert("IDLE");
        mV.data(info);
      }
      catch (IOException e) {
        mV.alert("IOException:"+e.getMessage());
        Log.e(U.TAG, "IOException:"+e.getMessage());
      }
      catch (U.DataException e) {
        mV.alert("DataException:"+e.getMessage());
        Log.e(U.TAG, "DataException:"+e.getMessage());
      }
      catch (U.FileException e) {
        mV.alert("FileException:"+e.getMessage());
        Log.e(U.TAG, "FileException:"+e.getMessage());
      }
    }

    private class Viewer {
      private TextView tvState, tvData, tvCount, tvCount2, tvPrevInterval, tvTime;
      private Button bRefresh, bOn, bOnSegm, bOff, bSettings;
      private TableLayout tlTable1;

      public Viewer(View v) {
        tvState = (TextView) v.findViewById(R.id.tvState);
        tvData = (TextView) v.findViewById(R.id.tvData);
        tvCount = (TextView) v.findViewById(R.id.tvCount);
        tvCount2 = (TextView) v.findViewById(R.id.tvCount2);
        tvPrevInterval = (TextView) v.findViewById(R.id.tvPrevInterval);
        tvTime = (TextView) v.findViewById(R.id.tvTime);
        bOn = (Button) v.findViewById(R.id.bOn);
        bOnSegm = (Button) v.findViewById(R.id.bOnSegm);
        bOff = (Button) v.findViewById(R.id.bOff);
        tlTable1 = (TableLayout) v.findViewById(R.id.tlTable1);

        tvState.setTextColor(U.MSG_COLOR);
      }

      @RequiresApi(api = Build.VERSION_CODES.M)
      public void setListeners(final TrackPageFragment fragment, final TrackStorage ts) {

        bOn.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) { fragment.runAll(); }
        });

        bOnSegm.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            ts.demandNewSegment();
            fragment.runAll();
          }
        });

        bOff.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) { fragment.stopAll(); }
        });
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

      public void alert(String s) { tvState.setText(s); }

      public void data(String s) { tvData.setText(s); }

      public void displayStorageStat(U.Summary2 s) {
        String r=String.format("%s:\n%d records, %d trackpoints in %d segments: \n%s",
                  s.fileName, s.found-2, s.adopted, s.segments, s.segMap);
        tvData.setText(r);
      }

      public void row(int fixCount, int pointCount, long prevUpdateIntervalS, long waitingS) {
        tvCount.setText(Integer.toString(fixCount));
        tvCount2.setText(Integer.toString(pointCount));
        tvPrevInterval.setText(Long.toString(prevUpdateIntervalS));
        tvTime.setText(Long.toString(waitingS));
      }

    }// end Viewer

  }// end MainPageFragment

  @Override
  protected androidx.fragment.app.Fragment createFragment() { return new TrackPageFragment(); }

}
