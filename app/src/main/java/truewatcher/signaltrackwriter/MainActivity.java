package truewatcher.signaltrackwriter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends SingleFragmentActivity {

  public static class TrackPageFragment extends PermissionAwareFragment
          implements PermissionReceiver {

    private MyRegistry mRg=MyRegistry.getInstance();
    private TrackStorage mTrackStorage=Model.getInstance().getTrackStorage();
    private TrackListener mTrackListener=Model.getInstance().getTrackListener();
    private TrackPageFragment.Viewer mV;
    private DataWatcher mDataWatcher=new DataWatcher();
    private final int REQUEST_ACTION_OPEN_DOCUMENT_TREE = 100;
    private String mSAFAppFolderUri = "", mSAFRootFolderUri = "";
    private String mAppFolderName;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.fragment_main, menu);
      super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_cells) {
        Intent si=new Intent(getActivity(),CellsActivity.class);
        startActivity(si);
        return true;
      }
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
      boolean needsPermission = mRg.getBool("useTowerFolder") ||
          ( mRg.getBool("useMediaFolder") && ( Build.VERSION.SDK_INT < 30 ));
      if (needsPermission && ! checkStoragePermission()) {
        mV.alert("No storage permission, asking user");
        askStoragePermission();
        return;
      }
      if ( mRg.getBool("useSAF") && (Build.VERSION.SDK_INT >= 30)) {
        mSAFRootFolderUri = mRg.get("SAFRootFolderUri");
        mSAFAppFolderUri = mRg.get("SAFAppFolderUri");
        mAppFolderName = getActivity().getPackageName();
        boolean accessCheck = ! mSAFRootFolderUri.isEmpty() && ! mSAFAppFolderUri.isEmpty() &&
            (null != U.FileUtilsSAF.fileExistsSAF(mSAFRootFolderUri, mAppFolderName, getActivity()));
        if (! accessCheck) Log.w(U.TAG, "SAF folder path is present, but does not work. Re-initializing...");
        if (mSAFRootFolderUri.isEmpty() || ! accessCheck) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestSAFFolder();
          }
          return;
        }
        mTrackStorage.setTargetPath(mSAFAppFolderUri); // fall through
      }

      if (! mTrackListener.isOn()) {
        printStorageInfo();
      }
      else {
        mDataWatcher.run();
        if (U.DEBUG) Log.d(U.TAG,"trackFragment:onResume"+"restarting dataWatcher");
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

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void requestSAFFolder() {
      Intent it = U.FileUtilsSAF.makeIntentToRequestFolder(getActivity(), Environment.DIRECTORY_DOCUMENTS);
      startActivityForResult(it, REQUEST_ACTION_OPEN_DOCUMENT_TREE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
      if (requestCode == REQUEST_ACTION_OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK) {
        Uri uri = null;
        if (resultData != null) {
          uri = resultData.getData();
          final int takeFlags = resultData.getFlags()
              & (Intent.FLAG_GRANT_READ_URI_PERMISSION
              | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
          try {
            getActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
          } catch (Exception e) {
            Log.e(U.TAG, "Something wrong with persistable permission:"
                + e.getMessage() + "\n" + e.getStackTrace());
          }

          if (U.DEBUG) Log.d(U.TAG, "got uri: " + uri.toString() + ", path: " + uri.getPath());
          // got uri: content://com.android.externalstorage.documents/tree/primary%3ADocuments
          // /tree/primary:Documents

          mSAFRootFolderUri = uri.toString();
          DocumentFile appFolder = null;
          try {
            appFolder = U.FileUtilsSAF.createDirectoryIfMissingSAF(mSAFRootFolderUri, mAppFolderName, getActivity());
            /*
            DocumentFile test0 = appFolder.createFile("text/csv","test0.csv");
            Uri ut0 = test0.getUri();
            OutputStream stream = getActivity().getContentResolver().openOutputStream(Uri.parse(ut0.toString()), "w");
            stream.write("???".getBytes());
            DocumentFile dt0 = appFolder.findFile("test0.csv");
            Log.d(U.TAG, "found fast:"+(dt0 != null));
            U.FileUtilsSAF.filePutContentsSAF(appFolder.getUri().toString(), "test1.csv",
                "test content", "text/csv", getActivity(), false);
            Log.d(U.TAG, "test0 uri: " + test0.getUri().toString()); */
          }
          catch (U.FileException e) { //  | IOException
            mV.alert("FileException: "+e.getMessage());
            Log.e(U.TAG, "FileException: "+e.getMessage()+"\n"+e.getStackTrace());
            return;
          }

          mSAFAppFolderUri = appFolder.getUri().toString();
          // add to build.gradle: implementation 'androidx.documentfile:documentfile:1.0.1'
          // https://stackoverflow.com/questions/62375696/unexpected-behavior-when-documentfile-fromtreeuri-is-called-on-uri-of-subdirec
          //Log.d(U.TAG, "appFolder uri: " + appFolder.getUri().toString());
          mRg.set("SAFRootFolderUri", mSAFRootFolderUri);
          mRg.saveToShared(getActivity(),"SAFRootFolderUri");
          mRg.set("SAFAppFolderUri", mSAFAppFolderUri);
          mRg.saveToShared(getActivity(),"SAFAppFolderUri");
          /*DocumentFile test=U.FileUtilsSAF.fileExistsSAF(mSAFAppFolderUri, "test0.csv", getActivity());
          if (test != null) {
            Log.d(U.TAG, "test uri: " + test.getUri().toString());
          }
          else {
            Log.d(U.TAG, "test uri: miss");
          }*/

          mTrackStorage.setTargetPath(mSAFAppFolderUri);
          printStorageInfo();
          mV.adjustVisibility(mTrackListener.isOn());
          /*
          String myFileExt = "Text.txt";
          String myMime = "text/plain";
          String myExt = "txt";

          try {
            U.FileUtilsSAF.createIfMissingSAF(mMyFolderUri, myFileExt, myExt, getActivity());
            U.FileUtilsSAF.filePutContentsSAF(mMyFolderUri, myFileExt, " + line\n", myMime, getActivity(),true);
          } catch (U.FileException e) {
            Log.e(U.TAG, "FileException:" + e.getMessage() + "\n" + e.getStackTrace());
          } catch (IOException e) {
            Log.e(U.TAG, "IOException:" + e.getMessage() + "\n" + e.getStackTrace());
          }
          Log.d(U.TAG, "onActivityResult: got " + requestCode + "/" + resultCode);
           */
        }
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
          mV.alert("This app will not work without access to Fine Location");
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
        if (U.DEBUG) Log.d(U.TAG, "permission denied for code ="+reqCode);
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
