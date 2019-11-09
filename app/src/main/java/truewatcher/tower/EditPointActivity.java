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
  private String mArg="empty";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = getIntent();
    Bundle bd = intent.getExtras();       
    if (bd != null) { mArg = (String) bd.get("id"); }
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
  
  public String getArg() { return mArg; }
  
  public void showEditTextDialog(DialogFragment dialog) {
    dialog.show(getSupportFragmentManager(), "EditTextDialogFragment");
  }
  
  public void showConfirmationDialog(DialogFragment dialog) {
    dialog.show(getSupportFragmentManager(), "ConfirmationDialogFragment");
  }
  
  public static class EditPointFragment extends Fragment
      implements ConfirmationDialogFragment.ConfirmationDialogReceiver,
      EditTextDialogFragment.EditTextDialogReceiver {
    private Model mModel;
    private PointList mPointList;
    private android.support.v4.app.Fragment mFragment;
    private Editor mEd;
    private Point mPoint;

    private TextView tvAlert, tvType, tvProtect, tvId, tvComment, tvLatLon, tvDate, tvRange,
            tvRangeLabel, tvAlt, tvAltLabel, tvCellData, tvNull;
    private EditText etNote;
    private ImageButton bNoteOk;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      mFragment=this;
      mModel=Model.getInstance();
      mPointList=mModel.getPointList();
      mEd=new Editor();
    }
      
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      View v = inflater.inflate(R.layout.fragment_edit_point, container, false);
      tvAlert = (TextView) v.findViewById(R.id.tvAlert);
      // getting data from the activity argument
      // https://stackoverflow.com/questions/12739909/send-data-from-activity-to-fragment-in-android
      EditPointActivity activity = (EditPointActivity) getActivity();
      String myData = activity.getArg();
      if (U.DEBUG) Log.d(U.TAG,"EditPointFragment:"+"Got data="+myData);
      
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
      
      bNoteOk.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mEd.updateNote();
        }
      });
            
      mEd.adoptPoint(extractId());
      mPoint=mEd.getPoint();
      U.enlargeFont(getActivity(), new TextView[] {tvId, tvType, tvComment} );
      return v;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.edit_point_fragment, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_center) {
        if (mPoint != null && mPoint.hasCoords()) {
          JSbridge jsb=mModel.getJSbridge();
          jsb.exportLatLon(mPoint.lat,mPoint.lon);
          jsb.setDirty();
          Point p2=(Point) mPoint.clone();
          mPointList.setProximityOrigin(p2);
        }
        return true;
      }
      if (id == R.id.action_map) {
        if (mPoint != null && mPoint.hasCoords()) {
          JSbridge jsb=mModel.getJSbridge();
          jsb.exportLatLon(mPoint.lat,mPoint.lon);
          jsb.setDirty();
        }
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
        if (mPoint.isProtected()) { tvAlert.setText("This point is protected"); }
        else { requestConfirmation(R.id.action_delete, R.string.action_delete); }
        return true;
      }
      if (id == R.id.action_coords) {
        mEd.findCoords();
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
        if (mEd.die()) exit("Ok");
      }
    }
    
    private int extractId() {
      // getting data from the activity argument
      // https://stackoverflow.com/questions/12739909/send-data-from-activity-to-fragment-in-android
      EditPointActivity activity = (EditPointActivity) mFragment.getActivity();
      String myData = activity.getArg();
      if (U.DEBUG) Log.d(U.TAG,"Editor:"+"Got id="+myData);
      int iid=Integer.parseInt(myData);
      return iid;
    }
    
    private class Editor implements PointReceiver {
      private Point p;
      
      public void adoptPoint(int iid) {
        p = mPointList.getById(iid);
        if (p == null) {
          tvAlert.setText("No point with id="+iid);
          return;
        }
        fillForm();
      }
      
      public Point getPoint() { return p; }
      
      private void fillForm() { fillForm(p); }
      
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
      
      public void toggleProtect() {
        if (p.isProtected()) p.unprotect();
        else p.protect();
        mPointList.update(p);
        fillForm();
      }
      
      public boolean die() {
        if (p.isProtected()) {
          tvAlert.setText("Cannot delete a protected point");
          return false;
        }
        mPointList.moveUnprotectedToTrash(p.getId());
        mModel.getJSbridge().setDirty();
        return true;
      }
      
      public void findCoords() {
        //tvAlert.setText("Sorry, not implemented in the free version");
        if ( ! p.getType().equals("cell") || p.cellData == null || p.cellData.length() < 10) {
          tvAlert.setText("This point is not a cell");
          return;
        }
        ResolverWrapper rw=new ResolverWrapper(tvAlert,tvNull);
        CellInformer ci=new CellInformer();
        //ci.setFragment(mFragment);
        ci.onlyResolve(rw, mEd, mEd.getPoint());
      }
      
      public void updateComment(String s) {
        p.setComment(s);
        mPointList.update(p);
        mModel.getJSbridge().setDirty();
        fillForm();
      }
      
      public void updateNote() {
        String s=etNote.getText().toString();
        p.setNote(s);
        mPointList.update(p);
        fillForm();
      }
      
      @Override
      public void onPointavailable(Point p) { 
      // callback after resolving cell to coords
        tvNull.setVisibility(View.GONE);
        tvAlert.setVisibility(View.VISIBLE);
        mPointList.update(p);
        mModel.getJSbridge().setDirty();
        fillForm(p);
      }
      
      private String ne(String s) {
        if (s == null || s.isEmpty()) return "";
        return s;
      }
      
      private String ne(int i) {
        if (i <= 0) return "";
        return String.valueOf(i);
      }
    }
    
    private class ResolverWrapper extends PointIndicator {
      
      public ResolverWrapper(TextView twP, TextView twD) {
        super(twP, twD);
      }      
    }
    
  }// end ListFragment

  @Override
  protected android.support.v4.app.Fragment createFragment() { return new EditPointFragment(); }
}
