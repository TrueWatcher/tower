package truewatcher.signaltrackwriter;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackStorage {
  private String mTargetPath;
  private String mMyFileExt = "currentSignalTrack.csv";
  private boolean mShouldStartNewSegment = false;
  private long mTotalPointCount =0;
  public Trackpoint latestStoredTrackpoint = null;
  private MyRegistry mRg = MyRegistry.getInstance();
  private int mLastNl=0;
  private int mLastId=0;
  private String[] mCachedLines =  null;

  public void demandNewSegment() {
    mShouldStartNewSegment = true;
  }

  public void desistNewSegment() {
    mShouldStartNewSegment = false;
  }

  public void initTargetDir(Context context) throws IOException, U.FileException {
    String nativeFolder = context.getExternalFilesDir(null).getPath();
    mTargetPath = nativeFolder;
    if (mRg.getBool("useMediaFolder")) {
      String appMediaFolder = getMediaDir(context);
      Log.i(U.TAG, "appMediaFolder=" + appMediaFolder);
      if (appMediaFolder.isEmpty()) throw new U.FileException("Cannot find the app's media folder");
      mTargetPath = appMediaFolder;
    }
    else if (mRg.getBool("useTowerFolder")) {
      String towerFolder = getTowerDir(nativeFolder);
      if (towerFolder.isEmpty()) throw new U.FileException("Cannot find truewatcher.tower");
      mTargetPath = towerFolder;
    }
    if (U.DEBUG) Log.i(U.TAG, "My path=" + mTargetPath);
    initMyFile();
  }

  private void initMyFile() throws IOException {
    if (null == U.fileExists(mTargetPath, mMyFileExt, "csv")) {
      String headerNl = TextUtils.join(Trackpoint.SEP, Trackpoint.FIELDS).concat(Trackpoint.NL);
      U.filePutContents(mTargetPath, mMyFileExt, headerNl, false);
      demandNewSegment();
    }
  }

  private String getMediaDir(Context context) {
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

  private int detectLastId() throws U.FileException, IOException, U.DataException {
    U.Summary2 res=visitStored(new Numerator());
    return Integer.valueOf(res.segMap);
  }

  public int getLastId() { return mLastId; }

  public String getTowerDir(String nativeDir) {
    String changedDir = nativeDir.replace("truewatcher.signaltrackwriter","truewatcher.tower");
    if (false == new File(changedDir).exists()) return "";
    return changedDir;
  }


  public String getWorkingFileFull() {
    if (null == mTargetPath) return mMyFileExt;
    if ( ! mTargetPath.endsWith("/")) mTargetPath = mTargetPath.concat("/");
    return mTargetPath.concat(mMyFileExt);
  }

  public void simplySave(Trackpoint p) {
    if ( ! mRg.getBool("trackShouldWrite")) return;
    if ( ( mShouldStartNewSegment || mTotalPointCount == 0 ) && p.getType().equals("T")) {
      p.setNewSegment();
      mShouldStartNewSegment = false;
    }
    mLastId += 1;
    p.setIdInt(mLastId);
    String line = p.toCsv().concat(Trackpoint.NL);
    try {
      U.filePutContents(mTargetPath, mMyFileExt, line, true);
    }
    catch (IOException e) {
      throw new U.RunException("IOException" + e.getMessage());
    }
    if (p.getType().equals("T")) mTotalPointCount += 1;
  }

  public void updateLast(Trackpoint p) {
    if ( ! mRg.getBool("trackShouldWrite")) return;
    String buf= "";
    try {
      buf=U.fileGetContents(mTargetPath, mMyFileExt);
      buf = buf.trim();
      p.setNewSegment(detectNewSegment(buf));
      p.setIdInt(mLastId);
      String line = p.toCsv().concat(Trackpoint.NL);
      buf=buf.substring(0,mLastNl+1);
      buf=buf.concat(line);
      U.filePutContents(mTargetPath, mMyFileExt, buf, false);
    }
    catch (IOException e) {
      throw new U.RunException("IOException:" + e.getMessage());
    }
    catch (U.DataException e) {
      throw new U.RunException("DataException:" + e.getMessage() + "Trace:" + e.getStackTrace());
    }
  }

  private String detectNewSegment(String buf) throws U.DataException {
    buf = buf.trim();
    mLastNl=buf.lastIndexOf(Trackpoint.NL);
    String lastLine=buf.substring(mLastNl+1);
    if (U.DEBUG) Log.i(U.TAG, "lastLine:"+lastLine+"===");
    String ns=(new Trackpoint()).fromCsv(lastLine).getNewSegment();
    return ns;
  }

  //adb pull /sdcard/Android/data/truewatcher.trackwriter/files/currentTrack.csv

  public void saveNote(String comment, String data) {
    Trackpoint p = new Trackpoint("note", comment, data);
    simplySave(p);
  }

  public U.Summary2 statStored_() throws U.FileException, IOException, U.DataException {
    U.Summary2 res=visitStored(new Counter());
    U.Summary2 mil=visitStored(new Mileage());
    res.segMap=res.segMap+Point.NL+mil.segMap;
    U.Summary2 res2=visitStored(new Numerator());
    mLastId=Integer.valueOf(res2.segMap);
    if (U.DEBUG) Log.i(U.TAG, "found last ID:"+mLastId);
    return res;
  }

  public U.Summary2 statStored() throws U.FileException, IOException, U.DataException {
    U.Summary2 res=visitStored(new Counter());
    if (res.adopted == 0) { // the track is empty
      return res;
    }
    U.Summary2 mil=visitStored(new Mileage());
    // repack segMaps
    String[] counts = TextUtils.split(res.segMap," ");
    String[] mils = TextUtils.split(mil.segMap," ");
    int i=0;
    String segMap="";
    for (; i < counts.length-1; i+=1) {
      segMap += String.format("#%d  %s  %s\n",i+1,counts[i],mils[i]);
    }
    res.segMap=segMap;
    U.Summary2 res2=visitStored(new Numerator());
    mLastId=Integer.valueOf(res2.segMap);
    if (U.DEBUG) Log.i(U.TAG, "found last ID:"+mLastId);
    mCachedLines = null;
    return res;
  }

  private abstract class Visitor {
    void onInit() {}
    void onNewsegment(int lineNumber, int segNumber, Trackpoint p, String[] lines) {}
    void onNewtrackpoint(int lineNumber, int segNumber, Trackpoint p, String[] lines) {}
    void onEnd(int lineNumber, int segNumber, Trackpoint p, String[] lines) throws U.DataException {}
    String presentResult() { return ""; }
  }

  private class Counter extends Visitor {
    private List<Integer> counts;
    private int found = 0;

    @Override
    void onInit() {
      counts = new ArrayList<Integer>();
      counts.add(0);
    }

    @Override
    void onNewsegment(int lineNumber, int segNumber, Trackpoint p, String[] lines) {
      counts.add(0);
    }

    @Override
    void onNewtrackpoint(int lineNumber, int segNumber, Trackpoint p, String[] lines) {
      found += 1;
      counts.set(segNumber, counts.get(segNumber) + 1);
    }

    @Override
    void onEnd(int lineNumber, int segNumber, Trackpoint p, String[] lines) throws U.DataException {
      if (counts.get(0) > 0) throw new U.DataException("Some data precede start of the 1st segment");
      if (p != null) latestStoredTrackpoint = (Trackpoint) p.clone();
      else latestStoredTrackpoint = null;
      mTotalPointCount = found;
    }

    @Override
    String presentResult() {
      String map="";
      if (found == 0) { map = "<empty track>"; }
      else {
        for (int ii = 1; ii < counts.size(); ii += 1) {
          map += Integer.toString(counts.get(ii)) + " ";
        }
      }
      return map;
    }
  }

  private class Mileage extends Visitor {
    private List<Float> meters;
    private int found = 0;

    @Override
    void onInit() {
      meters = new ArrayList<Float>();
      meters.add((float) 0);
    }

    @Override
    void onNewsegment(int lineNumber, int segNumber, Trackpoint p, String[] lines) {
      meters.add((float) 0);
    }

    @Override
    void onNewtrackpoint(int lineNumber, int segNumber, Trackpoint p, String[] lines) {
      found += 1;
      Trackpoint next = nextPoint(lineNumber,lines);
      if (null != next) {
        float leg=(float) U.proximityM(p,next);
        meters.set(segNumber, meters.get(segNumber) + leg);
      }
    }

    private Trackpoint nextPoint(int lineNumber, String[] lines) {
      Trackpoint p;
      for (int ii=lineNumber+1; ii < lines.length; ii += 1) {
        try {
          p = (new Trackpoint()).fromCsv(lines[ii]);
        }
        catch (U.DataException e) {
          throw new U.RunException("U.DataException:"+e.getMessage());
        }
        if (p == null) continue;
        if (p.isNewSegment()) return null;
        if (p.getType().equals("T")) return p;
      }
      return null;
    }

    @Override
    void onEnd(int lineNumber, int segNumber, Trackpoint p, String[] lines) throws U.DataException {
    }

    @Override
    String presentResult() {
      String map="";
      if (found == 0) { map = "<empty track>"; }
      else {
        for (int ii = 1; ii < meters.size(); ii += 1) {
          map += U.str2int(Float.toString(meters.get(ii))).concat("m ");
        }
      }
      return map;
    }
  }

  public class Numerator extends Visitor {
    private int mCurrentId = 0;
    //private int mLastId = 0;
    private String mError = "";

    @Override
    void onNewtrackpoint(int lineNumber, int segNumber, Trackpoint p, String[] lines) {
      String foundId = p.id;
      int foundIdInt = -1;
      if (foundId.isEmpty()) {
        mError += " Empty id at " + String.valueOf(lineNumber) + " ";
        return;
      }
      try {
        foundIdInt = Integer.parseInt(foundId);
      }
      catch (NumberFormatException e) {}
      if (foundIdInt < 0) {
        mError += " Wrong id:" + foundId + " " + String.valueOf(lineNumber) + " ";
        return;
      }
      if (foundIdInt <= mCurrentId) mError += " Non-order id:" + foundId + " (after " + mCurrentId + ") "
              + " at line " + String.valueOf(lineNumber+1) + " ";
      mCurrentId = foundIdInt;
    }

    @Override
    String presentResult() {
      if ( ! mError.isEmpty()) Log.e(U.TAG, "Numerator errors:"+mError+" ");
      return String.valueOf(mCurrentId);
    }
  }

  public U.Summary2 visitStored(Visitor visitor) throws U.FileException, IOException, U.DataException {
    if (mCachedLines == null || mCachedLines.length == 0) {
      if (null == U.fileExists(mTargetPath, mMyFileExt, "csv")) {
        throw new U.FileException("Missing my good file");
      }
      String buf = U.fileGetContents(mTargetPath, mMyFileExt);
      mCachedLines = splitCsv(buf);
    }
    int l = mCachedLines.length;
    if (l < 2) throw new U.DataException("Wrong content");
    int i = 1;
    int j = 0;
    int found = 0;
    Trackpoint p = null;
    visitor.onInit();

    for (i = 1; i < l; i += 1) {
      p = (new Trackpoint()).fromCsv(mCachedLines[i]);
      if (null == p || !p.getType().equals("T")) continue;
      if (p.isNewSegment()) {
        j += 1;
        visitor.onNewsegment(i,j,p,mCachedLines);
      }
      found += 1;
      visitor.onNewtrackpoint(i,j,p,mCachedLines);
    }
    visitor.onEnd(i,j,p,mCachedLines);
    String map = visitor.presentResult();
    return new U.Summary2("storage", l, found, mMyFileExt, j, map, 0);
  }

  private String[] splitCsv(String buf) throws U.DataException {
    String[] lines = TextUtils.split(buf, Point.NL);
    int l = lines.length;
    if (l == 0) throw new U.DataException("The file has no header line");
    String header = TextUtils.join(Trackpoint.SEP, Trackpoint.FIELDS);
    if (!lines[0].equals(header)) throw new U.DataException("The file has wrong header line");
    if (l < 2) throw new U.DataException("Wrong content");
    return lines;
  }

  public void deleteLastSegment() throws U.FileException, U.DataException, IOException {
    int lastSegmentMark = -1, i = 0;
    String[] fields;
    if (null == U.fileExists(mTargetPath, mMyFileExt, "csv")) {
      throw new U.FileException("Missing my good file");
    }
    String buf = U.fileGetContents(mTargetPath, mMyFileExt);
    String[] lines = splitCsv(buf);
    int l = lines.length;
    int typeColumn = Trackpoint.FIELDS.indexOf("type");
    int newsegColumn = Trackpoint.FIELDS.indexOf("new_track");
    if (typeColumn < 0 || newsegColumn < 0)
      throw new U.RunException("Something is wrong with csv headers");

    for (i = l - 1; i > 0; i -= 1) {
      if (lines[i].isEmpty()) continue;
      fields = TextUtils.split(lines[i], Point.SEP);
      lines[i] = "";
      if (fields[typeColumn].equals("T") && ! fields[newsegColumn].isEmpty()) {
        lastSegmentMark = i;
        break;
      }
    }
    if (lastSegmentMark <= 0) return;// no segments found - nothing to write
    buf = TextUtils.join(Point.NL, lines);
    buf = buf.trim().concat(Point.NL);
    U.filePutContents(mTargetPath, mMyFileExt, buf, false);
    mShouldStartNewSegment = true;
  }

  public int renumber() throws U.FileException, U.DataException, IOException {
    int i = 0, count = 0;
    String[] fields;
    if (null == U.fileExists(mTargetPath, mMyFileExt, "csv")) {
      throw new U.FileException("Missing my good file");
    }
    String buf = U.fileGetContents(mTargetPath, mMyFileExt);
    String[] lines = splitCsv(buf);
    int l = lines.length;
    int idColumn = Trackpoint.FIELDS.indexOf("id");
    int typeColumn = Trackpoint.FIELDS.indexOf("type");
    if (idColumn < 0 || typeColumn < 0)
      throw new U.RunException("Something is wrong with csv headers");

    for (i = 1; i < l; i += 1) {
      if (lines[i].isEmpty()) continue;
      fields = TextUtils.split(lines[i], Point.SEP);
      if (fields[typeColumn].equals("T")) {
        count += 1;
        fields[idColumn] = String.valueOf(count);
      }
      else { fields[idColumn] = ""; }
      lines[i] = TextUtils.join(Point.SEP, fields);;
    }
    buf = TextUtils.join(Point.NL, lines);
    buf = buf.trim().concat(Point.NL);
    U.filePutContents(mTargetPath, mMyFileExt, buf, false);
    return count;
  }

  public U.Summary trackCsv2Gpx(String targetFileExt) throws U.FileException, U.DataException, IOException {
    return (new TrackToGpx()).trackCsv2Gpx(targetFileExt);
  }

  private class TrackToGpx {
    public int mCount = 0, mRecords = 0, mSegments = 1;
    private String mTrkptTemplate, mGpxFramingHead, mGpxFramingTail, mTrkHeader, mTrkTail, mTrkChangeSegment;

    private void init() {
      mTrkptTemplate = "<trkpt lat=\"%s\" lon=\"%s\" >%s<time>%s</time>%s</trkpt>";
      mGpxFramingHead = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>" +
              "<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"truewatcher.trackwriter\" >";
      mTrkHeader = "<trk><name>%s</name><trkseg>";
      mTrkTail = "</trkseg></trk>";
      mTrkChangeSegment = "</trkseg>\n<trkseg>";
      mGpxFramingTail = "</gpx>";
    }

    public U.Summary trackCsv2Gpx(String targetFileExt) throws U.FileException, U.DataException, IOException {
      if (null == U.fileExists(mTargetPath, mMyFileExt, "csv")) {
        throw new U.FileException("Missing my good file");
      }
      String buf = U.fileGetContents(mTargetPath, mMyFileExt);
      buf = csv2gpx(targetFileExt, buf);
      if (mCount == 0) { return new U.Summary("failed to export", mRecords, mCount, targetFileExt); }
      targetFileExt = U.assureExtension(targetFileExt, "gpx");
      U.filePutContents(mTargetPath, targetFileExt, buf, false);
      //String aAct, int aFound, int aAdopted, String file,int aSegments
      return new U.Summary("exported", mRecords, mCount, targetFileExt, mSegments);
    }

    public String csv2gpx(String targetFileExt, String buf) throws U.DataException {
      StringBuilder outBuf = new StringBuilder();
      Map<String, String> csv;
      init();
      mCount = 0;
      String[] lines = splitCsv(buf);
      String trkpt;
      String[] values;
      int l = lines.length;
      mRecords = l - 2;// minus header and final NL
      outBuf.append(mGpxFramingHead);
      outBuf.append(Point.NL);
      outBuf.append(String.format(mTrkHeader, targetFileExt));
      outBuf.append(Point.NL);

      for (int i = 1; i < l; i += 1) {
        if (U.DEBUG) Log.d(U.TAG, "GpxHelper:" + "Got a line:" + lines[i]);
        if (lines[i].length() < 2) continue;
        values = TextUtils.split(lines[i], Point.SEP);
        csv = U.arrayCombine(Point.FIELDS, values);
        if ( ! csv.get("type").equals("T")) continue;
        trkpt = makeTrkpt(csv);
        if (isExtraSegment(csv)) {
          outBuf.append(mTrkChangeSegment);
          mSegments += 1;
        }
        outBuf.append(trkpt);
        outBuf.append(Point.NL);
        mCount += 1;
      }
      outBuf.append(mTrkTail);
      outBuf.append(mGpxFramingTail);
      return outBuf.toString();
    }

    private String makeTrkpt(Map<String, String> content) throws U.DataException {
      if (!content.get("type").equals("T")) {
        return "";
      }
      if (content.get("lat").isEmpty() || content.get("lon").isEmpty()) {
        throw new U.DataException("Trackpoint without cootds at line " + mCount);
      }
      String time = "", ele = "", cmt = "";
      if (!content.get("time").isEmpty()) {
        try {
          time = U.localTimeToUTC(content.get("time"));
        } catch (U.DataException e) {
          try {
            time = U.localTimeToUTC(Point.getDate());
          } catch (U.DataException ee) {
            time = "";
          }
        }
      }
      if (!content.get("alt").isEmpty()) {
        ele = "<ele>" + content.get("alt") + "</ele>";
      }
      //if (!content.get("name").isEmpty()) {
      //  cmt = "<cmt>" + content.get("name") + "</cmt>";
      //}

      String entry = String.format(mTrkptTemplate, content.get("lat"), content.get("lon"),
              ele, time, cmt);
      return entry;
    }

    private boolean isExtraSegment(Map<String, String> content) {
      if (mCount == 0) return false; // first <trkseg> is already in mTrkHeader
      if ( ! content.get("new_track").isEmpty()) return true;
      return false;
    }

  }// end private class TrackToGpx

}
