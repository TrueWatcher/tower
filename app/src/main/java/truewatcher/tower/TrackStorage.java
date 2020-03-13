package truewatcher.tower;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackStorage {
  private String mTargetPath;
  private String mCurrentTrackFile = "currentTrack.csv";
  private boolean mShouldStartNewSegment = false;
  private long mTotalPointCount =0;
  public Trackpoint latestStoredTrackpoint = null;
  private MyRegistry mRg = MyRegistry.getInstance();

  public String getMyFile() { return mCurrentTrackFile; }

  public void demandNewSegment() {
    mShouldStartNewSegment = true;
  }

  public void desistNewSegment() {
    mShouldStartNewSegment = false;
  }

  public void initTargetDir(Context context) throws IOException, U.FileException {
    String nativeFolder = context.getExternalFilesDir(null).getPath();
    if (mRg.getBool("useTowerFolder")) {
      String towerFolder = getTowerDir(nativeFolder);
      if (towerFolder.isEmpty()) throw new U.FileException("Cannot find truewatcher.tower");
      mTargetPath = towerFolder;
    }
    else { mTargetPath = nativeFolder; }
    if (U.DEBUG) Log.i(U.TAG, "My path=" + mTargetPath);
    initMyFile();
  }

  private void initMyFile() throws IOException {
    if (null == U.fileExists(mTargetPath, mCurrentTrackFile, "csv")) {
      String headerNl = TextUtils.join(Trackpoint.SEP, Trackpoint.FIELDS).concat(Trackpoint.NL);
      U.filePutContents(mTargetPath, mCurrentTrackFile, headerNl, false);
      demandNewSegment();
    }
  }

  public String getTowerDir(String nativeDir) {
    String changedDir = nativeDir.replace("truewatcher.trackwriter","truewatcher.tower");
    if (false == new File(changedDir).exists()) return "";
    return changedDir;
  }


  public String getWorkingFileFull() {
    if (null == mTargetPath) return mCurrentTrackFile;
    return mTargetPath + mCurrentTrackFile;
  }

  public Trackpoint simplySave(Trackpoint p) {
    //if ( ! mRg.getBool("enableTrack")) return null;
    if ( ( mShouldStartNewSegment || mTotalPointCount == 0 ) && p.getType().equals("T")) {
      p.setNewSegment();
      mShouldStartNewSegment = false;
    }
    String line = p.toCsv().concat(Trackpoint.NL);
    try {
      U.filePutContents(mTargetPath, mCurrentTrackFile, line, true);
    }
    catch (IOException e) {
      throw new U.RunException("IOException" + e.getMessage());
    }
    if (p.getType().equals("T")) mTotalPointCount += 1;
    return p;
  }

  //adb pull /sdcard/Android/data/truewatcher.trackwriter/files/currentTrack.csv

  public void saveNote(String comment, String data) {
    Trackpoint p = new Trackpoint("note", comment, data);
    simplySave(p);
  }

  public U.Summary2 statStored() throws U.FileException, IOException, U.DataException {
    U.Summary2 res=visitStored(new Counter());
    U.Summary2 mil=visitStored(new Mileage());
    res.segMap=res.segMap+Point.NL+mil.segMap;
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

  public U.Summary2 visitStored(Visitor visitor) throws U.FileException, IOException, U.DataException {
    if (null == U.fileExists(mTargetPath, mCurrentTrackFile, "csv")) {
      throw new U.FileException("Missing my good file");
    }
    String buf = U.fileGetContents(mTargetPath, mCurrentTrackFile);
    String[] lines = splitCsv(buf);
    int l = lines.length;
    if (l < 2) throw new U.DataException("Wrong content");
    int i = 1;
    int j = 0;
    int found = 0;
    Trackpoint p = null;
    visitor.onInit();

    for (i = 1; i < l; i += 1) {
      p = (new Trackpoint()).fromCsv(lines[i]);
      if (null == p || !p.getType().equals("T")) continue;
      if (p.isNewSegment()) {
        j += 1;
        visitor.onNewsegment(i,j,p,lines);
      }
      found += 1;
      visitor.onNewtrackpoint(i,j,p,lines);
    }
    visitor.onEnd(i,j,p,lines);
    String map = visitor.presentResult();
    return new U.Summary2("storage", l, found, mCurrentTrackFile, j, map, 0);
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
    if (null == U.fileExists(mTargetPath, mCurrentTrackFile, "csv")) {
      throw new U.FileException("Missing my good file");
    }
    String buf = U.fileGetContents(mTargetPath, mCurrentTrackFile);

    buf = deleteLastSegmentString(buf);

    if (buf == null) return;// no segments found - nothing to write
    U.filePutContents(mTargetPath, mCurrentTrackFile, buf, false);
    mShouldStartNewSegment = true;
  }

  public String deleteLastSegmentString(String buf) throws U.DataException {
    int lastSegmentMark = -1, i = 0;
    String[] fields;
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
    if (lastSegmentMark <= 0) return null;
    buf = TextUtils.join(Point.NL, lines);
    buf = buf.trim().concat(Point.NL);
    return buf;
  }

  public void deleteAll() {
    if (null == U.fileExists(mTargetPath, mCurrentTrackFile, "csv")) {
      return;
    }
    U.unlink(mTargetPath, mCurrentTrackFile);
  }

  public String trackCsv2LatLonString() throws U.DataException, IOException, U.FileException {
    return (new Track2LatLonJSON()).file2LatLonJSON(mCurrentTrackFile);
  }

  public class Track2LatLonJSON {
    private int mPointCount=0;
    private int mSegCount=1;
    private int mRecordCount=0;
    private String mTargetFile=mCurrentTrackFile;

    public String file2LatLonJSON(String aTargetFileExt)
            throws U.FileException, IOException, U.DataException {
      if (aTargetFileExt.length() > 1) mTargetFile = aTargetFileExt;
      mTargetFile = U.assureExtension(mTargetFile, "csv");
      if (null == U.fileExists(mTargetPath, mTargetFile, "csv")) {
        throw new U.FileException("Missing file " + mTargetFile);
      }
      String buf = U.fileGetContents(mTargetPath, mTargetFile);
      return csv2LatLonJSON(buf);
    }

    public String csv2LatLonJSON(String buf) throws U.DataException {
      StringBuilder outBuf = new StringBuilder();
      Map<String, String> csv;
      int countInSegment = 0;

      String[] lines = splitCsv(buf);
      String[] values;
      int l = lines.length;
      mRecordCount = l - 2;
      outBuf.append("[[");
      for (int i = 1; i < l; i += 1) {
        if (U.DEBUG) Log.d(U.TAG, "trackCsv2LatLonString:" + "Got a line:" + lines[i]);
        if (lines[i].length() < 2) continue;
        values = TextUtils.split(lines[i], Point.SEP);
        csv = U.arrayCombine(Trackpoint.FIELDS, values);
        if (!csv.get("type").equals("T")) continue;

        if (isExtraSegment(csv, mPointCount)) {
          outBuf.append("],[");
          mSegCount += 1;
          countInSegment = 0;
        }
        if (countInSegment > 0) outBuf.append(",");
        outBuf.append("[")
                .append(csv.get("lat"))
                .append(",")
                .append(csv.get("lon"))
                .append("]");

        mPointCount += 1;
        countInSegment += 1;
      }
      outBuf.append("]]");
      //Log.d(U.TAG, outBuf.toString());
      if (mPointCount == 0) return "[]";
      return outBuf.toString();
    }

    public U.Summary getResults() {
      if (mPointCount > 0) {
        return new U.Summary("loaded", mRecordCount, mPointCount, mTargetFile, mSegCount);
      }
      return new U.Summary("failed to load", mRecordCount, mPointCount, mTargetFile);
    }
  }

  private boolean isExtraSegment(Map<String, String> content, int count) {
    if (count == 0) return false; // first <trkseg> is already in template
    if ( ! content.get("new_track").isEmpty()) return true;
    return false;
  }

  public U.Summary trackCsv2Gpx(String targetFileExt) throws U.FileException, U.DataException, IOException {
    return (new TrackToGpx()).trackCsv2Gpx(targetFileExt);
  }

  public class TrackToGpx {
    public int mCount = 0, mRecords = 0, mSegments = 1;
    private String mTargetFileExt="test_file.gpx";
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
      if (null == U.fileExists(mTargetPath, mCurrentTrackFile, "csv")) {
        throw new U.FileException("Missing my good file");
      }
      String buf = U.fileGetContents(mTargetPath, mCurrentTrackFile);
      buf = csv2gpx(targetFileExt, buf);
      if (mCount == 0) { return new U.Summary("failed to export", mRecords, mCount, targetFileExt); }
      mTargetFileExt = U.assureExtension(targetFileExt, "gpx");
      U.filePutContents(mTargetPath, mTargetFileExt, buf, false);
      //String aAct, int aFound, int aAdopted, String file,int aSegments
      return getResults();
    }

    public U.Summary getResults() {
      return new U.Summary("exported", mRecords, mCount, mTargetFileExt, mSegments);
    }

    public String csv2gpx(String targetFileExt, String buf) throws U.DataException {
      mTargetFileExt = targetFileExt;
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
        csv = U.arrayCombine(Trackpoint.FIELDS, values);
        if ( ! csv.get("type").equals("T")) continue;
        trkpt = makeTrkpt(csv);
        if (isExtraSegment(csv, mCount)) {
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
      if (!content.get("name").isEmpty()) {
        cmt = "<cmt>" + content.get("name") + "</cmt>";
      }

      String entry = String.format(mTrkptTemplate, content.get("lat"), content.get("lon"),
              ele, time, cmt);
      return entry;
    }

  }// end private class TrackToGpx

}
