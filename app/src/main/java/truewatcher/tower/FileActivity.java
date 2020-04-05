package truewatcher.tower;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;

public class FileActivity extends SingleFragmentActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }
  
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
  
  public static class FileFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private Model mModel=Model.getInstance();
    private PointList mPointList=Model.getInstance().getPointList();
    private StorageHelper mStorageHelper=Model.getInstance().getStorageHelper();
    private TrackStorage mTrackStorage=Model.getInstance().getTrackStorage();
    private FileFragment.Viewer mV;
    private String mSelectedFile="";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      View v = inflater.inflate(R.layout.fragment_file, container, false);
      mV=new Viewer(v);
      adoptCatalog();
      mV.setListeners(this);
      mV.showMyFolder(mStorageHelper.getMyDir());
      mV.alert("File loaded:"+MyRegistry.getInstance().get("myFile"));
      return v;
    }

    private void adoptCatalog() {
      addAdapter("csv", mV.getSpinnerDirCsv());
      addAdapter("gpx", mV.getSpinnerDirGpx());
    }

    private void addAdapter(String mode, Spinner spinner) {
      String[] catalog=U.getCatalog(mStorageHelper.getMyDir(), mode);
      if (catalog == null || catalog.length == 0) catalog=new String[]{"no files"};
      // the evil thing always selects 0th item by itself and ignores prompt
      catalog=U.arrayConcat(new String[] {"Choose file"}, catalog);
      ArrayAdapter<String> sa = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_item, catalog);
      sa.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
      spinner.setAdapter(sa); 
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
      if (position == 0) return;
      mSelectedFile = parent.getItemAtPosition(position).toString();
      if (U.DEBUG) Log.i(U.TAG,"FileFragment_onItemSelected:"+"Selected: " + mSelectedFile);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) { }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.file_fragment, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_go) {
        String r="";
        try { r=go(mV.exportAct(), mV.exportMode()); }
        catch (Exception e) {
          mV.alert(e.getMessage());
          e.printStackTrace();
          return true;
        }
        if (r.equals("quit")) getActivity().finish();
        return true;
      }
      if (id == R.id.action_list_from_file) {
        getActivity().finish(); 
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
    
    private String go(String act, String mode) throws U.DataException, U.FileException, IOException {
      String myFile=MyRegistry.getInstance().get("myFile");
      String targetFile;
      int removedCount=-1;
      U.Summary s;
      
      mV.alert(act+" "+mSelectedFile);
      if (U.DEBUG) Log.d(U.TAG, "FileFragment_go:"+"act="+act+", file="+mSelectedFile);
      if (act.equals(getString(R.string.action_open))) {
        targetFile=assureExists(mSelectedFile,"csv");
        mStorageHelper.trySetMyFile(targetFile);
        if (targetFile.equals("trash.csv")) {
          boolean useTrash=MyRegistry.getInstance().getBool("useTrash");
          if (useTrash) { throw new U.FileException("To open "+targetFile+", disable Use Trash in Settings"); }
        }
        mStorageHelper.checkPointCount(targetFile, mPointList);
        setRegistryMyFile(targetFile);
        s=mPointList.clearAndLoad();
        mV.showStat(act, s, -1, "");
        return "Ok";
      }
      else if (act.equals(getString(R.string.action_new))) {
        targetFile=mV.getExportFile();
        targetFile=assureNotExists(targetFile,mode);
        s=mStorageHelper.writePoints(mPointList, targetFile, 0, 0, mode);
        mStorageHelper.trySetMyFile(targetFile);
        setRegistryMyFile(targetFile);
        mPointList.fastClear();
        mV.showStat(act, null, -1, targetFile);
        return "Ok";
      }
      else if (act.equals(getString(R.string.action_load))) {
        // adb push ~/Desktop/myRoute.gpx /sdcard/Android/data/truewatcher.tower/files
        targetFile=assureExists(mSelectedFile,mode);
        s=mStorageHelper.readPoints(mPointList, targetFile, mPointList.getSize(), mode);
        if (s.adopted > 0) { 
          mPointList.save();
          mPointList.setDirty();
        }
        mV.showStat(act, s, -1, myFile);
        return "Ok";
      }
      else if (act.equals(getString(R.string.action_view_track))) {
        String latLonJson = "[]";
        targetFile=assureExists(mSelectedFile,mode);
        if (mode.equals("gpx")) {
          GpxHelper gh = new GpxHelper();
          latLonJson = gh.track2latLonJson(U.fileGetContents(mStorageHelper.getMyDir(), mSelectedFile));
          s = gh.getResults();
        }
        else if (mode.equals("csv")) {
          TrackStorage.Track2LatLonJSON csvReader = mTrackStorage.new Track2LatLonJSON();
          latLonJson = csvReader.file2LatLonJSON(mSelectedFile);
          s = csvReader.getResults();
        }
        else throw new U.RunException("Not to get here");
        if ( ! s.act.equals("loaded")) {
          mV.alert(s.act);
          return "fail";
        }
        JSbridge jsb=Model.getInstance().getJSbridge();
        jsb.addViewTrackLatLonJson(latLonJson);
        mV.showStat(act, s, -1, mSelectedFile);
        return "Ok";
      }
      else if (act.equals(getString(R.string.action_export))) {
        targetFile=mV.getExportFile();
        if (targetFile.length() == 0) { throw new U.FileException("Empty file name"); }
        targetFile=U.assureExtension(targetFile,mode);
        int from=mV.getExportFrom();
        int until=mV.getExportUntil();
        if (U.DEBUG) Log.d(U.TAG, "FileFragment_Export:"+"targetFile="+targetFile+", from="+from+", until="+until);
        s=mStorageHelper.writePoints(mPointList, targetFile, from, until, mode);
        if ( mV.getRemoveExported() ) {
          removedCount=mPointList.fastDeleteGroup(from, until);
          if (removedCount > 0) {
            mPointList.save();
            mPointList.setDirty();
          }
        }
        adoptCatalog();
        mV.showStat(act, s, removedCount, myFile);
        return "Ok";
      }
      else if (act.equals(getString(R.string.action_export_track))) {
        if (mModel.getTrackListener().isOn()) throw new U.FileException("Stop recording first");
        targetFile=mV.getExportFile();
        if (targetFile.length() == 0) {
          throw new U.FileException("Empty file name");
        }
        targetFile=U.assureExtension(targetFile,mode);
        if (U.DEBUG) Log.d(U.TAG, "FileFragment_Export track:"+"targetFile="+targetFile);
        s=mTrackStorage.trackCsv2Gpx(targetFile);
        if ( mV.getRemoveExported() ) {
          mTrackStorage.deleteAll();
          removedCount=9999;
        }
        adoptCatalog();
        mV.showStat(act, s, removedCount, mTrackStorage.getMyFile());
        return "Ok";
      }
      else if (act.equals(getString(R.string.action_delete))) {
        targetFile=assureExists(mSelectedFile,mode);
        if (targetFile.equals(myFile)) { throw new U.FileException("File "+targetFile+" is open now"); }
        s=U.unlink(mStorageHelper.getMyDir(), targetFile);
        adoptCatalog();
        mV.showStat(act, s, -1, "");
        return "Ok";
      }
      else throw new U.RunException ("FileActivity_go:"+"Wrong ACT="+act);
    }
    
    private String assureExists(String f, String ext) throws U.FileException {
      if (f == null || f.isEmpty()) { throw new U.FileException("Choose the file"); }
      String ff=U.assureExtension(f, ext);
      boolean exists=(null != U.fileExists(mStorageHelper.getMyDir(), ff, ext));
      if (exists == false) { throw new U.FileException("Wrong file name:"+ff); }
      return ff;
    }

    private String assureNotExists(String f, String ext) throws U.FileException {
      if (f == null || f.isEmpty()) { throw new U.FileException("Enter the file name"); }
      String ff=U.assureExtension(f, ext);
      boolean exists=(null != U.fileExists(mStorageHelper.getMyDir(), ff, ext));
      if (exists == true) { throw new U.FileException("File "+ff+" already exists, delete it or choose another name"); }
      return ff;
    }
    
    private void setRegistryMyFile(String f) {
      MyRegistry.getInstance().set("myFile", f);
      MyRegistry.getInstance().saveToShared(getActivity(), "myFile");
    }

    private class Viewer {
      private LinearLayout lTop, lExport;
      private RadioGroup rgAct, rgMode;
      private TextView tvAlert, tvMyFolder;
      private EditText etExportFile,etExportFrom,etExportUntil;
      private Spinner spDirCsv, spDirGpx;
      private CheckBox ckRemoveExported;
      private String mAct;
      private String mMode="csv";// csv, gpx

      public Viewer(View v) {
        spDirCsv = (Spinner) v.findViewById(R.id.spDirCsv);
        spDirGpx = (Spinner) v.findViewById(R.id.spDirGpx);
        tvAlert = (TextView) v.findViewById(R.id.tvAlert);
        tvMyFolder = (TextView) v.findViewById(R.id.tvMyFolder);
        lTop = (LinearLayout) v.findViewById(R.id.lTop);
        rgMode = (RadioGroup) v.findViewById(R.id.rgMode);
        lExport = (LinearLayout) v.findViewById(R.id.lExport);
        etExportFile = (EditText) v.findViewById(R.id.etExportFile);
        etExportFrom = (EditText) v.findViewById(R.id.etExportFrom);
        etExportUntil = (EditText) v.findViewById(R.id.etExportUntil);
        ckRemoveExported = (CheckBox) v.findViewById(R.id.ckRemoveExported);
        rgAct = (RadioGroup) v.findViewById(R.id.rgAct);

        U.enlargeFont(getActivity(), new TextView[] {tvAlert} );
        tvAlert.setTextColor(U.MSG_COLOR);
      }

      public void showMyFolder(String s) { tvMyFolder.setText("Data folder: "+s); }

      public Spinner getSpinnerDirCsv() { return spDirCsv; }

      public Spinner getSpinnerDirGpx() { return spDirGpx; }

      public void setListeners(OnItemSelectedListener _this) {
        spDirGpx.setOnItemSelectedListener((OnItemSelectedListener) _this);
        spDirCsv.setOnItemSelectedListener((OnItemSelectedListener) _this);
        rgMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(RadioGroup group, int checkedId) { getMode(rgMode); }
        });

        getAct(rgAct);
        rgAct.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(RadioGroup group, int checkedId) { getAct(rgAct); }
        });
      }

      private void adjustVisibility(String act) {
        final String[] hideExportControls=new  String[] {
                getString(R.string.action_open),
                getString(R.string.action_load),
                getString(R.string.action_delete),
                getString(R.string.action_view_track)
        };
        if (U.arrayContains( hideExportControls, act)) {
          etExportFile.setVisibility(View.GONE);
          lExport.setVisibility(View.GONE);
          ckRemoveExported.setVisibility(View.GONE);
          adjustSpinnerVisibility(mMode);
        }
        else if (act.equals(getString(R.string.action_new))) {
          etExportFile.setVisibility(View.VISIBLE);
          lExport.setVisibility(View.GONE);
          ckRemoveExported.setVisibility(View.GONE);
          adjustSpinnerVisibility("off");
        }
        else if (act.equals(getString(R.string.action_export))) {
          etExportFile.setVisibility(View.VISIBLE);
          lExport.setVisibility(View.VISIBLE);
          ckRemoveExported.setVisibility(View.VISIBLE);
          adjustSpinnerVisibility("off");
        }
        else if (act.equals(getString(R.string.action_export_track))) {
          etExportFile.setVisibility(View.VISIBLE);
          etExportFile.setText("track" +
                  Trackpoint.getDate().replace(' ','_').replace(':','-'));
          lExport.setVisibility(View.GONE);
          ckRemoveExported.setVisibility(View.VISIBLE);
          adjustSpinnerVisibility("off");
        }
        else {
          Log.e(U.TAG,"FileFragment_adjustVisibility:"+"wrong act="+act);
          tvAlert.setText("wrong act="+act);
          return;
        }

        if (act.equals(getString(R.string.action_open))) {
          rgMode.setVisibility(View.GONE);
          rgMode.check(R.id.rbCsv);
          mMode="csv";
          adjustSpinnerVisibility(mMode);
        }
        else if (act.equals(getString(R.string.action_new))) {
          rgMode.setVisibility(View.GONE);
          rgMode.check(R.id.rbCsv);
          mMode="csv";
        }
        else if (act.equals(getString(R.string.action_export_track))) {
          rgMode.setVisibility(View.GONE);
          rgMode.check(R.id.rbGpx);
          mMode="gpx";
        }
        else {
          rgMode.setVisibility(View.VISIBLE);
        }
      }

      private void adjustSpinnerVisibility(String mode) {
        if (mode.equals("gpx")) {
          spDirGpx.setVisibility(View.VISIBLE);
          spDirCsv.setVisibility(View.GONE);
        }
        else if (mode.equals("csv")) {
          spDirCsv.setVisibility(View.VISIBLE);
          spDirGpx.setVisibility(View.GONE);
        }
        else if (mode.equals("off")) {
          spDirCsv.setVisibility(View.GONE);
          spDirGpx.setVisibility(View.GONE);
        }
        else {
          Log.e(U.TAG, "FileFragment:"+"Wrong mode="+mode);
        }
      }

      private void getAct(RadioGroup rg) {
        RadioButton radioButton = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
        mAct=(String) radioButton.getText();
        adjustVisibility(mAct);
      }

      public String exportAct() { return mAct; }

      private void getMode(RadioGroup rg) {
        RadioButton radioButton = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
        mMode=(String) radioButton.getText();
        adjustVisibility(mAct);
      }

      public String exportMode() { return mMode; }

      public void alert(String s) { tvAlert.setText(s); }

      public String getExportFile() { return etExportFile.getText().toString(); }

      public int getExportFrom() { return Integer.valueOf(etExportFrom.getText().toString()); }

      public int getExportUntil() {
        String str_until = etExportUntil.getText().toString();
        if (str_until.equals("*")) return -1;
        else return Integer.valueOf(str_until);
      }

      public boolean getRemoveExported() { return ckRemoveExported.isChecked(); }

      public void showStat(String act, U.Summary s) {
        showStat(act, s, -1, "");
      }

      public void showStat(String act, U.Summary s, int removedCount, String extra) {
        String resTemplate="";
        String removed="";

        if (act.equals(getString(R.string.action_open))) {
          resTemplate="%s %s points (of %s) from %s";
          tvAlert.setText(String.format(resTemplate, s.act, s.adopted,s.found, s.fileName));
        }
        else if (act.equals(getString(R.string.action_new))) {
          resTemplate="New empty file %s is now current";
          mV.alert(String.format(resTemplate, extra));
        }
        else if (act.equals(getString(R.string.action_load))) {
          resTemplate="%s %s points (of %s) from %s to %s";
          tvAlert.setText(String.format(resTemplate, s.act, s.adopted,s.found, s.fileName, extra));
        }
        else if (act.equals(getString(R.string.action_view_track))) {
          resTemplate = "%s %s trackpoints (%s segment) from %s";
          tvAlert.setText(String.format(resTemplate, s.act, s.adopted, s.segments, extra));
        }
        else if (act.equals(getString(R.string.action_export))) {
          if (removedCount > 0) removed=", "+removedCount+" points removed from "+extra;
          resTemplate = "%s %s points (of %s) to %s%s";
          tvAlert.setText(String.format(resTemplate, s.act, s.adopted, s.found, s.fileName, removed));
        }
        else if (act.equals(getString(R.string.action_export_track))) {
          if (removedCount > 0) removed=", all points removed from "+extra;
          resTemplate="%s %d points (of %d, %d segments) to %s%s";
          tvAlert.setText(String.format(resTemplate,
                  s.act,s.adopted,s.found,s.segments,s.fileName,removed));
        }
        else if (act.equals(getString(R.string.action_delete))) {
          mV.alert(s.act+" "+s.fileName);
        }
        else throw new U.RunException ("FileActivity_Viewer_showStat:"+"Wrong ACT="+act);
      }

    }// end Viewer
  }// end FileFragment
  
  @Override
  protected Fragment createFragment() { return new FileFragment(); }
}
