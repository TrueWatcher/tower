package truewatcher.signaltrackwriter;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

public class SettingsActivity extends SingleFragmentActivity {

  public static class SettingsPageFragment extends Fragment {

    private MyRegistry mRg=MyRegistry.getInstance();
    private EditText etGpsMinDistance, etGpsMinDelay, etCellFilter;
    private CheckBox cbTrackShoudWrite, cbUseSAF, cbUseMediaFolder, cbUseTowerFolder;
    private TextView tvWorkingFileFull;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // do not show keyboard
      getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      View v = inflater.inflate(R.layout.fragment_settings, container, false);
      etGpsMinDistance = (EditText) v.findViewById(R.id.etGpsMinDistance);
      etGpsMinDelay = (EditText) v.findViewById(R.id.etGpsMinDelay);
      //cbTrackShoudWrite = (CheckBox) v.findViewById(R.id.cbTrackShoudWrite);
      cbUseSAF = (CheckBox) v.findViewById(R.id.cbUseSAF);
      cbUseMediaFolder = (CheckBox) v.findViewById(R.id.cbUseMediaFolder);
      //cbUseTowerFolder = (CheckBox) v.findViewById(R.id.cbUseTowerFolder);
      tvWorkingFileFull = (TextView) v.findViewById(R.id.tvWorkingFileFull);
      etCellFilter = (EditText) v.findViewById(R.id.cellFilter);
      //if (null == etGpsMinDistance) Log.e(U.TAG,"null!!!");
      setupEditText(etGpsMinDistance, "gpsMinDistance");
      setupEditText(etGpsMinDelay, "gpsMinDelayS");
      //setupCheckBox(cbTrackShoudWrite, "trackShouldWrite")
      setupCheckBox(cbUseSAF, "useSAF");;
      setupCheckBox(cbUseMediaFolder, "useMediaFolder");
      //setupCheckBox(cbUseTowerFolder, "useTowerFolder");
      tvWorkingFileFull.setText(Model.getInstance().getTrackStorage().getWorkingFileFull());
      setupEditText(etCellFilter, "cellFilter");
      return v;
    }

    private void setupEditText(EditText et, String k) {
      et.setText((String) mRg.get(k));
      final String mKey=k;
      // https://alvinalexander.com/source-code/android/android-programming-how-save-edittext-changes-without-save-button-ie-text-change
      et.addTextChangedListener(new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int start, int before, int count) {
          mRg.set(mKey , c.toString());
          mRg.saveToShared(getActivity(),mKey);
        }
        @Override
        public void beforeTextChanged(CharSequence c, int start, int count, int after) {}
        @Override
        public void afterTextChanged(Editable c) {}
      });
    }

    private void setupCheckBox(CheckBox cb, String k) {
      cb.setChecked(mRg.getBool(k));
      final String mKey=k;
      // https://alvinalexander.com/source-code/android/android-programming-how-save-edittext-changes-without-save-button-ie-text-change
      cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          //cb.setChecked(isChecked);
          mRg.set(mKey, isChecked);
          mRg.saveToShared(getActivity(),mKey);
        }
      });
    }

  }// end MainPageFragment

  @Override
  protected Fragment createFragment() { return new SettingsPageFragment(); }
}
