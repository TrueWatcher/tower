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

public class Point extends LatLon implements Cloneable {
  private String mType;
  private static final Set<String> TYPES = new HashSet<String>(Arrays.asList(
    new String[] {"cell","gps","mark"}
  ));
  public String alt;
  public String range;
  public String cellData;
  public String time;
  private int mId;
  private String mComment="";
  private String mNote="";
  private String mSym="";
  private boolean mProtect=false;
  public static final List<String> FIELDS = Collections.unmodifiableList(Arrays.asList(
    new String[] {"id","type","comment","protect","lat","lon","alt","range","time","cellData","note","sym"}
  ));
  public static final String SEP = ";";
  public static final String NL = "\n";

  public Point() { }
  
  public Point(String t) {
    setType(t);
  }
  
  public Point(String t, String lat, String lon) {
    setType(t);
    this.lat=lat;
    this.lon=lon;
  }
  
  public Point(String t, Location loc) {
    setType(t);
    this.lat=String.valueOf(loc.getLatitude());
    this.lon=String.valueOf(loc.getLongitude());
    if (loc.hasAltitude()) this.alt=String.valueOf(loc.getAltitude());
    if (loc.hasAccuracy()) this.range=String.valueOf(loc.getAccuracy());
  }
  
  public Object clone() {
    try { return super.clone(); }
    catch ( CloneNotSupportedException e ) { return null; }
  } 
  
  public void setType(String t) {
    if ( ! TYPES.contains(t) ) throw new U.RunException("Unhnown type="+t);
    mType=t;
  }
  
  public String getType() { return mType; }
    
  public void setComment(String s) {
    int maxChars=20;
    if (s.length() > maxChars) s=s.substring(0, maxChars);
    mComment=filterChars(s);
  }
  
  public String getNote() { return mNote; }
  
  public void setNote(String s) {
    int maxChars=100;
    if (s.length() > maxChars) s=s.substring(0, maxChars);
    mNote=filterChars(s);
  }
  
  public String getComment() { return mComment; }
  
  public void setId(int i) { mId=i; }
  public int getId() { return mId; }
  
  public static String getDate() {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    String fd = df.format(Calendar.getInstance().getTime());
    return fd;
  }
  
  public void setCurrentTime() { this.time=Point.getDate(); }
  
  public boolean isProtected() { return mProtect; }
  public void protect() { mProtect=true; }
  public void unprotect() { mProtect=false; }
  
  public JSONArray makeJsonPresentation(int index) {
    JSONArray ja=new JSONArray();
    if (index < 0) index=mId;
    ja.put(mType);
    ja.put(lat);
    ja.put(lon);
    String text=String.valueOf(index);
    if (mComment != null && ! mComment.isEmpty()) text+="."+mComment;
    ja.put(text);
    return ja;//.toString();
  }
  
  public String toCsv() {
    List<String> ls=new ArrayList<String>();
    ls.add(ne(mId));
    ls.add(ne(mType));
    ls.add(ne(mComment));
    ls.add(ne(mProtect));
    ls.add(ne(lat));
    ls.add(ne(lon));
    ls.add(ne(alt));
    ls.add(ne(range));
    ls.add(ne(time));
    ls.add(ne(cellData));
    ls.add(ne(mNote));
    ls.add(ne(mSym));
    String s= TextUtils.join(Point.SEP, ls);
    return s;
  }
  
  public Point fromCsv(String s) throws U.DataException {
    s=s.trim();
    String[] ls=TextUtils.split(s, Point.SEP);
    if (ls.length != Point.FIELDS.size()) {
      throw new U.DataException("Source has "+ls.length+" fields, while "+Point.FIELDS.size()+" are required");
    }
    mId=eInt(ls[0]);
    if ( ! TYPES.contains(ls[1])) {
      throw new U.DataException("Unknown type="+ls[1]+"!");
    }
    setType(ls[1]);
    setComment(ls[2]);
    mProtect=eBool(ls[3]);
    lat=ls[4];
    lon=ls[5];
    alt=ls[6];
    range=ls[7];
    time=ls[8];
    cellData=ls[9];
    mNote=ls[10];
    mSym=ls[11];
    return this;
  }
  
  private int eInt(String s) {
    if (s.isEmpty()) return 0;
    return Integer.valueOf(s);
  }
  
  private boolean eBool(String s) {
    if (s.isEmpty() || s.equals("false")) return false;
    return true;
  }
  
  private String ne(String s) {
    if (s == null || s.isEmpty()) return "";
    if (s.contains(Point.SEP)) throw new U.RunException("Misplaced separator");
    if (s.contains(Point.NL)) throw new U.RunException("Misplaced NL");
    return s;
  }
  
  private String ne(int i) {
    if (i <= 0) return "";
    return String.valueOf(i);
  }
  
  private String ne(boolean b) {
    if (b) return "true";
    return "";
  }
  
  public static String filterChars(String s) {
    // for a safe export to CSV and GPX
    s=s.replace(Point.SEP,",");
    s=s.replace(Point.NL,"/");
    s=s.replaceAll("&<>","*");
    return s;
  }
  
  public static String filterCharsMore(String s) {
    s=s.replace("\"","");
    s=s.replace(" ","_");
    s=s.replaceAll("[,;&<>~]","*");
    return filterChars(s);
  }
  
}
