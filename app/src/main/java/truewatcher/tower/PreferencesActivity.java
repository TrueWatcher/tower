package truewatcher.tower;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    Preference pAlert;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, String rootKey) {
      //if (U.DEBUG) Log.i(U.TAG,"at PreferencesFragment.onCreatePreferences");
      setHasOptionsMenu(true);
      setPreferencesFromResource(R.xml.prefs, rootKey);
      pAlert=getPreferenceManager().findPreference("pAlert");
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

      if ( ! mRegistry.getBool("isKeylessDistro")) {
        PreferenceScreen screen = getPreferenceScreen();
        key="yandexMapKey";
        screen.removePreference(findPreference(key));
        key="yandexLocatorKey";
        screen.removePreference(findPreference(key));
      }

      if (mRegistry.noAnyKeys()) {
        pAlert.setSummary(getString(R.string.keyless_warning));
        //if (U.DEBUG) Log.d(U.TAG,"PreferencesFragment"+"Warning shown");
      }
    }
    
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      if ( ! mRegistry.keyExists(key)) {
        Log.e(U.TAG, "PreferencesFragment:"+"Unknown key:"+key+"!");
        return;
      }
      Map<String, ?> mp = prefs.getAll();
      String checkedApi = checkApiKey(key, String.valueOf(mp.get(key)) );
      if (checkedApi.isEmpty()) { pAlert.setSummary(""); }
      else { // change was denied -- no api key
        pAlert.setSummary(checkedApi);
        return;
      }
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
      if (U.DEBUG) Log.i(U.TAG, "PreferencesFragment:"+"Preference "+key+" set to "+mRegistry.get(key));
      // update the value on the settings screen
      if (null != getPreferenceManager().findPreference(key)) {
      // some prefs are not on the screen  
        getPreferenceManager().findPreference(key).setSummary(mRegistry.get(key));
      }
      adjustApiKeys(key);

      if (key.equals("mapZoom")) {
        Model.getInstance().getJSbridge().exportZoom(mRegistry.get(key));
      }
      if (key.equals("mapProvider") || key.equals("mapZoom")) {
        Model.getInstance().getJSbridge().setDirty();// to redraw map
      }
    }

    private void _showAlert(String text) {
      getPreferenceManager().findPreference("pAlert").setSummary(text);
    }

    private String checkApiKey(String key, String value) {
      String alertTemplate="Your app has no API key for %s. You can obtain it for free at %s" +
              " and enter in the appropriate field below";
      CheckPoint c=tryChekpoints(key,value);
      if (c == null) return "";
      getPreferenceScreen().getSharedPreferences()
              .unregisterOnSharedPreferenceChangeListener(this);
      mRegistry.set(key,c.fallbackValue);
      mRegistry.saveToShared(getActivity(),key);
      ListPreference lp = (ListPreference) findPreference(key);
      lp.setValue(c.fallbackValue);
      lp.setSummary(c.fallbackValue);
      getPreferenceScreen().getSharedPreferences()
              .registerOnSharedPreferenceChangeListener(this);
      return String.format(alertTemplate, c.serviceName, c.issuerUrl);
    }

    private static class CheckPoint {
      public String key;
      public String value;
      public String keyKey;
      public String fallbackValue;
      public String serviceName;
      public String issuerUrl;

      public CheckPoint(String aKey, String aValue, String aKeyKey, String aFallbackValue, String aServiceName, String aIssuerUrl) {
        key=aKey;
        value=aValue;
        keyKey=aKeyKey;
        fallbackValue=aFallbackValue;
        serviceName=aServiceName;
        issuerUrl=aIssuerUrl;
      }
    }

    private CheckPoint tryChekpoints(String key, String value) {
      int i=0;
      CheckPoint[] ck=new CheckPoint[2];
      ck[0]=new CheckPoint(
              "mapProvider","yandex hyb","yandexMapKey","osm map",
              "Yandex Maps","https://developer.tech.yandex.com/" );
      ck[1]=new CheckPoint(
              "cellResolver","yandex", "yandexLocatorKey", "mylnikov",
              "Yandex Locator", "https://yandex.ru/dev/locator/keys/get/" );

      CheckPoint c;
      for (i=0; i < ck.length; i+=1) {
        c=ck[i];
        if ( c.key.equals(key) && c.value.equals(value) && mRegistry.get(c.keyKey).isEmpty() ) return c;
      }
      return null;
    }

    private void adjustApiKeys(String key) {
      final List<String> watched=new ArrayList(Arrays.asList(
              new String[] {"yandexMapKey","yandexLocatorKey"}));
      if ( ! mRegistry.getBool("isKeylessDistro") || ! watched.contains(key)) return;
      getPreferenceScreen().getSharedPreferences()
              .unregisterOnSharedPreferenceChangeListener(this);
      mRegistry.set(key, mRegistry.getScrambled(key));
      mRegistry.saveToShared(getActivity(), key);
      getPreferenceManager().findPreference(key).setSummary("[obfuscated]");
      getPreferenceScreen().getSharedPreferences()
              .registerOnSharedPreferenceChangeListener(this);
    }

  }// end fragment
}