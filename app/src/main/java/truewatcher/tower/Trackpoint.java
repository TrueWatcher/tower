package truewatcher.tower;

import java.util.Set;
import org.json.JSONArray;
import android.location.Location;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Trackpoint extends LatLon implements Cloneable {
  private int mId=0;
  private String mType="T";
  private static final Set<String> TYPES = new HashSet<String>(Arrays.asList(
          new String[] {"T","note"}
  ));
  public String alt="";
  public String range="";
  public String time=getDate();
  public String data="";
  private String mNewSegment="";
  public String comment="";
  // https://www.gpsvisualizer.com/tutorials/tracks.html
  public static final List<String> FIELDS = Collections.unmodifiableList(Arrays.asList(
          new String[] {"type","new_track","time","lat","lon","alt","range","name","data"}
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

  public Object clone() {
    try { return super.clone(); }
    catch ( CloneNotSupportedException e ) { return null; }
  }

  public void setId(int i) { mId=i; }
  public int getId() { return mId; }

  public void setType(String t) {
    if ( ! TYPES.contains(t) ) throw new U.RunException("Unhnown type="+t);
    mType=t;
  }

  public String getType() { return mType; }

  public void setNewSegment() { mNewSegment="1"; }
  public void setNewSegment(String s) { mNewSegment=s; }
  public boolean isNewSegment() { return ! mNewSegment.isEmpty(); }

  public static String getDate() {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String fd = df.format(Calendar.getInstance().getTime());
    return fd;
  }

  public JSONArray makeJsonPresentation() {
    JSONArray ja=new JSONArray();
    if ( ! mType.equals("T")) return ja;
    ja.put(lat);
    ja.put(lon);
    return ja;//.toString();
  }

  public String toCsv() {
    List<String> ls=new ArrayList<String>();
    ls.add(ne(mType));
    ls.add(ne(mNewSegment));
    ls.add(ne(time));
    ls.add(ne(lat));
    ls.add(ne(lon));
    ls.add(ne(alt));
    ls.add(ne(range));
    ls.add(ne(comment));
    ls.add(ne(data));
    String s= TextUtils.join(Trackpoint.SEP, ls);
    return s;
  }

  public Trackpoint fromCsv(String s) throws U.DataException {
    s=s.trim();
    if (s.isEmpty()) return null;
    String[] ls=TextUtils.split(s, Trackpoint.SEP);
    if (ls.length != Trackpoint.FIELDS.size()) {
      throw new U.DataException("Source has "+ls.length+" fields, while "+Trackpoint.FIELDS.size()+" are required");
    }
    setType(ls[0]);
    if ( ! ls[1].isEmpty()) setNewSegment(ls[1]);
    time=ls[2];
    lat=ls[3];
    lon=ls[4];
    alt=ls[5];
    range=ls[6];
    comment=ls[7];
    data=ls[8];
    return this;
  }

  private String ne(String s) {
    if (s == null || s.isEmpty()) return "";
    if (s.contains(SEP)) throw new U.RunException("Misplaced separator");
    if (s.contains(NL)) throw new U.RunException("Misplaced NL");
    return s;
  }
}
