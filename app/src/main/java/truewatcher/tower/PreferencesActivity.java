package truewatcher.tower;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Map;

//https://storiesandroid.wordpress.com/2015/10/06/android-settings-using-preference-fragments/

public class PreferencesActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //if (U.DEBUG) Log.i(U.TAG,"at PreferencesActivity.onCreate");
    getSupportFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, new PreferencesFragment())
            .commit();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.prefs_activity, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_back) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
  
  public static class PreferencesFragment extends PreferenceFragmentCompat
          implements OnSharedPreferenceChangeListener {

    MyRegistry mRegistry = MyRegistry.getInstance();

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, String rootKey) {
      //if (U.DEBUG) Log.i(U.TAG,"at PreferencesFragment.onCreatePreferences");
      setHasOptionsMenu(true);
      setPreferencesFromResource(R.xml.prefs, rootKey);
      adjustPrefsScreen();
    }

    @Override
    public void onResume() {
      super.onResume();
      getPreferenceScreen().getSharedPreferences()
              .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
      super.onPause();
      getPreferenceScreen().getSharedPreferences()
              .unregisterOnSharedPreferenceChangeListener(this);
    }
    
    private void adjustPrefsScreen() {
      String key;

      key="cellResolver";
      ListPreference lpCellResolver = (ListPreference) findPreference(key);;
      if (lpCellResolver.findIndexOfValue(mRegistry.get(key)) < 0) {
        if (U.DEBUG) Log.d(U.TAG,"PreferencesFragment:"+"Unknown lpCellResolver value:"+mRegistry.get(key));
      }
      else { lpCellResolver.setValue(mRegistry.get(key)); }
      lpCellResolver.setSummary(mRegistry.get(key));

      key="mapProvider";
      ListPreference lpMapProvider = (ListPreference) findPreference(key);
      if (lpMapProvider.findIndexOfValue(mRegistry.get(key)) < 0) {
        if (U.DEBUG) Log.d(U.TAG,"PreferencesFragment"+"Unknown lpMapProvider value:"+mRegistry.get(key));
      }
      else { lpMapProvider.setValue(mRegistry.get(key)); }
      lpMapProvider.setSummary(mRegistry.get(key));

      key="mapZoom";
      EditTextPreference etpMapZoom = (EditTextPreference) findPreference(key);
      etpMapZoom.setText(mRegistry.get(key));
      etpMapZoom.setSummary(mRegistry.get(key));

      key="maxPoints";
      EditTextPreference etpMaxPoints = (EditTextPreference) findPreference(key);
      //((TextView) etpMapZoom).setInputType(InputType.TYPE_CLASS_NUMBER);
      etpMaxPoints.setText(mRegistry.get(key));
      etpMaxPoints.setSummary(mRegistry.get(key));

      key="useTrash";
      SwitchPreferenceCompat swUseTrash = (SwitchPreferenceCompat) findPreference(key);
      swUseTrash.setChecked(mRegistry.getBool(key));
    }
    
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      if ( ! mRegistry.keyExists(key)) {
        Log.e(U.TAG, "PreferencesFragment:"+"Unknown key:"+key+"!");
        return;
      }
      Map<String, ?> mp = prefs.getAll();
      if (key.equals("maxPoints")) {
        String filtered=U.enforceInt(MyRegistry.INT_KEYS, key, String.valueOf(mp.get(key)));
        int demanded=Integer.valueOf(filtered);
        int adopted=Model.getInstance().getPointList().adoptMax(demanded);
        mRegistry.set(key, adopted);
        if (adopted != demanded) {
          if (U.DEBUG) Log.d(U.TAG,"PreferencesFragment:"+"maxPoints set to "+adopted+", not to "+demanded);
        }
      }
      else {
        // make sure there're no letters in numbers
        String filtered=U.enforceInt(MyRegistry.INT_KEYS, key, String.valueOf(mp.get(key)));
        mRegistry.set(key, filtered);
      }
      if (key.equals("mapZoom")) {
        Model.getInstance().getJSbridge().exportZoom(mRegistry.get(key));
      }
      if (key.equals("mapProvider") || key.equals("mapZoom")) {
        Model.getInstance().getJSbridge().setDirty();// to redraw map
      }
      if (U.DEBUG) Log.i(U.TAG, "PreferencesFragment:"+"Preference "+key+" set to "+mRegistry.get(key));
      // update the value on the settings screen
      if (null != getPreferenceManager().findPreference(key)) {
      // some prefs are not on the screen  
        getPreferenceManager().findPreference(key).setSummary(mRegistry.get(key));
      }
    }

  }// end fragment
}