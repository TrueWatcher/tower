package truewatcher.tower;

import android.content.Context;
import java.text.SimpleDateFormat;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public abstract class U {

  public static final String TAG = "tower";

  public static boolean DEBUG = BuildConfig.DEBUG;//true;//

  public static void debugOn() {
    U.DEBUG = true;
  }

  public static final String H = "https://";

  public static int MSG_COLOR = Color.parseColor("#0000ff");

  static class RunException extends RuntimeException {
    public RunException(String s) {
      super(s);
    }
  }

  static class DataException extends Exception {
    public DataException(String s) {
      super(s);
    }
  }

  static class FileException extends Exception {
    public FileException(String s) {
      super(s);
    }
  }

  static class UserException extends Exception {
    public UserException(String s) {
      super(s);
    }
  }

  public static long getTimeStamp() {
    return (long) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
  }

  public static final int minFixDelayS=5;

  // Gets integer part of a number, represented as string
  public static String str2int(String value) {
    int posDot = value.indexOf(".");
    if (posDot < 0) return value;
    String[] parts = value.split(Pattern.quote("."));
    if (parts.length != 2) {
      Log.e(U.TAG, "U_str2int:" + "Wrong argument=" + value);
    }
    return parts[0];
  }

  // Concatenates arrays
  public static <T> T[] arrayConcat(T[] first, T[] second) {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  // Produces a map from a key list and a value list
  public static Map<String, String> arrayCombine(List<String> keys, String[] values) {
    Map<String, String> m = new ArrayMap<String, String>();
    for (int j = 0; j < keys.size(); j += 1) {
      m.put(keys.get(j), values[j]);
    }
    return m;
  }

  // Tests if a string is present in an array of strings
  public static boolean arrayContains(String[] haystack, String needle) {
    return Arrays.asList(haystack).contains(needle);
  }

  // Sorts a map by values in ascending or descending order
  public static Map<Integer, Double> sortByComparator(Map<Integer, Double> unsortMap, final boolean order) {
    // https://stackoverflow.com/questions/8119366/sorting-hashmap-by-values
    //@param order: true=ASC false=DESC
    List<Entry<Integer, Double>> list = new LinkedList<Entry<Integer, Double>>(unsortMap.entrySet());

    Collections.sort(list, new Comparator<Entry<Integer, Double>>() {
      @Override
      public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
        if (order) {
          return o1.getValue().compareTo(o2.getValue());
        } else {
          return o2.getValue().compareTo(o1.getValue());
        }
      }
    });
    Map<Integer, Double> sortedMap = new LinkedHashMap<Integer, Double>();
    for (Entry<Integer, Double> entry : list) {
      sortedMap.put(entry.getKey(), entry.getValue());
    }

    return sortedMap;
  }

  // replaces the target list content (pointer remains the same)
  public static <T> void refillList(List<T> target, List<T> source) {
    int lSource = source.size();
    int lTarget = target.size();
    int underflow = lTarget - lSource;
    int firstOver;
    for (int i = 0; i < lSource; i += 1) {
      if (i < lTarget) target.set(i, source.get(i));
      else target.add(source.get(i));
    }
    if (underflow > 0) {
      firstOver = lSource;
      for (int i = 0; i < underflow; i += 1) {
        if (U.DEBUG) Log.d(U.TAG, "U_refillList:" + "Removing at " + firstOver);
        //target.remove(i); crashes if several points deleted
        target.remove(firstOver);
      }
    }
  }

  // a great distance - to put empty points to bottom
  public static final double FAR = 1e8;

  // calculates distance in meters from coords (assumed spherical, aka haversine)
  public static double proximityM(LatLon p, LatLon center) {
    if ( ! p.hasCoords()) return FAR;
    double earthRadius = 6371000;
    double deg2rad = 0.0174532925199433;

    double dLat = deg2rad * (Double.parseDouble(center.lat) - Double.parseDouble(p.lat));
    double dLon = deg2rad * (Double.parseDouble(center.lon) - Double.parseDouble(p.lon));
    double lat1 = deg2rad * (Double.parseDouble(p.lat));
    double lat2 = deg2rad * (Double.parseDouble(center.lat));
    double slat = Math.sin(dLat / 2);
    double slon = Math.sin(dLon / 2);
    double a = slat * slat + slon * slon * Math.cos(lat1) * Math.cos(lat2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadius * c;
  }

  public static double azimuth(Point p, Point center) {
    if (center.lat.equals(p.lat) && center.lon.equals(p.lon)) return 1000;
    double dLat = (Double.parseDouble(center.lat) - Double.parseDouble(p.lat));
    double dLon = (Double.parseDouble(center.lon) - Double.parseDouble(p.lon));
    double deg2rad = 0.0174532925199433;
    double cosLat = Math.cos(Double.parseDouble(center.lat) * deg2rad);
    return Math.atan2(dLon * cosLat, dLat) / deg2rad;
  }

  // find square distance in arbitrary units, fast for sorting
  public static double sqDistance(Point p, Point center) {
    double deg2rad = 0.0174532925199433;
    double cosLat = Math.cos(Double.parseDouble(center.lat) * deg2rad);
    return sqDistance(p, center, cosLat);
  }

  public static double sqDistance(Point p, Point center, double cosLat) {
    if ( ! p.hasCoords()) return FAR*FAR;
    double dLat = (Double.parseDouble(center.lat) - Double.parseDouble(p.lat));
    double dLon = (Double.parseDouble(center.lon) - Double.parseDouble(p.lon));
    double dx = dLon * cosLat;
    return dLat * dLat + dx * dx;
  }

  // set extension in a full file name
  public static String assureExtension(String fileName, String ext) {
    String[] nameExt = fileName.split(Pattern.quote("."));
    int l = nameExt.length;
    if (l == 0) {
      Log.e(U.TAG, "U_assureExtension:" + "Wrong FILENAME=" + fileName);
    }
    if (l == 1) return fileName + "." + ext;
    nameExt[l - 1] = ext;
    return nameExt[0] + "." + nameExt[l - 1];
    //String.join((CharSequence) ".",(CharSequence[]) nameExt); gives error
  }

  // a data object to return details of file operations
  static class Summary {
    public String fileName;
    public String act;
    public int found;
    public int adopted;
    public int segments=0;

    public Summary(String aAct, int aFound, int aAdopted, String file) {
      fileName = file;
      act = aAct;
      found = aFound;
      adopted = aAdopted;
    }

    public Summary(String aAct, int aFound, int aAdopted, String file,int aSegments) {
      this(aAct, aFound, aAdopted, file);
      segments = aSegments;
    }
  }

  static class Summary2 extends Summary {
    public String segMap="";
    public long away=0;

    public Summary2(String aAct, int aFound, int aAdopted, String file,int aSegments, String aSegMap, long aAway) {
      super(aAct, aFound, aAdopted, file, aSegments);
      segMap=aSegMap;
      away=aAway;
    }

  }

  // deletes a file
  public static Summary unlink(String path, String fileExt) {
    try {
      File file = new File(path, fileExt);
      file.delete();
    } catch (Exception e) {
      Log.e(U.TAG, "U_unlink:" + e.getMessage());
      return new Summary("failed to delete", 0, 0, fileExt);
    }
    return new Summary("deleted", 0, 0, fileExt);
  }

  // checks if a file exists
  public static File fileExists(String path, String name, String ext) {
    String ne = U.assureExtension(name, ext);
    //Log.d(U.TAG,"U_fileExists:","FILENAME="+ne);
    File file = new File(path, ne);
    if (file.exists()) return file;
    return null;
  }

  // reads a whole directory
  public static String[] getCatalog(String path) {
    String[] myDir = (new File(path)).list();
    return myDir;
  }

  // reads a directory? filtered by the given type
  public static String[] getCatalog(String path, String ext) {
    final String dotExt = "." + ext;
    FilenameFilter ff = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return (name.endsWith(dotExt));
      }
    };
    String[] myDir = (new File(path)).list(ff);
    //String[] myDir=(new File(mPath)).list(makeFilter("."+ext));
    return myDir;
  }

  // reads a whole file as one string
  //https://stackoverflow.com/questions/14376807/how-to-read-write-string-from-a-file-in-android
  public static String fileGetContents(String path, String fileName) throws IOException {
    File file = new File(path, fileName);
    int length = (int) file.length();
    byte[] bytes = new byte[length];
    FileInputStream in = new FileInputStream(file);
    try {
      in.read(bytes);
    } finally {
      in.close();
    }
    return new String(bytes);
  }

  // reads a file from /assets as one string
  public static String readAsset(Context context, String fileName) throws IOException {
    String s = "";
    InputStream stream = context.getResources().getAssets().open(fileName);
    int size = stream.available();
    byte[] buffer = new byte[size];
    stream.read(buffer);
    stream.close();
    s = new String(buffer);
    return s;
  }

  // saves a string as a file
  public static void filePutContents(String path, String fileNameExt, String buf, boolean isAppend) throws IOException {
    File file = new File(path, fileNameExt);
    FileOutputStream stream = new FileOutputStream(file, isAppend);
    try {
      stream.write(buf.getBytes());
    } finally {
      stream.close();
    }
  }

  // creates a bundle from a map
  public static Bundle map2bundle(Map<String, String> args) {
    Bundle bundle = new Bundle();
    for (Map.Entry<String, String> entry : args.entrySet()) {
      bundle.putString(entry.getKey(), entry.getValue());
    }
    return bundle;
  }

  // assures that a string is not longer than max
  public static String truncate(String s, int max) {
    if (s.length() > max) return s.substring(0, max);
    return s;
  }

  public static String truncate(double d, int max) {
    String s = String.valueOf(d);
    return truncate(s, max);
  }

  // Sets font size of TextView view elements to the size of EditText elements
  public static void enlargeFont(Context context, TextView[] textViewList) {
    float ts = (new EditText(context)).getTextSize();// in px, not sp!
    for (int i = 0; i < textViewList.length; i += 1) {
      textViewList[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, ts);
    }
  }

  // removes non-digits from a value, if a key belongs to a list
  // makes a substitute for inputType=number
  public static String enforceInt(String[] intKeys, String key, String value) {
    if (Arrays.asList(intKeys).contains(key)) {
      String s = value.replaceAll("[^\\d]", "");
      if (s.isEmpty()) s = "0";
      return s;
    }
    return value;
  }

  public static boolean classExists(String className) {
    // BuildConfig.class.getCanonicalName()
    Class<?> objectClass;
    try {
      objectClass = Class.forName(className);
    } catch (ClassNotFoundException e) {
      return false;
    }
    return true;
  }

  public static boolean classHasField(Class<?> classClass, String fieldName) {
    Class<?> objectClass;
    try {
      classClass.getField(fieldName);
    } catch (NoSuchFieldException e) {
      return false;
    }
    return true;
  }

  // https://developer.android.com/reference/java/util/Date#getTimezoneOffset()
  public static float getTimeOffsetHr() {
    Calendar calendar = Calendar.getInstance(Locale.getDefault());
    float offset = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / (60 * 60 * 1000);
    return offset;
  }

  public static String utcToLocalTime(String utcDateTime) throws U.DataException {

    String dateFormateInLocalTimeZone = "";//Will hold the final converted date
    String dataUTC = "";
    Date localDate = null;
    SimpleDateFormat formatter;
    SimpleDateFormat parser;

    parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");// input format
    parser.setTimeZone(TimeZone.getTimeZone("UTC"));
    try {
      localDate = parser.parse(utcDateTime);
    } catch (ParseException e) {
      Log.e(U.TAG, "UTCtoLocalTime:" + "Failed to parse:" + utcDateTime + ", " + e.getMessage());
      throw new U.DataException(e.getMessage());
    }

    formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");//Set output format
    if (U.DEBUG) {
      formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
      dataUTC = formatter.format(localDate);
    }
    formatter.setTimeZone(TimeZone.getDefault());
    dateFormateInLocalTimeZone = formatter.format(localDate);
    if (U.DEBUG)
      Log.d(U.TAG, "UTCtoLocalTime: " + utcDateTime + "=" + dataUTC + ">" + dateFormateInLocalTimeZone);
    return dateFormateInLocalTimeZone;
  }

  public static String localTimeToUTC(String localDateTime) throws U.DataException {

    String dateFormateInUTC = "";//Will hold the final converted date
    String dataLocal = "";
    Date localDate = null;
    SimpleDateFormat formatter;
    SimpleDateFormat parser;

    parser = new SimpleDateFormat("yyyy-MM-dd HH:mm");// input format
    parser.setTimeZone(TimeZone.getDefault());
    try {
      localDate = parser.parse(localDateTime);
    } catch (ParseException e) {
      Log.e(U.TAG, "localTimeToUTC:" + "Failed to parse:" + localDateTime + ", " + e.getMessage());
      throw new U.DataException(e.getMessage());
    }

    formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm':30Z'");// output format
    if (U.DEBUG) {
      formatter.setTimeZone(TimeZone.getDefault());
      dataLocal = formatter.format(localDate);
    }
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    dateFormateInUTC = formatter.format(localDate);
    if (U.DEBUG)
      Log.d(U.TAG, "localTimeToUTC: " + localDateTime + "=" + dataLocal + ">" + dateFormateInUTC);

    return dateFormateInUTC;
  }

  // clears all shared preferences
  public static void clearPrefs(Context context) {
    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = pref.edit();
    editor.clear().commit();
  }

  public static String joinJsonArrays(String a1, String a2) {
    String empty="[]";
    int l1=a1.length();
    int l2=a2.length();
    if ( ! (a1.startsWith("[") && a1.endsWith("]")) ) throw new U.RunException("Wrong A1="+a1);
    if ( ! (a2.startsWith("[") && a2.endsWith("]")) ) throw new U.RunException("Wrong A2="+a1);
    if (a1.equals(empty)) return a2;
    if (a2.equals(empty)) return a1;
    String res=a1.substring(0, l1-1).concat(",").concat(a2.substring(1,l2));
    return res;
  }

  public static int countEntries(String haystack, String needle) {
    return (haystack.length() - haystack.replace(needle, "").length()) / needle.length();
  }
}
