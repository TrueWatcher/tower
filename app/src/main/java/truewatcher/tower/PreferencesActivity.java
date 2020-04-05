package truewatcher.tower;

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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
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
    Preference pAlert;

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
      pAlert=getPreferenceManager().findPreference("pAlert");

      prepareListPreference("mapProvider");
      prepareListPreference("cellResolver");
      prepareEditTextPref("mapZoom");
      prepareEditTextPref("maxPoints");
      prepareSwitchPref("useTrash");
      prepareSwitchPref("enableTrack");
      prepareSwitchPref("shouldCenterMapOnTrack");
      prepareEditTextPref("gpsMinDistance");
      prepareEditTextPref("gpsMinDelayS");

      if ( ! mRegistry.getBool("isKeylessDistro")) {
        PreferenceScreen screen = getPreferenceScreen();
        key="yandexMapKey";
        screen.removePreference(findPreference(key));
        key="yandexLocatorKey";
        screen.removePreference(findPreference(key));
      }

      //if (mRegistry.noAnyKeys()) { alert(getString(R.string.keyless_warning)); }
    }

    private ListPreference prepareListPreference(String key) {
      ListPreference lp = (ListPreference) findPreference(key);
      if (lp.findIndexOfValue(mRegistry.get(key)) < 0) {
        if (U.DEBUG) Log.d(U.TAG,"PreferencesFragment"+"Unknown "+key+" value:"+mRegistry.get(key));
      }
      else { lp.setValue(mRegistry.get(key)); }
      lp.setSummary(mRegistry.get(key));
      return lp;
    }

    private EditTextPreference prepareEditTextPref(String key) {
      EditTextPreference etp = (EditTextPreference) findPreference(key);
      etp.setText(mRegistry.get(key));
      etp.setSummary(mRegistry.get(key));
      return etp;
    }

    private SwitchPreferenceCompat prepareSwitchPref(String key) {
      SwitchPreferenceCompat swp = (SwitchPreferenceCompat) findPreference(key);
      swp.setChecked(mRegistry.getBool(key));
      return swp;
    }
    
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      if ( ! mRegistry.keyExists(key)) {
        Log.e(U.TAG, "PreferencesFragment:"+"Unknown key:"+key+"!");
        return;
      }
      Map<String, ?> mp = prefs.getAll();
      String checkedApi = rollbackIfNoKey(key, String.valueOf(mp.get(key)) );
      if (checkedApi.isEmpty()) { alert(""); }
      else { // change was denied -- no api key
        alert(checkedApi);
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
        // make sure there are no letters in numbers
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
      if (key.equals("mapProvider")) {
        Model.getInstance().getJSbridge().setDirty(3);// to redraw map
      }
      if (key.equals("enableTrack")) {
        syncCurrentTrack(mRegistry.getBool(key));
      }
    }

    private String rollbackIfNoKey(String key, String value) {
      String alertTemplate="Your app has no API key for %s. You can obtain it for free at %s%s" +
              " and enter in the appropriate field below";
      KeyCheck c= tryCheckKeys(key,value);
      if (c == null) return "";
      // roll back
      getPreferenceScreen().getSharedPreferences()
              .unregisterOnSharedPreferenceChangeListener(this);
      mRegistry.set(key,c.fallbackValue);
      mRegistry.saveToShared(getActivity(),key);
      ListPreference lp = (ListPreference) findPreference(key);
      lp.setValue(c.fallbackValue);
      lp.setSummary(c.fallbackValue);
      getPreferenceScreen().getSharedPreferences()
              .registerOnSharedPreferenceChangeListener(this);
      return String.format(alertTemplate, c.serviceName, U.H, c.issuerUrl);
    }

    private static class KeyCheck {
      public String key;
      public String value;
      public String keyKey;
      public String fallbackValue;
      public String serviceName;
      public String issuerUrl;

      public KeyCheck(String aKey, String aValue, String aKeyKey, String aFallbackValue, String aServiceName, String aIssuerUrl) {
        key=aKey;
        value=aValue;
        keyKey=aKeyKey;
        fallbackValue=aFallbackValue;
        serviceName=aServiceName;
        issuerUrl=aIssuerUrl;
      }
    }

    private KeyCheck tryCheckKeys(String key, String value) {
      int i=0;
      KeyCheck[] ck=new KeyCheck[] {
        new KeyCheck(
              "mapProvider","yandex hyb","yandexMapKey","osm map",
              "Yandex Maps","developer.tech.yandex.com/" ),
        new KeyCheck(
              "cellResolver","yandex", "yandexLocatorKey", "mylnikov",
              "Yandex Locator", "yandex.ru/dev/locator/keys/get/" )
      };

      KeyCheck c;
      for (i=0; i < ck.length; i+=1) {
        c=ck[i];
        if ( c.key.equals(key) && c.value.equals(value) && mRegistry.get(c.keyKey).isEmpty() ) return c;
      }
      return null;
    }

    private void adjustApiKeys(String key) {
      if ( ! mRegistry.getBool("isKeylessDistro") || ! U.arrayContains(MyRegistry.APIS, key) ) return;
      getPreferenceScreen().getSharedPreferences()
              .unregisterOnSharedPreferenceChangeListener(this);
      mRegistry.set(key, mRegistry.getScrambled(key));
      mRegistry.saveToShared(getActivity(), key);
      getPreferenceManager().findPreference(key).setSummary("[obfuscated]");
      getPreferenceScreen().getSharedPreferences()
              .registerOnSharedPreferenceChangeListener(this);
    }

    private void alert(String s) {
      Spannable ss = new SpannableString(s);
      ss.setSpan(new ForegroundColorSpan(U.MSG_COLOR), 0, ss.length(), 0);
      pAlert.setSummary(ss);
    }

    private void syncCurrentTrack(boolean isEnabled) {
      String buf="[[]]";
      if (isEnabled) {
        try {
          buf = Model.getInstance().getTrackStorage().trackCsv2LatLonString();
        }
        catch (Exception e) {
          alert("Error:" + e.getMessage());
        }
      }
      Model.getInstance().getJSbridge().replaceCurrentTrackLatLonJson(buf);
    }

  }// end fragment
}