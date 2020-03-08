package truewatcher.tower;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Xml;

public class GpxHelper {
  // https://developer.android.com/training/basics/network-ops/xml#java

  private final String ns = null;
    
  public String gpx2csv(String gpxString) throws U.DataException, IOException {
      ByteArrayInputStream is = new ByteArrayInputStream(gpxString.getBytes(StandardCharsets.UTF_8));
    try {
      XmlPullParser parser = Xml.newPullParser();
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      parser.setInput(is, null);
      parser.nextTag();
      return readGpx(parser);
    }
    catch (XmlPullParserException e) {
      throw new U.DataException("XmlPullParserException:"+e.getMessage());
    }
    finally {
      is.close();
    }
  }

  private String readGpx(XmlPullParser parser) throws XmlPullParserException, IOException {
    List<String> entries = new ArrayList<String>();
    String line="";

    parser.require(XmlPullParser.START_TAG, ns, "gpx");
    while ( ! (parser.next() == XmlPullParser.END_TAG && parser.getName().equals("gpx"))) {
      if (parser.getEventType() != XmlPullParser.START_TAG) {
        continue;
      }
      String name = parser.getName();
      if (name.equals("wpt")) {
        line=concatCsv(readWpt(parser));
        entries.add(line);
        if (U.DEBUG) Log.d(U.TAG, "GpxHelper:"+"Got a line:"+line);
      }
      else {
        if (U.DEBUG) Log.d(U.TAG, "GpxHelper:"+"readGpx: skipping "+name);
        skip(parser);
      }
    }
    String h=TextUtils.join(Point.SEP, Point.FIELDS)+Point.NL;
    return h+TextUtils.join("",entries);
  }

  private Map<String,String> readWpt(XmlPullParser parser) throws XmlPullParserException, IOException {
    Map<String,String> csv= new ArrayMap<String,String>();

    parser.require(XmlPullParser.START_TAG, ns, "wpt");
    
    // "id","type","comment","protect","lat","lon","alt","range","time","cellData","note","sym"
    // Read attributes of the wpt tag
    csv.put("lat",parser.getAttributeValue(null, "lat"));
    csv.put("lon",parser.getAttributeValue(null, "lon"));
    // read my custom attributes
    csv.put("type",parser.getAttributeValue(null, "type"));
    csv.put("range",parser.getAttributeValue(null, "range"));
    csv.put("protect",parser.getAttributeValue(null, "protect"));
    
    // Read nested tags
    // ele time name sym cmt desc extensions
    while ( ! (parser.next() == XmlPullParser.END_TAG && parser.getName().equals("wpt"))) {
      if (parser.getEventType() != XmlPullParser.START_TAG) {
        continue;
      }
      String name = parser.getName();
      if (name.equals("type")) {
        csv.put("type",readTagText("type",parser));
      }
      else if (name.equals("ele")) {
        csv.put("alt",readTagText("ele",parser));
      }
      else if (name.equals("time")) {
        csv.put("time",readTagText("time",parser));
      }
      else if (name.equals("name")) {
        csv.put("id",readTagText("name",parser));
      }
      else if (name.equals("cmt")) {
        csv.put("comment",readTagText("cmt",parser));
      }
      else if (name.equals("sym")) {
        csv.put("sym",readTagText("sym",parser));
      }
      else if (name.equals("desc")) {
        csv.put("note",readTagText("desc",parser));
      }
      else if (name.equals("extensions")) {
        Map<String,String> extensions=readExtensions(parser);
        csv.putAll(extensions);
      }
      else {
        if (U.DEBUG) Log.d(U.TAG, "GpxHelper:"+"readWpt: skipping "+name);
        skip(parser);
      }
    }
    return postProcess(csv);
  }

  private Map<String,String> readExtensions(XmlPullParser parser) throws XmlPullParserException, IOException {
    Map<String, String> extensions = new ArrayMap<String, String>();

    parser.require(XmlPullParser.START_TAG, ns, "extensions");
    while (parser.next() != XmlPullParser.END_TAG) {
      if (parser.getEventType() != XmlPullParser.START_TAG) {
        continue;
      }
      String name = parser.getName();
      if (name.equals("type")) {
        extensions.put("type",readTagText("type",parser));
      }
      else if (name.equals("range")) {
        extensions.put("range",readTagText("range",parser));
      }
      else if (name.equals("protect")) {
        extensions.put("protect",readTagText("protect",parser));
      }
      else if (name.equals("cellData")) {
        extensions.put("cellData",readTagText("cellData",parser));
      }
      else {
        if (U.DEBUG) Log.d(U.TAG, "GpxHelper:"+"Unknown extensions:"+name);
        skip(parser);
      }
    }
    return extensions;
  }

  private Map<String,String> postProcess(Map<String,String> csv) {
    String type=csv.get("type");
    //if (U.DEBUG) Log.d(U.TAG,"GpxHelper:","type="+type);
    if (type == null || type.isEmpty()) {
      if (csv.get("cellData") != null) csv.put("type","cell");
      else if (csv.get("alt") != null) csv.put("type","gps");
      else csv.put("type","mark");    
    }
    String time=csv.get("time");
    if (time != null && ! time.isEmpty()) {
      try { time=U.utcToLocalTime(time); }
      catch (U.DataException e) { time=Point.getDate(); }
      csv.put("time",time);
    }
    return csv;
  }

  private String readTagText (String tag, XmlPullParser parser) throws IOException, XmlPullParserException {
    parser.require(XmlPullParser.START_TAG, ns, tag);
    String txt = readText(parser);
    parser.require(XmlPullParser.END_TAG, ns, tag);
    return txt;
  }

  private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
    String result = "";
    if (parser.next() == XmlPullParser.TEXT) {
      result = parser.getText();
      parser.nextTag();
    }
    return result;
  }

  private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
    if (parser.getName() == null) { return; } // caused by <extensions><... /></extensions>
    if (parser.getEventType() != XmlPullParser.START_TAG) {
      throw new IllegalStateException();
    }
    int depth = 1;
    while (depth != 0) {
      switch (parser.next()) {
      case XmlPullParser.END_TAG:
        depth--;
        break;
      case XmlPullParser.START_TAG:
        depth++;
        break;
      }
    }
  }

  private String concatCsv(Map<String,String> csv) {
    List<String> header=Point.FIELDS;
    List<String> data=new ArrayList<String>();
    for (String field : header) {
      if (csv.get(field) != null) data.add(csv.get(field));
      else data.add("");
    }
    return TextUtils.join(Point.SEP,data)+Point.NL;
  }

  private StringBuilder mSb=new StringBuilder("[");
  private int mSegCount=0;
  private int mPointCount=0;
  private boolean isSeg0=true;
  private boolean isPoint0=true;

  public String track2latLonJson(String gpxString) throws U.DataException, IOException {
    ByteArrayInputStream is = new ByteArrayInputStream(gpxString.getBytes(StandardCharsets.UTF_8));
    try {
      XmlPullParser parser = Xml.newPullParser();
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      parser.setInput(is, null);
      parser.nextTag();
      return readGpx2(parser);
    }
    catch (XmlPullParserException e) {
      throw new U.DataException("XmlPullParserException:"+e.getMessage());
    }
    finally {
      is.close();
    }
  }

  private String readGpx2(XmlPullParser parser) throws XmlPullParserException, IOException {

    parser.require(XmlPullParser.START_TAG, ns, "gpx");
    mSb=new StringBuilder("[");
    mSegCount=0;
    mPointCount=0;
    isSeg0=true;
    isPoint0=true;

    while ( ! (parser.next() == XmlPullParser.END_TAG && parser.getName().equals("gpx"))) {
      if (parser.getEventType() != XmlPullParser.START_TAG) { continue; }
      String name = parser.getName();
      if (name.equals("trk")) { continue; }
      if (name.equals("trkseg")) {
        mSegCount+=1;
        isPoint0=true;
        if (isSeg0) isSeg0=false;
        else mSb.append(",");
        mSb.append("[");
        readTrkseg(parser);
        mSb.append("]");
        if (U.DEBUG) Log.d(U.TAG, "GpxHelper:"+"Done a track segment," +
                " segments="+mSegCount+", points="+mPointCount);
      }
      else {
        if (U.DEBUG) Log.d(U.TAG, "GpxHelper:"+"readGpx2: skipping "+name);
        skip(parser);
      }
    }
    mSb.append("]");
    return mSb.toString();
  }

  private void readTrkseg(XmlPullParser parser) throws XmlPullParserException, IOException {

    parser.require(XmlPullParser.START_TAG, ns, "trkseg");
    while ( ! (parser.next() == XmlPullParser.END_TAG && parser.getName().equals("trkseg"))) {
      if (parser.getEventType() != XmlPullParser.START_TAG) {
        continue;
      }
      String name = parser.getName();
      if (name.equals("trkpt")) {
        readTrkpt(parser);
        //if (U.DEBUG) Log.d(U.TAG, "GpxHelper:"+"Done a trackpoint,count="+mPointCount);
      }
      else {
        if (U.DEBUG) Log.d(U.TAG, "GpxHelper:"+"readTrkseg: skipping "+name);
        skip(parser);
      }
    }
  }

  private void readTrkpt(XmlPullParser parser) throws XmlPullParserException, IOException {
    parser.require(XmlPullParser.START_TAG, ns, "trkpt");

    String lat=parser.getAttributeValue(null, "lat");
    String lon=parser.getAttributeValue(null, "lon");
    if (lat == null || lat.isEmpty() || lon == null || lon.isEmpty()) {
      if (U.DEBUG) Log.d(U.TAG, "GpxHelper:"+"a trackpoint without LAT or LON, skipped");
      return;
    }
    mPointCount+=1;
    if (isPoint0) isPoint0=false;
    else mSb.append(",");
    mSb     .append("[")
            .append(lat)
            .append(",")
            .append(lon)
            .append("]");
    while ( ! (parser.next() == XmlPullParser.END_TAG && parser.getName().equals("trkpt"))) {
      //if (U.DEBUG) Log.d(U.TAG, "GpxHelper:"+"readTrkpt: skipping "+parser.getName());
      /*if (parser.getEventType() == XmlPullParser.START_TAG) {
        Log.d(U.TAG, "GpxHelper:"+"readTrkpt: starttag "+parser.getName());
      }
      if (parser.getEventType() == XmlPullParser.END_TAG) {
        Log.d(U.TAG, "GpxHelper:"+"readTrkpt: endtag "+parser.getName());
      }*/
      skip(parser);
    }
  }

  public U.Summary getResults() {
    String outcome="";
    if (mSegCount == 0) outcome="No tracks found";
    else if (mPointCount == 0) outcome="No trackpoints found";
    else outcome="loaded";
    return new U.Summary(outcome, mPointCount,mPointCount,"",mSegCount);
  }
  
  private String mWptTemplate, mExtensionsTemplate;
  private Map<String,String> mSyms=new ArrayMap<String,String>();
  public String mProgress="";
  public int mCount=0;
  private String mHeader=TextUtils.join(Point.SEP, Point.FIELDS);
  private String mGpxFramingHead, mGpxFramingTail;
  
  private void init() {
    mWptTemplate="<wpt lat=\"%s\" lon=\"%s\" >%s<time>%s</time><name>%s</name>%s<sym>%s</sym>%s%s</wpt>";
    mExtensionsTemplate="<extensions>%s%s%s%s</extensions>";
    mSyms.put("cell","Navaid, Blue");
    mSyms.put("gps","Flag, Red");
    mSyms.put("mark","Navaid, Violet");// Navaid, Magenta is rendered as Flag, Blue by Garmin
    mSyms.put("default","Navaid, Red");
    mGpxFramingHead="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>" +
            "<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"truewatcher.tower\" >";
    mGpxFramingTail="</gpx>";
  }

  public String csv2gpx(String buf) throws U.DataException {
    String outBuf="";
    Map<String,String> csv;
    init();
    mCount=0;
    String[] lines=TextUtils.split(buf, Point.NL);
    String wpt;
    String[] values;
    int l=lines.length;
    if (l == 0) throw new U.DataException("The file has no header line");
    if ( ! lines[0].equals(mHeader)) {
      Log.e(U.TAG, "StorageHelper_csv2gpx:"+"Wrong header:"+lines[0]);
      throw new U.DataException("The file has wrong header line");
    }
    for (int i=1; i < l; i+=1) {
      if (U.DEBUG) Log.d(U.TAG,"GpxHelper:"+"Got a line:"+lines[i]);
      if (lines[i].length() < 2) continue;
      values=TextUtils.split(lines[i], Point.SEP);
      csv=U.arrayCombine(Point.FIELDS, values);
      wpt=makeWpt(csv);
      outBuf+=wpt+Point.NL;
      mCount+=1;
    }
    outBuf=mGpxFramingHead+Point.NL+outBuf+mGpxFramingTail;
    return outBuf;
  }

  private String makeWpt(Map<String,String> content) throws U.DataException {
    if ( content.get("id").isEmpty() ) { throw new U.DataException("No id"); }
    String id=content.get("id");
    if ( content.get("lat").isEmpty() || content.get("lon").isEmpty()) {
      mProgress+="\n"+id+": no coords, skipped";
      return "";
    }
    String time="", ele="", cmt="", sym="", desc="";
    if ( ! content.get("time").isEmpty() ) {
      try { time=U.localTimeToUTC(content.get("time")); }
      catch (U.DataException e)  {
        try { time=U.localTimeToUTC(Point.getDate()); }
        catch (U.DataException ee)  { time=""; }
      }
    }
    if ( ! content.get("alt").isEmpty() ) {
      ele="<ele>"+content.get("alt")+"</ele>";
    }
    if ( ! content.get("comment").isEmpty() ) {
      cmt="<cmt>"+content.get("comment")+"</cmt>";
    }
    if ( ! content.get("sym").isEmpty() ) sym=content.get("sym");
    else sym=getSym(content.get("type"));
    if (! content.get("note").isEmpty() ) {
      desc="<desc>"+content.get("note")+"</desc>";
    }
    String extensions=buildExtensions(content);

    //<wpt lat="%s" lon="%s" >%s<time>%s</time><name>%s</name>%s<sym>%s</sym>%s%s</wpt>
    String entry=String.format(mWptTemplate, content.get("lat"), content.get("lon"),
        ele, time, id, cmt, sym, desc, extensions);
    return entry;
  }

  private String buildExtensions(Map<String,String> content) {
    String typeEl="", rangeEl="", protectEl="", cellDataEl="";
    if ( ! content.get("type").isEmpty() ) {
      typeEl="<type>"+content.get("type")+"</type>";
    }
    if ( ! content.get("range").isEmpty() ) {
      rangeEl="<range>"+U.str2int(content.get("range"))+"</range>";
    }
    if ( ! content.get("protect").isEmpty() ) {
      protectEl="<protect>"+content.get("protect")+"</protect>";
    }
    if ( ! content.get("cellData").isEmpty() ) {
      protectEl="<cellData>"+content.get("cellData")+"</cellData>";
    }
    if (typeEl.isEmpty() && rangeEl.isEmpty() && protectEl.isEmpty() && cellDataEl.isEmpty()) return "";
    return String.format(mExtensionsTemplate, typeEl, rangeEl, protectEl, cellDataEl);
  }
  
  private String getSym(String type) {
    if ( type.equals("cell") || type.equals("gps") || type.equals("mark") ) return mSyms.get(type);
    return mSyms.get("default");
  }

}

