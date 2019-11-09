package truewatcher.tower;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.RadioButton;

public class DeeperRadioGroup {

  private int mCheckedRadio=-1;

  public DeeperRadioGroup(ViewGroup radioContainer) {
    setRadioExclusiveClick(radioContainer);
  }

  public int getCheckedRadioButtonId() { return mCheckedRadio; }

  // https://stackoverflow.com/questions/10461005/how-to-group-radiobutton-from-different-linearlayouts
  private void setRadioExclusiveClick(ViewGroup parent) {
    final List<RadioButton> radios = getRadioButtons(parent);

    for (RadioButton radio: radios) {
      radio.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          RadioButton r = (RadioButton) v;
          r.setChecked(true);
          mCheckedRadio=r.getId();
          for (RadioButton r2:radios) { if (r2.getId() != r.getId()) r2.setChecked(false); }
        }
      });
    }
  }

  private List<RadioButton> getRadioButtons(ViewGroup parent) {      
    List<RadioButton> radios = new ArrayList<RadioButton>();
    for (int i=0;i < parent.getChildCount(); i++) {
      View v = parent.getChildAt(i);
      if (v instanceof RadioButton) {
        radios.add((RadioButton) v);
        if (((RadioButton)v).isChecked()) mCheckedRadio=v.getId();
      }
      else if (v instanceof ViewGroup) {
        List<RadioButton> nestedRadios = getRadioButtons((ViewGroup) v);
        radios.addAll(nestedRadios);
      }
    }
    return radios;
  }
}