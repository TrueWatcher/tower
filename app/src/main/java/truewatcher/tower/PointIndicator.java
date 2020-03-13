package truewatcher.tower;

import android.view.View;
import android.widget.TextView;

public class PointIndicator {

  protected TextView twProgress;
  protected TextView twData;

  public PointIndicator(TextView twP, TextView twD) {    
    twProgress=twP;
    twData=twD;    
    twProgress.setText("");
    twData.setText("");
  }

  public void initProgress() { 
    twProgress.setText("");
    showProgress();
  }
  
  public void showProgress(String p) { 
    twProgress.setText(p);
    showProgress();
  }

  public void showProgress() {
    twProgress.setVisibility(View.VISIBLE);
  }

  public void addProgress(String p) { addProgress(p, ", "); }

  public void addProgress(String p, String separator) {
    String t = (String) twProgress.getText();
    if (t.length() > 0) t=t+separator;
    twProgress.setVisibility(View.VISIBLE);
    twProgress.setText(t+p);
  }

  public void hideProgress() { 
    //twProgress.setText("");
    twProgress.setVisibility(View.GONE);
  }

  public void showData(String d) { 
    twData.setVisibility(View.VISIBLE);
    twData.setText(d);
  }

  public void addData(String d, String separator) {
    twData.setVisibility(View.VISIBLE);
    String t = (String) twData.getText();
    if (t.length() > 0) t=t+separator;
    twData.setText(t+d);
  }
  
  public void addData(String d) { addData(d, ","); }

  public void hideData() { 
    //twProgress.setText("");
    twData.setVisibility(View.GONE);
  } 
  
  public static String truncate(String s,int max) {
    if ( s.length() > max ) return s.substring(0,max);
    return s;
  }
  
  public static String floor(String s) {
    if (s == null) return "";
    if (s.isEmpty()) return s;
    String r=s;
    int p=s.indexOf(".");
    if (p > 0) r=r.substring(0,p);
    return r;
  }

  public void clearIndicator() {
    twProgress.setText("");
    twData.setText("");
  }

  public void hideIndicator() {
    hideProgress();
    hideData();
  }

}
