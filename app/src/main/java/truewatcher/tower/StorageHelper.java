package truewatcher.tower;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import truewatcher.tower.U.DataException;

public class StorageHelper {
  private String mMyFile;
  private String mMyExt="csv";
  private String mHeader=TextUtils.join(Point.SEP, Point.FIELDS);
  private String mPath;
  private String mMyTrashFile="trash.csv";

  public static String getWorkingFolder(Context context, MyRegistry mRg) throws U.FileException {
    String nativeFolder = context.getExternalFilesDir(null).getPath();
    String targetPath = nativeFolder;
    if (mRg.getBool("useMediaFolder")) {
      String appMediaFolder = getMediaDir(context);
      Log.i(U.TAG, "appMediaFolder=" + appMediaFolder);
      if (appMediaFolder.isEmpty()) throw new U.FileException("Cannot find the app's media folder");
      targetPath = appMediaFolder;
    }
    return targetPath;
  }

  public static String getMediaDir(Context context) {
    // context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) gives a subfolder of Android/data
    //https://stackoverflow.com/questions/69658332/how-to-create-folder-inside-android-media-in-android-11
    File[] dirs = new File[0];
    dirs = context.getExternalMediaDirs();
    for (int i = 0; i<dirs.length; i+=1){
      if (dirs[i].getName().contains(context.getPackageName())) {
        return dirs[i].getAbsolutePath();
      }
    }
    return "";
  }

  public void init(String targetPath, String myFile) {
    if (U.DEBUG) Log.i(U.TAG, "My path=" + targetPath);
    mPath = targetPath;
    mMyFile=U.assureExtension(myFile, mMyExt);
  }
  public void init(Context context, String myFile, MyRegistry mRg) throws U.FileException {
    String targetPath = getWorkingFolder(context, mRg);
    init(targetPath, myFile);
  }

  public void trySetMyFile(String f) throws U.FileException {
    if (U.fileExists(mPath, f, mMyExt) == null) {
      Log.e(U.TAG, "StorageHelper_trySetMyFile:"+"Unknown file"+f);
      throw new U.FileException("Wrong file name:"+f);
    }
    mMyFile=U.assureExtension(f, mMyExt);
  }

  public String getMyFile() { return mMyFile; }

  public void savePoints(PointList pl) throws IOException,U.DataException { writePoints(pl, mMyFile, 0, -1, ""); }

  public U.Summary writePoints(PointList pl, String targetFile, int from, int until, String convertTo)
      throws IOException,U.DataException {
    int count=0;
    int fullCount=0;
    Point p;
    int id;
    U.Summary outcome;
    String buf=mHeader+Point.NL;
    StringBuilder sb=new StringBuilder(buf);
    while ((p=pl.iterate()) != null) {
      fullCount+=1;
      id=p.getId();
      if (id >= from && (until < 0 || id <= until)) {
        sb.append(p.toCsv()).append(Point.NL);
        count+=1;
      }
    }
    buf=sb.toString();
    if (convertTo.equals("gpx")) {
      GpxHelper gh=new GpxHelper();
      buf=gh.csv2gpx(buf);
      outcome=new U.Summary("exported", fullCount, gh.mCount, targetFile);
    }
    else outcome=new U.Summary("wrote", fullCount, count, targetFile);
    U.filePutContents(mPath, targetFile, buf, false);
    return outcome;
  }

  public void trashPoint(Point p) throws IOException {
    if (p == null) {
      Log.w(U.TAG,"StorageHelper:"+"trashPoint : null argument");
      return;
    }
    String h=mHeader+Point.NL;
    String buf = p.toCsv()+Point.NL;
    if ( U.fileExists(mPath, mMyTrashFile, "csv") == null) buf=h+buf;
    U.filePutContents(mPath, mMyTrashFile, buf, true);
  }

  public int getPointCount(String targetFile) throws IOException,U.DataException {
    targetFile=U.assureExtension(targetFile,"csv");
    String buf=U.fileGetContents(mPath, targetFile);
    String[] lines=splitCsv(buf);
    int l=lines.length;
    int expected=l-2;
    if (l == 1) expected=0;
    return expected;
  }

  public int checkPointCount(String targetFile, PointList pl) throws IOException,U.DataException {
    int expected=getPointCount(targetFile);
    checkWithListSize(expected,pl);
    return expected;
  }

  private String[] splitCsv(String buf) throws DataException {
    String[] lines=TextUtils.split(buf, Point.NL);
    int l=lines.length;
    if (l == 0) throw new U.DataException("The file has no header line");
    if ( ! lines[0].equals(mHeader)) throw new U.DataException("The file has wrong header line");
    return lines;
  }

  private void checkWithListSize(int expected, PointList pl) throws DataException {
    int maxCount=pl.getMax();
    if (expected > maxCount) { throw new U.DataException("No room!"
        +" Set max point count to at least "+expected); }
  }

  public U.Summary readPoints(PointList pl) throws IOException,U.DataException {
    U.Summary s=readPoints(pl, mMyFile, 0, "");
    pl.clearDirty();
    return s;
  }

  public U.Summary readPoints(PointList pl, String targetFile, int currentPointCount) throws IOException,U.DataException {
    return readPoints(pl, targetFile, currentPointCount, "");
  }

  public U.Summary readPoints(PointList pl, String targetFile, int currentPointCount, String convertFrom)
      throws IOException,U.DataException {
    if (null == U.fileExists(mPath, targetFile)) {
      return new U.Summary("loaded", 0, 0, targetFile);
    }
    String buf=U.fileGetContents(mPath, targetFile);
    if (convertFrom.equals("gpx")) {
      //buf=GpxHelper.removeGpxTail(buf);
      GpxHelper gh=new GpxHelper();
      buf=gh.gpx2csv(buf);
    }

    String[] lines=splitCsv(buf);
    int l=lines.length;
    if (l == 1) return new U.Summary("loaded", 0, 0, mMyFile);
    int expected=currentPointCount+l-2;// 2 for the header and ending NL
    checkWithListSize(expected,pl);

    String line;
    Point p;
    int i=1;
    int count=0;
    int maxCount=pl.getMax();
    for (; i < l; i+=1) {
      line=lines[i].trim();
      if (line.isEmpty()) continue;
      p=(new Point()).fromCsv(line);
      if (U.DEBUG) Log.d(U.TAG,"StorageHelper:"+"About to add point "+p.getId());
      if (p.getId() == pl.getNext()) { pl.addAsNext(p); }
      else { pl.addAndShiftNext(p); }
      count+=1;
      if (count >= maxCount) {
        if (U.DEBUG) Log.d(U.TAG,"StorageHelper:"+"Loaded first "+count+" points of "+(l-2));
        break;
      }
    }
    if (count > 0) pl.setDirty();
    return new U.Summary("loaded", l-2, count, targetFile);
  }

  public static String append2LatLonString(String unit, boolean isNewSeg, String lls) {
    StringBuilder buf;
    if (null == lls || lls.length() < 4) {
      return "[["+unit+"]]";
    }
    if (isNewSeg) unit="],["+unit;
    else unit=","+unit;
    String cutEnding=lls.substring(0, lls.length()-2);// minus "]]"
    buf=new StringBuilder(cutEnding).append(unit).append("]]");
    return buf.toString();
  }

  public String getMyDir() { return mPath; }
}
