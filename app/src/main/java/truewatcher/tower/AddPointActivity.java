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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

public class AddPointActivity  extends SingleFragmentActivity {

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
    if (id == R.id.action_back) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public static class AddPointFragment extends PermissionAwareFragment {
    
    private LinearLayout mLTop;
    private TextView tvCenter, tvGpsStatus, tvGpsData, tvCellStatus, tvCellData, tvNumber, tvAlert;
    private EditText etComment, etLat, etLon;
    private Button bGetCell, bGetGps;
    private RadioGroup rPointType;
    private CheckBox cbAsCenter, cbProtect;
    private CellInformer mCellInformer;
    private GpsInformer mGpsInformer;
    private PointList mPointList;
    private JSbridge mJSbridge;
    private PointRenderer mCellRenderer;
    private PointRenderer mGpsRenderer;
    private Model mModel=Model.getInstance();
    private Fragment mFragment;
    private DeeperRadioGroup mRGroup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      mFragment=this;
      mCellInformer = mModel.getCellInformer();
      mCellInformer.setFragment(this);
      mGpsInformer = mModel.getGpsInformer();
      mGpsInformer.setFragment(this);
      mPointList = mModel.getPointList(); 
      mJSbridge = mModel.getJSbridge();
    }
      
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      Point toBeRemoved;
      View v = inflater.inflate(R.layout.fragment_add_point, container, false);
      
      tvCenter=(TextView) v.findViewById(R.id.tvMapCenter);
      tvCenter.setText(mJSbridge.importCenterLatLon());
      
      tvGpsStatus=(TextView) v.findViewById(R.id.tvGpsStatus);
      tvGpsData=(TextView) v.findViewById(R.id.tvGpsData);
      tvCellStatus=(TextView) v.findViewById(R.id.tvCellStatus);
      tvCellData=(TextView) v.findViewById(R.id.tvCellData); 
      mGpsRenderer=new PointRenderer(tvGpsStatus, tvGpsData);
      mCellRenderer=new PointRenderer(tvCellStatus, tvCellData);
      
      bGetGps=(Button) v.findViewById(R.id.bGetGps);
      bGetCell=(Button) v.findViewById(R.id.bGetCell);
      bGetGps.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mGpsInformer.go(mGpsRenderer,mGpsRenderer);
        }
      });
      bGetCell.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mCellInformer.go(mCellRenderer,mCellRenderer);
        }
      });
      
      mGpsRenderer.showPoint(mModel.lastGps);
      mCellRenderer.showPoint(mModel.lastCell);
      mGpsRenderer.showProgress(mGpsInformer.getStatus());
      mCellRenderer.showProgress(mCellInformer.getStatus());
      
      mLTop=(LinearLayout) v.findViewById(R.id.lTop);
      tvAlert=(TextView) v.findViewById(R.id.tvAlert);
      tvNumber=(TextView) v.findViewById(R.id.tvNumber);
      tvNumber.setText( mPointList.getNextS() );
      try {
        toBeRemoved=mPointList.getEdge();
        if (toBeRemoved != null) {
          String rp="List is full, ready to remove "+String.valueOf(toBeRemoved.getId())+"."+toBeRemoved.getComment();
          tvAlert.setText(rp);
        }
      }
      catch (Exception e) {
        String rp="List is full, "+e.getMessage();
        tvAlert.setText(rp);
      }
      rPointType=(RadioGroup) v.findViewById(R.id.rPointType);
      mRGroup=new DeeperRadioGroup(rPointType);
      etComment=(EditText) v.findViewById(R.id.etComment);
      cbAsCenter=(CheckBox) v.findViewById(R.id.cbAsCenter);
      cbProtect=(CheckBox) v.findViewById(R.id.cbProtect);      
      etLat=(EditText) v.findViewById(R.id.etLat);
      etLon=(EditText) v.findViewById(R.id.etLon);
      adjustFont();
      return v;
    }
    
    private void adjustFont() {
      float ts=etComment.getTextSize();
      TextView[] tv={tvNumber};
      for (int i=0; i < tv.length; i+=1) { tv[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, ts);}
    }
    
    private class Adder {
      
      private void go() {
        String outcome="";
        String removed;
        Point p;
        String aType=getAdditionType();
        tvAlert.setText(aType);
        String[] latLon=getLatLon(aType);
        if (latLon == null) {// valid unresolved cell gives {"",""}  
          tvAlert.setText(aType+": no data");
          return;
        }          
        p=preparePoint(aType, latLon, cbAsCenter.isChecked(), cbProtect.isChecked());
        try {
          removed=mPointList.addAsNext(p);
          mJSbridge.setDirty();
          outcome="added:"+p.getId()+","+p.getType()+","+p.getComment();
          outcome+="; "+removed;
        }
        catch (Exception e) {
          outcome="Cannot add anything: "+e.getMessage();
        }
        tvAlert.setText(outcome);
        tvNumber.setText( mPointList.getNextS() );
        etComment.setText("");
        cbProtect.setChecked(false);
        cbAsCenter.setChecked(false);
        mLTop.requestFocus();// removes the focus from comment or any other field
        savePoints();
      }
      
      private void savePoints() {
        String res=mPointList.save();
        //mTvAlert.setText(res);
      }
      
      private Point preparePoint(String aType, String[] latLon, boolean asCenter, boolean protect) {
        Point p;
        if (aType.equals("gps")) { p = (Point) mModel.lastGps.clone(); }
        else if (aType.equals("cell")) { p = (Point) mModel.lastCell.clone(); }
        else p=new Point("mark",latLon[0],latLon[1]);
        p.setComment( getComment() );
        p.setId( mPointList.getNext() );
        p.setCurrentTime();
        if (protect) p.protect();
        if (asCenter) {
          mPointList.setProximityOrigin((Point) p.clone());
          mJSbridge.exportLatLon(p.lat,p.lon);
          mJSbridge.setDirty();
        }
        return p;
      }
      
      private String getAdditionType() {
        //int selectedId = rPointType.getCheckedRadioButtonId(); fails is buttons are not directly inside the group
        int selectedId = mRGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.rbMapCenter) return "mapCenter";
        if (selectedId == R.id.rbGps) return "gps";
        if (selectedId == R.id.rbCell) return "cell";
        if (selectedId == R.id.rbWaypoint) return "waypoint";
        return "error";
      }
      
      private String[] getLatLon(String aType) {
        String[] latLon;
        String lat;
        String lon;
        String s;
        Point p;
        if (aType.equals("mapCenter")) {
          s=mJSbridge.importCenterLatLon();
          if (s.isEmpty() || ! s.contains(",")) return null;
          latLon=s.split(",");
          if (latLon.length != 2) return null;
          return latLon;
        }
        if (aType.equals("waypoint")) {
          lat=etLat.getText().toString();
          lon=etLon.getText().toString();
          if (lat.isEmpty() || lon.isEmpty()) return null;
          return new String[]{lat, lon};
        }      
        if (aType.equals("gps")) {
          if (mModel.lastGps == null) return null;
          p=mModel.lastGps;
          return new String[]{p.lat, p.lon};
        }
        if (aType.equals("cell")) {
          if (mModel.lastCell == null) return null;
          p=mModel.lastCell;          
          if (p.cellData == null || p.cellData.isEmpty()) return null;
          if ( ! p.hasCoords()) return new String[]{"",""};
          return new String[]{p.lat, p.lon};
        }
        return null;
      }
      
      private String getComment() {
        return etComment.getText().toString(); // checks are at Point.addComment
      }
      
    }// end Adder
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.add_point_fragment, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_do_add) {
        (new Adder()).go();
        getActivity().finish();
        return true;
      }
      return super.onOptionsItemSelected(item);
    }

    private class PointRenderer extends PointIndicator implements PointReceiver {
      
      public PointRenderer(TextView twP, TextView twD) { super(twP,twD); }
      
      @Override
      public void addProgress(String d) { showProgress(d); }// just show last update
      
      public void showPoint(Point p) {
        if (p == null) {
          if (U.DEBUG) Log.d(U.TAG,"PointRenderer:"+"Empty point");
          return;
        }
        String s="";
        String location="";
        if (p.cellData != null && ! p.cellData.isEmpty()) s+=JsonHelper.filterQuotes(p.cellData);
        if (p.lat != null && p.lon != null) location+="lat="+truncate(p.lat,10)+",lon="+truncate(p.lon,10);
        if (p.alt != null) location+=",alt="+truncate(p.alt,6);
        if ( ! location.isEmpty() && p.range != null) location+=",Accuracy:"+floor(p.range);
        if ( ! location.isEmpty()) {
          if ( ! s.isEmpty()) s+="\n";
          s+=location;  
        }
        if ( ! s.isEmpty()) { showData(s); }
      }

      @Override
      public void onPointavailable(Point p) { showPoint(p); }
    
    } // end PointRenderer
    
    @Override
    public void onDestroy() {
      super.onDestroy();
    }

  }// end AddPointFragment
    
  @Override
  protected android.support.v4.app.Fragment createFragment() { return new AddPointFragment(); }
}
