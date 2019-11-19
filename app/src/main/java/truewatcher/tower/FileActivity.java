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
    
    private LinearLayout lTop, lExport;
    private RadioGroup rgAct, rgMode;
    private TextView tvAlert, tvMyFolder;
    private EditText etExportFile,etExportFrom,etExportUntil;
    private Spinner spDirCsv, spDirGpx;
    private CheckBox ckRemoveExported;
    private Model mModel=Model.getInstance();
    private PointList mPointList=Model.getInstance().getPointList();
    private StorageHelper mStorageHelper=Model.getInstance().getStorageHelper();
    private String mSelectedFile="";
    private String mAct;// Open, Load, Export, Delete
    private String mMode="csv";// csv, gpx
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      View v = inflater.inflate(R.layout.fragment_file, container, false);
      
      spDirCsv=(Spinner) v.findViewById(R.id.spDirCsv);      
      spDirGpx=(Spinner) v.findViewById(R.id.spDirGpx);
      adoptCatalog();
      spDirGpx.setOnItemSelectedListener((OnItemSelectedListener) this);
      spDirCsv.setOnItemSelectedListener((OnItemSelectedListener) this);
      
      tvAlert=(TextView) v.findViewById(R.id.tvAlert);
      tvMyFolder=(TextView) v.findViewById(R.id.tvMyFolder);
      tvMyFolder.setText("Data folder: "+mStorageHelper.getMyDir());
      lTop=(LinearLayout) v.findViewById(R.id.lTop);
      rgMode = (RadioGroup) v.findViewById(R.id.rgMode);
      rgMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) { getMode(rgMode); }
      });
      lExport=(LinearLayout) v.findViewById(R.id.lExport);
      etExportFile=(EditText) v.findViewById(R.id.etExportFile);
      etExportFrom=(EditText) v.findViewById(R.id.etExportFrom);
      etExportUntil=(EditText) v.findViewById(R.id.etExportUntil);
      ckRemoveExported=(CheckBox) v.findViewById(R.id.ckRemoveExported);
      
      rgAct = (RadioGroup) v.findViewById(R.id.rgAct);
      getAct(rgAct);
      rgAct.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) { getAct(rgAct); }
      });

      U.enlargeFont(getActivity(), new TextView[] {tvAlert} );
      String myFile=MyRegistry.getInstance().get("myFile");
      tvAlert.setText("File loaded:"+myFile);
      return v;
    }
    
    private void adoptCatalog() {
      addAdapter("csv", spDirCsv);
      addAdapter("gpx", spDirGpx);
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
    
    private void getAct(RadioGroup rg) {
      RadioButton radioButton = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId()); 
      mAct=(String) radioButton.getText();
      adjustVisibility(mAct);
    }
    
    private void getMode(RadioGroup rg) {
      RadioButton radioButton = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId()); 
      mMode=(String) radioButton.getText();
      adjustVisibility(mAct);
    }
    
    private void adjustVisibility(String act) {
      if (act.equals("Open") || act.equals("Load") || act.equals("Delete")) {
        etExportFile.setVisibility(View.GONE);
        lExport.setVisibility(View.GONE);
        ckRemoveExported.setVisibility(View.GONE);
        adjustSpinnerVisibility(mMode);
      }
      else if (act.equals("Export")) {
        etExportFile.setVisibility(View.VISIBLE);
        lExport.setVisibility(View.VISIBLE);
        ckRemoveExported.setVisibility(View.VISIBLE);
        adjustSpinnerVisibility("off");
      }
      else {
        Log.e(U.TAG,"FileFragment_adjustVisibility:"+"wrong act="+act);
        tvAlert.setText("wrong act="+act);
        return;
      }
      
      if (act.equals("Open")) {
        rgMode.setVisibility(View.GONE);
        rgMode.check(R.id.rbCsv);
        mMode="csv";
        adjustSpinnerVisibility(mMode);        
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
        try {
          r=go();
        }
        catch (Exception e) {
          tvAlert.setText(e.getMessage());
          e.printStackTrace();
          return true;
        }
        if (r == "quit") getActivity().finish(); 
        return true;
      }
      if (id == R.id.action_list_from_file) {
        getActivity().finish(); 
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
    
    private String go() throws U.DataException, U.FileException, IOException {
      String myFile=MyRegistry.getInstance().get("myFile");
      String objFile;
      U.Summary s;
      
      tvAlert.setText(mAct+" "+mSelectedFile);
      if (U.DEBUG) Log.d(U.TAG, "FileFragment_go:"+"act="+mAct+", file="+mSelectedFile);
      if (mAct.equals("Open")) {
        objFile=assureExists(mSelectedFile,"csv");
        mStorageHelper.trySetMyFile(objFile);
        if (objFile.equals("trash.csv")) {
          boolean useTrash=MyRegistry.getInstance().getBool("useTrash");
          if (useTrash) { throw new U.FileException("To open "+objFile+", disable Use Trash in Settings"); }
        }
        mStorageHelper.checkPointCount(objFile, mPointList);
        setRegistryMyFile(objFile);
        s=mPointList.clearAndLoad();
        tvAlert.setText(s.act+" "+s.adopted+" points (of "+s.found+") from "+s.fileName);
        return "Ok";
      }
      else if (mAct.equals("Load")) {
        // adb push ~/Desktop/myRoute.gpx /sdcard/Android/data/truewatcher.tower/files
        objFile=assureExists(mSelectedFile,mMode);
        s=mStorageHelper.readPoints(mPointList, objFile, mPointList.getSize(), mMode);
        String outcome=s.act+" "+s.adopted+" points (of "+s.found+") from "+s.fileName;
        outcome += " to "+myFile;
        tvAlert.setText(outcome);
        if (s.adopted > 0) { 
          mPointList.save();
          mPointList.setDirty();
        }
        return "Ok";
      }
      else if (mAct.equals("Export")) {
        String targetFile=etExportFile.getText().toString();
        if (targetFile.length() == 0) { throw new U.FileException("Empty file name"); }
        targetFile=U.assureExtension(targetFile,mMode);
        int from=Integer.valueOf(etExportFrom.getText().toString());
        int until=-1;
        String str_until=etExportUntil.getText().toString();
        if (str_until.equals("*")) until=-1;
        else until=Integer.valueOf(str_until);
        if (U.DEBUG) Log.d(U.TAG, "FileFragment_Export:"+"targetFile="+targetFile+", from="+from+", until="+until);
        s=mStorageHelper.writePoints(mPointList, targetFile, from, until, mMode);
        String outcome=s.act+" "+s.adopted+" points (of "+s.found+") to "+s.fileName;
        if ( ckRemoveExported.isChecked() ) {
          int count=mPointList.fastDeleteGroup(from, until);
          if (count > 0) { 
            mPointList.save();
            mPointList.setDirty();
          }
          outcome+=", "+count+" points removed from "+myFile;
        }
        tvAlert.setText(outcome);
        adoptCatalog();
        return "Ok";
      }
      else if (mAct.equals("Delete")) {
        objFile=assureExists(mSelectedFile,mMode);
        if (objFile.equals(myFile)) { throw new U.FileException("File "+objFile+" is open now"); }
        s=U.unlink(mStorageHelper.getMyDir(), objFile);
        tvAlert.setText(s.act+" "+s.fileName);
        adoptCatalog();
        return "Ok";
      }
      else Log.e(U.TAG, "FileActivity_go:"+"Wrong ACT="+mAct);
      return "fail";
    }
    
    private String assureExists(String f, String ext) throws U.FileException {
      if (f == null || f.isEmpty()) { throw new U.FileException("Choose the file"); }
      String ff=U.assureExtension(f, ext);
      boolean rr=(null != U.fileExists(mStorageHelper.getMyDir(), ff, ext));
      if (rr == false) { throw new U.FileException("Wrong file name:"+ff); }
      return ff;
    }
    
    private void setRegistryMyFile(String f) {
      MyRegistry.getInstance().set("myFile", f);
      MyRegistry.getInstance().saveToShared(getActivity(), "myFile");
    }
  }
  
  @Override
  protected Fragment createFragment() { return new FileFragment(); }
}
