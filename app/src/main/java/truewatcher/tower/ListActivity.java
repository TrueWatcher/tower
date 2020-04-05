package truewatcher.tower;

import android.app.Activity;
import android.support.v4.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Map;

public class ListActivity extends SingleFragmentActivity {

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
  
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);// delegate to the fragment
  }
  
  public void showConfirmationDialog(DialogFragment dialog) {
    dialog.show(getSupportFragmentManager(), "ConfirmationDialogFragment");
  }
  
  public static class ListPointsFragment extends Fragment 
      implements ConfirmationDialogFragment.ConfirmationDialogReceiver  {

    public static final String FLUSH="flush";
    private TextView tvAlert;
    private Fragment mFragment;
    private ListView lvListView;
    private PointList mPointList=Model.getInstance().getPointList();
    private ArrayList<String> mList;
    private ListHelper mListHelper = new ListHelper(mPointList);
    private ListHelper.MyArrayAdapter mAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      mFragment=this;
    }
      
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      View v = inflater.inflate(R.layout.fragment_list, container, false);
      tvAlert = (TextView) v.findViewById(R.id.tvAlert);
      lvListView = (ListView) v.findViewById(R.id.lvListView);
      
      if (mList == null) mList = mListHelper.getList();
      if (mList.isEmpty()) tvAlert.setText("No stored points");
      // Adapter must be created even if the list is empty
      mAdapter = new ListHelper.MyArrayAdapter(mFragment.getActivity(), R.layout.list_item_simple, mList);
      lvListView.setAdapter(mAdapter);
      lvListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
          int clickedId = mListHelper.getIdByPosition(position);
          if (U.DEBUG) Log.i(U.TAG,"ListPointsFragment:"+"Clicked at line="+position+
                  ", id="+String.valueOf(clickedId));
          Intent i = new Intent(mFragment.getActivity(), EditPointActivity.class);
          i.putExtra("id", clickedId);
          i.putExtra("caller", EditPointActivity.EditPointFragment.LIST);
          startActivityForResult(i, 1);
        }
      });

      //U.enlargeFont(getActivity(), new TextView[] {tvAlert} );
      tvAlert.setTextColor(U.MSG_COLOR);
      return v;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
      String result;
      if (requestCode != 1) return;
      if (resultCode == Activity.RESULT_OK) {
        Bundle bd = dataIntent.getExtras();       
        if (bd == null) {
          Log.e(U.TAG,"ListActivity:"+"No data received");
          return;
        }
        result = (String) bd.get("result");
        if (U.DEBUG) Log.d(U.TAG,"ListActivity:"+"Got result="+result);
        if (result.equals(ListActivity.ListPointsFragment.FLUSH)) {
          // quit LIST -- go to MAP
          mFragment.getActivity().finish();
          return;
        }
        adoptChanges();
      }
      else {
        if (U.DEBUG) Log.d(U.TAG,"ListFragment:"+"Got resultCode="+String.valueOf(resultCode));
        if (resultCode == Activity.RESULT_CANCELED) {// user had pressed BACK
          adoptChanges();
        }
      }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.list_fragment, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      int id = item.getItemId();
      if (id == R.id.action_map_from_list) {
        mFragment.getActivity().finish();
        return true;
      }
      if (id == R.id.action_file) {
        Intent fi=new Intent(this.getActivity(),FileActivity.class);
        startActivity(fi);
        return true;
      }
      if (id == R.id.action_sort_id) {
        mListHelper.setSort("id");
        adoptChanges();
        return true;
      }
      if (id == R.id.action_sort_reverse_id) {
        mListHelper.setSort("rid");
        adoptChanges();
        return true;
      }
      if (id == R.id.action_sort_proximity) {
        String locp=mListHelper.getLocationPresentation();
        if (locp == null) {
          tvAlert.setText("No current location");
        }
        else {
          mListHelper.setSort("pr");
          tvAlert.setText("Distances from: "+locp);
          adoptChanges();
        }
        return true;
      }
      if (id == R.id.action_renumber) {
        requestConfirmation(R.id.action_renumber, R.string.action_renumber);
        return true;
      }
      if (id == R.id.action_wipe) {
        requestConfirmation(R.id.action_wipe, R.string.action_delete);
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
    
    private void requestConfirmation(int actionId, int actionStringId) {
      ConfirmationDialogFragment dialog = new ConfirmationDialogFragment();
      Map<String,String> args=new ArrayMap<String,String>();
      args.put("actionId",String.valueOf(actionId));
      args.put("actionStringId",String.valueOf(actionStringId));
      dialog.setArguments(U.map2bundle(args));
      dialog.setTargetFragment(mFragment, 1);
      ((ListActivity) getActivity()).showConfirmationDialog(dialog);     
    }
        
    public void onConfirmationNegative(int id) {
      if (U.DEBUG) Log.d(U.TAG,"ListFragment:"+"ConfirmationNegative:"+id);
    }
    
    public void onConfirmationPositive(int id) {
      if (U.DEBUG) Log.d(U.TAG,"ListFragment:"+"ConfirmationPositive:"+id);
      if (id == R.id.action_wipe) {
        mPointList.clear();
        if (U.DEBUG) Log.d(U.TAG,"ListFragment:"+"New size="+mPointList.getSize());
        adoptChanges();
        return;
      }
      if (id == R.id.action_renumber) {
        mPointList.renumber();
        if (U.DEBUG) Log.d(U.TAG,"ListFragment:"+"New first="+mPointList.getIdByIndex(0));
        adoptChanges();
        return;
      }
    }
    
    private void adoptChanges() {
      mList = mListHelper.getList();
      mAdapter.changeObjects(mList);
      if (mList.isEmpty()) tvAlert.setText("No stored points");
      if (mPointList.isDirty()) {
        Model.getInstance().getJSbridge().onPoinlistmodified();
      }
      trySavePoints();
    }
    
    private void trySavePoints() {
      if ( ! mPointList.isDirty()) return;
      String res=mPointList.save();
      if ( ! res.equals(PointList.OK)) tvAlert.setText(res);
      if (U.DEBUG) Log.d(U.TAG,"ListFragment_trySavePoints:"+res);
    }
    
    @Override
    public void onResume() {
      super.onResume();
      String locp=mListHelper.getLocationPresentation();
      if (locp != null) {
      // some point might have been set as center -- redisplay list with proxymities
        if (U.DEBUG) Log.d(U.TAG,"ListActivity_onResume:"+"re-display with proxymities");
        mListHelper.setShowProximity();
        mList = mListHelper.getList();
        mAdapter.changeObjects(mList);
        if (mList.isEmpty()) tvAlert.setText("No stored points");
        else tvAlert.setText("Distances from: "+locp);
      }
      if (mPointList.isDirty()) {
      // the list itself may be changed -- redisplay list and mark JSbridge for map redisplay
        if (U.DEBUG) Log.d(U.TAG,"ListActivity_onResume:"+"re-display");
        adoptChanges();
      }
    }
    
  }// end ListFragment
  
  @Override
  protected Fragment createFragment() { return new ListPointsFragment(); }
}

  