package truewatcher.trackwriter;

import android.support.v4.util.ArrayMap;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class U {

  public static final String TAG="trackwriter";

  public static final boolean DEBUG=true;

  public static class UserException extends Exception {
    public UserException(String msg) { super(msg); }
  }

  public static class DataException extends Exception {
    public DataException(String msg) { super(msg); }
  }

  static class FileException extends Exception {
    public FileException(String s) { super(s); }
  }

  public static class RunException extends RuntimeException {
    public RunException(String msg) { super(msg); }
  }

  public static long getTimeStamp() {
    return (long) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
  }

  public static final int minFixDelayS=5;


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

  // a great distance - to put empty points to bottom
  public static final double FAR = 1e8;

  // calculates distance in meters from coords (assumed spherical, aka haversine)
  public static double proximityM(LatLon p, LatLon center) {
    if ( p.lat == null || p.lon == null || p.lat.isEmpty() || p.lon.isEmpty() ) return FAR;
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

  // Produces a map from a key list and a value list
  public static Map<String, String> arrayCombine(List<String> keys, String[] values) {
    Map<String, String> m = new ArrayMap<String, String>();
    for (int j = 0; j < keys.size(); j += 1) {
      m.put(keys.get(j), values[j]);
    }
    return m;
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

}
