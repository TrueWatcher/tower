package truewatcher.tower;

import android.support.v4.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.Map;

public class EditPointActivity extends SingleFragmentActivity {
  private int mArgId=-1;
  private String mArgCaller="";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = getIntent();
    Bundle bd = intent.getExtras();       
    if (bd != null) {
      mArgId = bd.getInt("id");
      mArgCaller = (String) bd.get("caller");
    }
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
  
  public int getArgId() { return mArgId; }
  public String getArgCaller() { return mArgCaller; }
  
  public void showEditTextDialog(DialogFragment dialog) {
    dialog.show(getSupportFragmentManager(), "EditTextDialogFragment");
  }
  
  public void showConfirmationDialog(DialogFragment dialog) {
    dialog.show(getSupportFragmentManager(), "ConfirmationDialogFragment");
  }
  
  public static class EditPointFragment extends Fragment
      implements ConfirmationDialogFragment.ConfirmationDialogReceiver,
      EditTextDialogFragment.EditTextDialogReceiver {

    public static final String MAP="map";
    public static final String LIST="list";
    private Model mModel=Model.getInstance();;
    private PointList mPointList=mModel.getPointList();;
    private JSbridge mJSbribge=mModel.getJSbridge();
    private android.support.v4.app.Fragment mFragment;
    private EditPointFragment.Editor mEd;
    private EditPointFragment.Viewer mV;
    private Point mPoint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      mFragment=this;
      mEd=new Editor();
    }
      
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      View v = inflater.inflate(R.layout.fragment_edit_point, container, false);
      mV=new Viewer(v);
      mV.setListeners(mEd);

      mEd.adoptPoint(extractId());
      mPoint=mEd.getPoint();
      return v;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.edit_point_fragment, menu);
      if (extractCaller().equals(this.MAP)) {
        MenuItem actionList = menu.findItem(R.id.action_list);
        actionList.setVisible(false);
      }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_center) {
        if (mPoint == null || ! mPoint.hasCoords()) {
          mV.alert("No coordinates");
          return true;
        }
        mJSbribge.consumeLocation(mPoint);
        Point p2=(Point) mPoint.clone();
        mPointList.setProximityOrigin(p2);
        return true;
      }
      if (id == R.id.action_map) {
        mJSbribge.consumeLocation(mPoint);
        exit(ListActivity.ListPointsFragment.FLUSH);
        return true;
      }
      if (id == R.id.action_list) {
        exit("Ok");
        return true;
      }
      if (id == R.id.action_protect) {
        mEd.toggleProtect();
        return true;
      }
      if (id == R.id.action_comment) {
        offerEditText(R.id.action_comment, R.string.action_comment, mPoint.getComment());
        return true;
      }
      if (id == R.id.action_delete) {
        if (mPoint.isProtected()) { mV.alert("This point is protected"); }
        else { requestConfirmation(R.id.action_delete, R.string.action_delete); }
        return true;
      }
      if (id == R.id.action_coords) {
        mEd.findCellCoords();
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
    
    private void offerEditText(int actionId, int actionStringId, String text) {
      EditTextDialogFragment dialog = new EditTextDialogFragment();
      Map<String,String> args=new ArrayMap<String,String>();
      args.put("actionId",String.valueOf(actionId));
      args.put("actionStringId",String.valueOf(actionStringId));
      args.put("text",text);
      dialog.setArguments(U.map2bundle(args));
      dialog.setTargetFragment(mFragment, 2);
      ((EditPointActivity) getActivity()).showEditTextDialog(dialog);   
    }
    
    public void onEditTextPositive(int id, String text) {
      if (U.DEBUG) Log.d(U.TAG, "EditPointFragment:"+"EditTextPositive:"+id);
      if (id == R.id.action_comment) {
        mEd.updateComment(text);
      }
    }
    
    private void requestConfirmation(int actionId, int actionStringId) {
      ConfirmationDialogFragment dialog = new ConfirmationDialogFragment();
      Map<String,String> args=new ArrayMap<String,String>();
      args.put("actionId",String.valueOf(actionId));
      args.put("actionStringId",String.valueOf(actionStringId));
      dialog.setArguments(U.map2bundle(args));
      dialog.setTargetFragment(mFragment, 1);
      ((EditPointActivity) getActivity()).showConfirmationDialog(dialog);     
    }
    
    private void exit(String result) {
      Intent returnIntent = new Intent();
      returnIntent.putExtra("result", result);                          
      mFragment.getActivity().setResult(AppCompatActivity.RESULT_OK, returnIntent);
      mFragment.getActivity().finish();     
    }
    
    public void onConfirmationNegative(int id) {
      if (U.DEBUG) Log.d(U.TAG, "EditPointFragment:"+"ConfirmationNegative:"+id);
    }
    
    public void onConfirmationPositive(int id) {
      if (U.DEBUG) Log.d(U.TAG, "EditPointFragment:"+"ConfirmationPositive:"+id);
      if (id == R.id.action_delete) {
        if (mEd.tryDeleteCurrentPoint()) exit("Ok");
      }
    }
    
    private int extractId() {
      // getting data from the activity argument
      // https://stackoverflow.com/questions/12739909/send-data-from-activity-to-fragment-in-android
      EditPointActivity activity = (EditPointActivity) mFragment.getActivity();
      int myData = activity.getArgId();
      if (U.DEBUG) Log.d(U.TAG,"Editor:"+"Got id="+String.valueOf(myData));
      return myData;
    }

    private String extractCaller() {
      EditPointActivity activity = (EditPointActivity) mFragment.getActivity();
      String myData = activity.getArgCaller();
      if (U.DEBUG) Log.d(U.TAG,"Editor:"+"Got caller="+myData);
      return myData;
    }

    @Override
    public void onPause() {
      super.onPause();
      if (mPointList.isDirty()) {
        String res=mPointList.save();
        if (U.DEBUG) Log.d(U.TAG,"EditPointFragment_trySavePoints:"+res);
      }
    }

    private class Editor implements PointReceiver {
      private Point mP;
      
      public void adoptPoint(int iid) {
        mP = mPointList.getById(iid);
        if (mP == null) {
          mV.alert("No point with id="+iid);
          return;
        }
        mV.fillForm(mP);
      }
      
      public Point getPoint() { return mP; }
      
      public void toggleProtect() {
        if (mP.isProtected()) mP.unprotect();
        else mP.protect();
        mPointList.update(mP);
        mV.fillForm(mP);
      }
      
      public boolean tryDeleteCurrentPoint() {
        if (mP.isProtected()) {
          mV.alert("Cannot delete a protected point");
          return false;
        }
        mPointList.moveUnprotectedToTrash(mP.getId());
        mJSbribge.onPoinlistmodified();
        return true;
      }

      public void updateComment(String s) {
        mP.setComment(s);
        mPointList.update(mP);
        mJSbribge.onPoinlistmodified();
        mV.fillForm(mP);
      }

      public void updateNote() {
        mP.setNote(mV.getNote());
        mPointList.update(mP);
        mV.fillForm(mP);
      }

      public void findCellCoords() {
        if ( ! mP.getType().equals("cell") || mP.cellData == null || mP.cellData.length() < 10) {
          mV.alert("This point is not a cell");
          return;
        }
        PointIndicator pi=new PointIndicator(mV.getTvAlert(), mV.getTvNull());
        CellInformer ci=new CellInformer();
        //ci.setFragment(mFragment);
        ci.onlyResolve(pi, this, mP);
      }

      @Override
      public void onPointavailable(Point p) {
        // callback after resolving cell to coords
        mV.adjustVisibility();
        mPointList.update(p);
        mJSbribge.onPoinlistmodified();
        mV.fillForm(p);
      }
    }

    private class Viewer {
      private TextView tvAlert, tvType, tvProtect, tvId, tvComment, tvLatLon, tvDate, tvRange,
              tvRangeLabel, tvAlt, tvAltLabel, tvCellData, tvNull;
      private EditText etNote;
      private ImageButton bNoteOk;

      public Viewer(View v) {
        tvAlert = (TextView) v.findViewById(R.id.tvAlert);
        tvType=(TextView) v.findViewById(R.id.tvType);
        tvProtect=(TextView) v.findViewById(R.id.tvProtect);
        tvId=(TextView) v.findViewById(R.id.tvId);
        tvLatLon=(TextView) v.findViewById(R.id.tvLatLon);
        tvDate=(TextView) v.findViewById(R.id.tvDate);
        tvRange=(TextView) v.findViewById(R.id.tvRange);
        tvRangeLabel=(TextView) v.findViewById(R.id.tvRangeLabel);
        tvAlt=(TextView) v.findViewById(R.id.tvAlt);
        tvAltLabel=(TextView) v.findViewById(R.id.tvAltLabel);
        tvCellData=(TextView) v.findViewById(R.id.tvCellData);
        tvNull=(TextView) v.findViewById(R.id.tvNull);
        tvComment=(TextView) v.findViewById(R.id.tvComment);
        etNote=(EditText) v.findViewById(R.id.etNote);
        bNoteOk=(ImageButton) v.findViewById(R.id.bNoteOk);

        U.enlargeFont(getActivity(), new TextView[] {tvId, tvType, tvComment} );
        tvAlert.setTextColor(U.MSG_COLOR);
      }

      public void setListeners(final Editor ed) {
        bNoteOk.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            ed.updateNote();
          }
        });
      }

      public void alert(String s) { tvAlert.setText(s); }

      public void fillForm(Point p) {
        tvId.setText(ne(p.getId()));
        tvType.setText(ne(p.getType()));
        if (p.isProtected()) tvProtect.setText(R.string.lock);
        else tvProtect.setText(".");
        tvComment.setText(ne(p.getComment()));
        tvDate.setText(ne(p.time));
        if (p.hasCoords()) tvLatLon.setText(p.lat+", "+p.lon);
        if (p.range != null && ! p.range.isEmpty()) {
          tvRangeLabel.setVisibility(View.VISIBLE);
          tvRange.setText(PointIndicator.floor(p.range));
        }
        else { tvRangeLabel.setVisibility(View.GONE); }
        if (p.alt != null && ! p.alt.isEmpty()) {
          tvAltLabel.setVisibility(View.VISIBLE);
          tvAlt.setText(PointIndicator.floor(p.alt));
        }
        else { tvAltLabel.setVisibility(View.GONE); }
        if ( p.getType().equals("cell") ) {
          tvCellData.setText(JsonHelper.filterQuotes(p.cellData));
          tvCellData.setVisibility(View.VISIBLE);
        }
        else {
          tvCellData.setVisibility(View.GONE);
        }
        etNote.setText(ne(p.getNote()));
      }

      private String ne(String s) {
        if (s == null || s.isEmpty()) return "";
        return s;
      }

      private String ne(int i) {
        if (i <= 0) return "";
        return String.valueOf(i);
      }

      public String getNote() { return etNote.getText().toString(); }

      public TextView getTvAlert() { return tvAlert; }

      public TextView getTvNull() { return tvNull; }

      public void adjustVisibility() {
        tvNull.setVisibility(View.GONE);
        tvAlert.setVisibility(View.VISIBLE);
      }

    }// end Viewer
    
  }// end ListFragment

  @Override
  protected android.support.v4.app.Fragment createFragment() { return new EditPointFragment(); }
}
