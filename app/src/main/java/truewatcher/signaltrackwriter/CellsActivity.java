package truewatcher.signaltrackwriter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

public class CellsActivity extends SingleFragmentActivity {
  public static class CellsPageFragment extends PermissionAwareFragment
      implements PermissionReceiver {

    private MyRegistry mRg=MyRegistry.getInstance();
    private CellsActivity.CellsPageFragment.Viewer mV;
    private CellsActivity.CellsPageFragment.CellsWatcher mCellsWatcher=new CellsActivity.CellsPageFragment.CellsWatcher();
    private boolean mIsOn = true;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.fragment_cells, menu);
      super.onCreateOptionsMenu(menu,inflater);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_back) {
        stopAll();
        getActivity().finish();
        return true;
      }
      if (id == R.id.action_pause) {
        togglePause();
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
      if (U.DEBUG) Log.d(U.TAG, "cellsFragment:onCreate");
      setHasOptionsMenu(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      if (U.DEBUG) Log.d(U.TAG,"cellsFragment:onCreateView");
      View v = inflater.inflate(R.layout.fragment_cells, container, false);
      mV=new CellsActivity.CellsPageFragment.Viewer(v);
      return v;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onResume() {
      super.onResume();
      if (U.DEBUG) Log.d(U.TAG,"cellsFragment:onResume");
      runAll();
    }

    @Override
    public void onPause() {
      super.onPause();
      if (U.DEBUG) Log.d(U.TAG,"cellsFragment:onPause");
      stopAll();
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      if (U.DEBUG) Log.d(U.TAG,"cellsFragment:onDestroy");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void runAll() {
      if (! checkLocationPermission()) {
        mV.alert("This app will not work without access to Fine Location");
        askLocationPermission();
        return;
      }
      mCellsWatcher.run();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkLocationPermission() {
      int cl=getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
      if (U.DEBUG) Log.d(U.TAG,"cl="+cl+"/"+ PackageManager.PERMISSION_GRANTED);
      return (cl == PackageManager.PERMISSION_GRANTED);
    }

    private void askLocationPermission() {
      genericRequestPermission(Manifest.permission.ACCESS_FINE_LOCATION, 1, this);
    }

    @Override
    public void receivePermission(int reqCode, boolean isGranted) {
      if ( ! isGranted) {
        if (U.DEBUG) Log.d(U.TAG, "permission denied for code="+reqCode);
        if (reqCode == 2) { // STORAGE
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

    private void stopAll() {
      mCellsWatcher.stop();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void togglePause() {
      if (mIsOn) {
        stopAll();
        mV.alert("PAUSED");
        mIsOn = false;
      }
      else {
        runAll();
        mIsOn = true;
      }
    }

    private class CellsWatcher implements Runnable,CellDataReceiver {
      private Handler mWatchHandler = new Handler();
      private int mWatchIntervalMS = 5000;
      private CellInformer mCellInformer = new CellInformer();
      private int mRunTime = 0;

      @RequiresApi(api = Build.VERSION_CODES.Q)
      @Override
      public void run() {
        mWatchIntervalMS = mRg.getInt("cellsRefreshS") * 1000;
        mRunTime += mWatchIntervalMS/1000;
        mV.alert(String.valueOf(mRunTime));
        mCellInformer.bindActivity((FragmentActivity) getActivity());
        mCellInformer.requestCellInfos(this, mCellInformer.ALL);
        //if (U.DEBUG) Log.i(U.TAG, "TrackActivity:"+"DataWatcher:"+"run()");
        mWatchHandler.postDelayed(this, mWatchIntervalMS);
      }

      public void stop() {
        mWatchHandler.removeCallbacks(this);
      }

      public void onCellDataObtained(List<JSONObject> cellDataList) {
        mV.cells(cellDataList);
        //String c = cellDataList.toString();
        //mV.data(c);
      }
    }

    private class Viewer {
      private TextView tvState, tvData;
      private final String fields = "[\"type\", \"MNC\", \"CID\", \"PCI\", \"dBm\"]";
      JSONArray mFa = new JSONArray();
      String mHeadline = "";

      @RequiresApi(api = Build.VERSION_CODES.N)
      public Viewer(View v) {
        tvState = (TextView) v.findViewById(R.id.tvState);
        tvData = (TextView) v.findViewById(R.id.tvData);
        tvState.setTextColor(U.MSG_COLOR);
        try {
          mFa = new JSONArray(fields);
        } catch (JSONException e) {
          throw new U.RunException("Not to happen");
        }
        String s = "";
        for (int i = 0; i < mFa.length(); i += 1) {
          try {
            s = mFa.getString(i);
          } catch (JSONException e) {
            s = "*";
          }
          mHeadline += s + " \t";
        }
        float textSize = tvState.getTextSize(); // gives px
        // https://stackoverflow.com/questions/3687065/textview-settextsize-behaves-abnormally-how-to-set-text-size-of-textview-dynam
        tvData.setTextSize( TypedValue.COMPLEX_UNIT_PX,textSize * 1.25f);
      }

      public void alert(String s) {
        tvState.setText(s);
      }

      public void data(String s) {
        tvData.setText(s);
      }

      public void sData(Spannable s) {
        tvData.setText(s);
      }

      public void cells(List<JSONObject> cellDataList) {
        SpannableStringBuilder b = new SpannableStringBuilder();
        SpannableString ss;
        String s;
        JSONObject cellData;
        b.append(mHeadline);
        for (int i=0; i < cellDataList.size(); i+=1) {
          b.append("\n");
          cellData = cellDataList.get(i);
          s = cell(cellData);
          ss = new SpannableString(s);
          if (CellInformer.isWatched(cellData)) ss = setColor(ss, Color.MAGENTA);
          b.append(ss);
        }
        sData(b);
      }

      private String cell(JSONObject cellData) {
        StringBuilder b = new StringBuilder();
        String s = "";
        String f="";
        for (int i=0; i < mFa.length(); i+=1) {
          try {
            f = mFa.getString(i);
            s = cellData.getString(f);
          }
          catch (JSONException e) { s = "-"; }
          if (s.equals("2147483647")) s="--";
          if (s.equals("268435455")) s="--";
          b.append(s);
          b.append(" \t");
        }
        return b.toString();
      }

      private SpannableString setColor(String s, int color) {
        SpannableString r = new SpannableString(s);
        r.setSpan(new ForegroundColorSpan(color), 0, s.length()-1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return r;
      }
      private SpannableString setColor(SpannableString ss, int color) {
        ss.setSpan(new ForegroundColorSpan(color), 0, ss.length()-1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
      }
    }// end Viewer

  }// end MainPageFragment
  @Override
  protected androidx.fragment.app.Fragment createFragment() { return new CellsActivity.CellsPageFragment(); }
}
