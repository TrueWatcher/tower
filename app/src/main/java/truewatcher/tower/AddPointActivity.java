package truewatcher.tower;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
    private Model mModel=Model.getInstance();
    private CellInformer mCellInformer=mModel.getCellInformer();;
    private GpsInformer mGpsInformer=mModel.getGpsInformer();
    private PointList mPointList=mModel.getPointList();;
    private JSbridge mJSbridge=mModel.getJSbridge();;;
    private AddPointFragment.Viewer mV;
    private AddPointFragment.PointRenderer mCellRenderer, mGpsRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      mCellInformer.setFragment(this);
      mGpsInformer.setFragment(this);
    }
      
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      Point toBeRemoved;
      View v = inflater.inflate(R.layout.fragment_add_point, container, false);
      mV=new Viewer(v);
      mV.showCenter(mJSbridge.importCenterLatLon());
      mV.showNumber(mPointList.getNextS());
      mV.showEdge(mPointList);
      mGpsRenderer=new PointRenderer(mV.getTvGpsStatus(), mV.getTvGpsData());
      mCellRenderer=new PointRenderer(mV.getTvCellStatus(), mV.getTvCellData());
      mV.setListeners(mGpsRenderer,mCellRenderer);
      mGpsRenderer.showPoint(mModel.lastGps);
      mCellRenderer.showPoint(mModel.lastCell);
      mGpsRenderer.showProgress(mGpsInformer.getStatus());
      mCellRenderer.showProgress(mCellInformer.getStatus());
      return v;
    }

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
    
    private class Adder {
      
      private void go() {
        String outcome="";
        String removed;
        Point p;
        String aType=getAdditionType();
        mV.alert(aType);
        String[] latLon=getLatLon(aType);
        if (latLon == null) {// valid unresolved cell gives {"",""}  
          mV.alert(aType+": no data");
          return;
        }          
        p=preparePoint(aType, latLon, mV.getAsCenter(), mV.getIsProtected());
        try {
          removed=mPointList.addAsNext(p);
          mJSbridge.onPoinlistmodified();
          outcome="added:"+p.getId()+","+p.getType()+","+p.getComment();
          outcome+="; "+removed;
        }
        catch (Exception e) {
          outcome="Cannot add anything: "+e.getMessage();
        }
        mV.alert(outcome);
        mV.showNumber(mPointList.getNextS());
        mV.setComment("");
        mV.uncheckBoxes();
        mV.removeFocus();
        String res=mPointList.save();
      }
      
      private Point preparePoint(String aType, String[] latLon, boolean asCenter, boolean isProtect) {
        Point p;
        if (aType.equals("gps")) { p = (Point) mModel.lastGps.clone(); }
        else if (aType.equals("cell")) { p = (Point) mModel.lastCell.clone(); }
        else p=new Point("mark",latLon[0],latLon[1]);
        p.setComment( mV.getComment() );
        p.setId( mPointList.getNext() );
        p.setCurrentTime();
        if (isProtect) p.protect();
        if (asCenter) {
          mPointList.setProximityOrigin((Point) p.clone());
          mJSbridge.consumeLocation(p);
        }
        return p;
      }
      
      private String getAdditionType() {
        int selectedId = mV.getCheckedRadioButton();
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
          lat=mV.getLat();
          lon=mV.getLon();
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
    }// end Adder

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

    private class Viewer {
      private LinearLayout mLTop;
      private TextView tvCenter, tvGpsStatus, tvGpsData, tvCellStatus, tvCellData, tvNumber, tvAlert;
      private EditText etComment, etLat, etLon;
      private Button bGetCell, bGetGps;
      private RadioGroup rPointType;
      private CheckBox cbAsCenter, cbProtect;
      private DeeperRadioGroup mRGroup;

      public Viewer(View v) {
        tvCenter = (TextView) v.findViewById(R.id.tvMapCenter);
        tvGpsStatus = (TextView) v.findViewById(R.id.tvGpsStatus);
        tvGpsData = (TextView) v.findViewById(R.id.tvGpsData);
        tvCellStatus = (TextView) v.findViewById(R.id.tvCellStatus);
        tvCellData = (TextView) v.findViewById(R.id.tvCellData);
        bGetGps = (Button) v.findViewById(R.id.bGetGps);
        bGetCell = (Button) v.findViewById(R.id.bGetCell);
        mLTop = (LinearLayout) v.findViewById(R.id.lTop);
        tvAlert = (TextView) v.findViewById(R.id.tvAlert);
        tvNumber = (TextView) v.findViewById(R.id.tvNumber);
        rPointType = (RadioGroup) v.findViewById(R.id.rPointType);
        mRGroup = new DeeperRadioGroup(rPointType);
        etComment = (EditText) v.findViewById(R.id.etComment);
        cbAsCenter = (CheckBox) v.findViewById(R.id.cbAsCenter);
        cbProtect = (CheckBox) v.findViewById(R.id.cbProtect);
        etLat = (EditText) v.findViewById(R.id.etLat);
        etLon = (EditText) v.findViewById(R.id.etLon);

        U.enlargeFont(getActivity(), new TextView[] {tvNumber} );
        tvAlert.setTextColor(U.MSG_COLOR);
      }

      public TextView getTvGpsStatus() { return tvGpsStatus; }

      public TextView getTvGpsData() { return tvGpsData; }

      public TextView getTvCellStatus() { return tvCellStatus; }

      public TextView getTvCellData() { return tvCellData; }

      public void setListeners(final PointRenderer gpsRenderer, final PointRenderer cellRenderer) {
        bGetGps.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            mGpsInformer.go(gpsRenderer, gpsRenderer);
          }
        });
        bGetCell.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            mCellInformer.go(cellRenderer, cellRenderer);
          }
        });
      }

      public void showCenter(String s) {
        tvCenter.setText(s);
      }

      public void showNumber(String s) {
        tvNumber.setText(s);
      }

      public void setComment(String s) { etComment.setText(s); }

      public String getComment() { return etComment.getText().toString(); }

      public void showEdge(PointList pointList) {
        String rp="";
        Point toBeRemoved;
        try {
          toBeRemoved = pointList.getEdge();
          if (toBeRemoved != null) {
            rp = "List is full, ready to remove " + String.valueOf(toBeRemoved.getId())
                    + "." + toBeRemoved.getComment();
          }
        }
        catch (Exception e) {
          rp = "List is full, " + e.getMessage();
        }
        tvAlert.setText(rp);
      }

      public void alert(String s) { tvAlert.setText(s); }

      public boolean getAsCenter() { return cbAsCenter.isChecked(); }

      public boolean getIsProtected() { return cbProtect.isChecked(); }

      public void uncheckBoxes() {
        cbProtect.setChecked(false);
        cbAsCenter.setChecked(false);
      }

      public void removeFocus() { mLTop.requestFocus(); }

      public int getCheckedRadioButton() { return mRGroup.getCheckedRadioButtonId(); }

      public String getLat() { return etLat.getText().toString(); }

      public String getLon() { return etLon.getText().toString(); }

    }// end Viewer
  }// end AddPointFragment
    
  @Override
  protected android.support.v4.app.Fragment createFragment() { return new AddPointFragment(); }
}
