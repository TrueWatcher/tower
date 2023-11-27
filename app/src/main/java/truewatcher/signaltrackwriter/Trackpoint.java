package truewatcher.signaltrackwriter;

import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Trackpoint extends LatLon implements Cloneable {
  public String id="";
  private String mType="T";
  private static final Set<String> TYPES = new HashSet<String>(Arrays.asList(
          new String[] {"T","note"}
  ));;
  public String alt="";
  public String range="";
  public String time=getDate();
  public String comment="";
  public String data="";
  public String data1="";
  private String mNewSegment="";
  // https://www.gpsvisualizer.com/tutorials/tracks.html
  public static final List<String> FIELDS = Collections.unmodifiableList(Arrays.asList(
          new String[] {"id","type","new_track","time","lat","lon","alt","range","comment","data","data1"}
          ));
  public static final String SEP = ";";
  public static final String NL = "\n";

  public Trackpoint() { }

  public Trackpoint(String aType, String aComment, String aData) {
    if ( ! aType.equals("note")) throw new U.RunException("Trackpoint note of wrong type="+aType);
    setType(aType);
    this.comment=aComment;
    this.data=aData;
  }

  public Trackpoint(Location loc) {
    this.lat=String.valueOf(loc.getLatitude());
    this.lon=String.valueOf(loc.getLongitude());
    if (loc.hasAltitude()) this.alt=String.valueOf(loc.getAltitude());
    if (loc.hasAccuracy()) this.range=String.valueOf(loc.getAccuracy());
  }

  public Trackpoint(String lat, String lon) {
    this.lat=lat;
    this.lon=lon;
  }

  public void addCell(JSONObject cellData) {
    this.setComment("S");
    JSONObject[] signalCellData = this.separateSignalFromCell(cellData);
    this.setData(signalCellData[0].toString());
    this.setData1(signalCellData[1].toString());
  }

  private JSONObject[] separateSignalFromCell(JSONObject cellData) {
    JSONObject signalData =  new JSONObject();
    JSONObject cellOnlyData =  new JSONObject();
    try {
      signalData =  new JSONObject(cellData.toString());
      cellOnlyData =  new JSONObject(cellData.toString());
      for (Iterator<String> it = cellData.keys(); it.hasNext(); ) {
        String key = it.next();
        if (CellInformer.CELL_PARAMS.indexOf(key) >= 0) { signalData.remove(key); }
        else { cellOnlyData.remove(key); }
      }
    }
    catch (JSONException e) {
      Log.e(U.TAG, "JSOM error:"+e.getMessage());
      return new JSONObject[] { signalData, cellOnlyData };
    }
    return new JSONObject[] { signalData, cellOnlyData };
  }

  public Object clone() {
    try { return super.clone(); }
    catch ( CloneNotSupportedException e ) { return null; }
  }

  public void setIdInt(int i) { id=String.valueOf(i); }
  public int getIdInt() { return Integer.valueOf(id); }

  public void setType(String t) {
    if ( ! TYPES.contains(t) ) throw new U.RunException("Unhnown type="+t);
    mType=t;
  }

  public String getType() { return mType; }

  public void setNewSegment() { mNewSegment="1"; }
  public void setNewSegment(String s) { mNewSegment=s; }
  public boolean isNewSegment() { return ! mNewSegment.isEmpty(); }
  public String getNewSegment() { return mNewSegment; }

  public static String getDate() {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String fd = df.format(Calendar.getInstance().getTime());
    return fd;
  }

  public void setComment(String c) { comment=c; }
  public void setData(String d) { data=d; }
  public void setData1(String d) { data1=d; }

  public JSONArray makeJsonPresentation() {
    JSONArray ja=new JSONArray();
    if ( ! mType.equals("T")) return ja;
    ja.put(lat);
    ja.put(lon);
    return ja;//.toString();
  }

  public String toCsv() {
    List<String> ls=new ArrayList<String>();
    ls.add(ne(id));
    ls.add(ne(mType));
    ls.add(ne(mNewSegment));
    ls.add(ne(time));
    ls.add(ne(lat));
    ls.add(ne(lon));
    ls.add(ne(alt));
    ls.add(ne(range));
    ls.add(ne(comment));
    ls.add(ne(data));
    ls.add(ne(data1));
    String s= TextUtils.join(Trackpoint.SEP, ls);
    return s;
  }

  public Trackpoint fromCsv(String s) throws U.DataException {
    s=s.trim();
    if (s.isEmpty()) return null;
    if (s.indexOf(Trackpoint.NL) >= 0) {
      Log.e(U.TAG, "Found NL in supposed csv line at:"+String.valueOf(s.indexOf(Trackpoint.NL)));
    }
    String[] ls=TextUtils.split(s, Trackpoint.SEP);
    if (ls.length != Trackpoint.FIELDS.size()) {
      throw new U.DataException("Source has "+ls.length+" fields, while "+Trackpoint.FIELDS.size()+" are required");
    }
    id=ls[0];
    setType(ls[1]);
    if ( ! ls[2].isEmpty()) setNewSegment(ls[2]);
    time=ls[3];
    lat=ls[4];
    lon=ls[5];
    alt=ls[6];
    range=ls[7];
    comment=ls[8];
    data=ls[9];
    data1=ls[10];
    return this;
  }

  private String ne(String s) {
    if (s == null || s.isEmpty()) return "";
    if (s.contains(SEP)) throw new U.RunException("Misplaced separator");
    if (s.contains(NL)) throw new U.RunException("Misplaced NL");
    return s;
  }
}
