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
    
  public static String removeGpxTail(String buf) {
    String closingTag="</gpx>";
    int tailPos=buf.indexOf(closingTag);
    if (tailPos < 0) {
      Log.w(U.TAG,"GpxHelper:"+"No closing gpx tag!");
      return buf+closingTag;
    }
    int theEnd=tailPos+closingTag.length();
    if (buf.length() < theEnd) {
      Log.e(U.TAG,"GpxHelper:"+"Possibly wrong closing gpx tag!");
      return buf;
    }
    if (buf.length() == theEnd) {
      if (U.DEBUG) Log.d(U.TAG,"GpxHelper:"+"Just right closing gpx tag");
      return buf;
    }
    Log.w(U.TAG,"GpxHelper:"+"Some data found after closing gpx tag, truncated");
    return buf.substring(0, theEnd);    
  }
    
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
    while (parser.next() != XmlPullParser.END_TAG) {
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
    // ele time name sym cmt desc
    while (parser.next() != XmlPullParser.END_TAG) {
      if (parser.getEventType() != XmlPullParser.START_TAG) {
        continue;
      }
      String name = parser.getName();
      if (name.equals("ele")) {
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
        csv.put("desc",readTagText("desc",parser));
      }
      else {
        skip(parser);
      }
    }
    return postProcess(csv);
  }

  private Map<String,String> postProcess(Map<String,String> csv) {
    String desc=csv.get("desc");
    if (desc != null && ! desc.equals("") && ! desc.equals(Point.SEP)) {
      int sp=desc.indexOf(Point.SEP);
      if (sp >=0) {
        String[] cellNote=TextUtils.split(desc,Point.SEP);// string.split() SUCKS !!!
        csv.put("cellData",cellNote[0]);
        csv.put("note",cellNote[1]);
      }
    }
    String type=csv.get("type");
    //if (U.DEBUG) Log.d(U.TAG,"GpxHelper:","type="+type);
    if (type == null || type.equals("")) {
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
  
  private String mWptTemplate;
  private Map<String,String> mSyms=new ArrayMap<String,String>();
  public String mProgress="";
  public int mCount=0;
  private String mHeader=TextUtils.join(Point.SEP, Point.FIELDS);
  private String mGpxFramingHead;
  private String mGpxFramingTail;
  
  private void init() {
    mWptTemplate="<wpt lat=\"%s\" lon=\"%s\" %s%s%s>%s<time>%s</time><name>%s</name>%s<sym>%s</sym>%s</wpt>";
    mSyms.put("cell","Navaid, Blue");
    mSyms.put("gps","Flag, Red");
    mSyms.put("mark","Navaid, Violet");// Navaid, Magenta is rendered as Flag, Blue by Garmin
    mSyms.put("default","Navaid, Red");
    mGpxFramingHead="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx>";
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
      wpt=makeGpxEntry(csv);
      outBuf+=wpt+Point.NL;
      mCount+=1;
    }
    outBuf=mGpxFramingHead+Point.NL+outBuf+mGpxFramingTail;
    return outBuf;
  }

  private String makeGpxEntry(Map<String,String> content) throws U.DataException {
    if ( content.get("id").isEmpty() ) { throw new U.DataException("No id"); }
    String id=content.get("id");
    if ( content.get("lat").isEmpty() || content.get("lon").isEmpty()) {
      mProgress+="\n"+id+": no coords, skipped";
      return "";
    }
    String typeAttr=" type=\""+content.get("type")+"\" ";
    String rangeAttr="";
    if ( ! content.get("range").isEmpty() ) {
      rangeAttr=" range=\""+U.str2int(content.get("range"))+"\" ";
    }
    String protectAttr="";
    if ( ! content.get("protect").isEmpty() ) protectAttr=" protect=\""+content.get("protect")+"\" ";
    String time="";
    if ( ! content.get("time").isEmpty() ) {
      try { time=U.localTimeToUTC(content.get("time")); }
      catch (U.DataException e)  {
        try { time=U.localTimeToUTC(Point.getDate()); }
        catch (U.DataException ee)  { time=""; }
      }
    }    
    String eleEl="";
    if ( ! content.get("alt").isEmpty() ) {
      eleEl="<ele>"+content.get("alt")+"</ele>";
    }
    String cmtEl="";
    if ( ! content.get("comment").isEmpty() ) {
      cmtEl="<cmt>"+content.get("comment")+"</cmt>";
    }
    String sym;
    if ( ! content.get("sym").isEmpty() ) sym=content.get("sym");
    else sym=getSym(content.get("type"));
    String descEl="";
    if ( ! content.get("cellData").isEmpty() || ! content.get("note").isEmpty() ) {
      descEl="<desc>"+content.get("cellData")+Point.SEP+content.get("note")+"</desc>";
    }
    //<wpt lat="%s" lon="%s" %s%s%s>%s<time>%s</time><name>%s</name>%s<sym>%s</sym>%s</wpt>
    String entry=String.format(mWptTemplate, content.get("lat"), content.get("lon"), typeAttr, rangeAttr, protectAttr, 
        eleEl, time, id, cmtEl, sym, descEl);
    return entry;
  }
  
  private String getSym(String type) {
    if ( type.equals("cell") || type.equals("gps") || type.equals("mark") ) return mSyms.get(type);
    return mSyms.get("default");
  }

}

