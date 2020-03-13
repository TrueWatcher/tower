package truewatcher.tower;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import truewatcher.tower.TestHelper.TestFailure;

public class Tests1 extends SingleFragmentActivity {
// adb shell am start -n truewatcher.tower/truewatcher.tower.Tests1
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }
  
  @TargetApi(23) 
  public static class Tests1Fragment extends Fragment {
    private TextView TvA;
    private TestHelper th=TestHelper.getInstance(null);
    private String mPath;
    private PointList mPl;
    private StorageHelper mSh;
    private TrackStorage mTs;
    private GpxHelper mGh;
    private String mGpxBuffer;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.i(U.TAG,"Tests1Fragment:onCreate");
    }
    
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
      Log.i(U.TAG,"mainFragment:onCreateView");
      View v = inflater.inflate(R.layout.fragment_tests, container, false);
      U.debugOn();
      TvA = (TextView) v.findViewById(R.id.tvA);
      //TvA.setText("Starting tests");
      th=TestHelper.getInstance(TvA);
      th.printlnln("Running Tests1...");
      return v;
    }
    
    @Override
    public void onResume() {
      super.onResume();
      try {
        //testAssertions();
        testTimeConversion();
        mPath = getActivity().getExternalFilesDir(null).getPath();
        //testFileUtilities();
        mSh = new StorageHelper();
        testCsvImport();
        testCsvExport();
        //testDistancesPresentation();
        testDistances();
        testTrackUtils();
        testGpxConversions();
        testListRotation();
        testSavedFiles();
        testSavedFiles2();
        
        th.printlnln("Tests1 completed SUCCESSFULLY");
      }
      catch (TestHelper.TestFailure e) {
        th.println(e.getMessage());
      }
      catch (IOException e) {
        th.println("IOException");
        th.println(e.getMessage());
      }
      catch (U.DataException e) {
        th.print("U.DataException:");
        th.println(e.getMessage());
      }
      catch (U.FileException e) {
        th.print("U.FileException:");
        th.println(e.getMessage());
      }
    }// end onResume
    
    private void testAssertions() throws TestFailure {
      th.printlnln("Testing assertion utilities -----");
      th.assertTrue(true, "assertTrue failed", "check assertTrue true");
      //th.println("Checking assertTrue with false, expecting failure");
      //th.assertTrue(false, "assertTrue gets false","");
      th.assertEquals(6, 3+3, "assertEquals failed", "check assertEquals int");
      th.assertEquals("йцукенг", "йцу"+"кенг", "assertEquals failed", "check assertEquals strings");
      th.assertEqualsList(Arrays.asList("foo", "bar", "baz"), Arrays.asList("foo", "bar", "baz"),
          "assertEquals failed", "check assertEqualsList strings");
      //th.println("Checking assertEqualsList with different lists, expecting failure");
      //th.assertEqualsList(Arrays.asList("foo", "bar", "baz"), Arrays.asList("foo", "baz", "bar"),"assertEquals failed", "check assertEqualsList strings");
      th.assertContains("йцукенг", "йцу"+"кенг", "assertContains failed", "check assertContains");
      th.assertContains("йцу"+"кенг", "йцукенг", "assertContains failed", "check assertContains again");
      //th.assertContains("йценг", "йцу"+"кенг", "assertContains got the uncontained", "assertContains should fail here");
      th.assertNotContains("йценг", "йцукенг", "assertNotContains failed", "check assertNotContains");
      //th.assertNotContains("енг", "йцукенг", "assertNotContains got inclusion", "assertNotContains should fail here");
    }
    
    private void testFileUtilities() throws TestFailure, IOException {
      th.printlnln("Testing file utilities -----");
      String testName0="test_".concat(Point.getDate());
      String testString="data_".concat(testName0);
      String extDat="dat";
      String testName=testName0+"."+extDat;
      String withExt=U.assureExtension(testName, extDat);
      th.assertEquals(testName,withExt,"Wrong extension check","Extension same");
      String withExt2=U.assureExtension(testName0, extDat);
      th.assertEquals(testName,withExt2,"Wrong extension append","Extension added");
      String withExt3=U.assureExtension(testName0+".txt", extDat);
      th.assertEquals(testName,withExt3,"Wrong extension overwrite","Extension enforced");
      
      File exists0=U.fileExists(mPath, testName, extDat);
      th.assertTrue(null == exists0,"The target file already exists somehow","File "+testName+" is new, writing...");
      U.filePutContents(mPath, testName, testString, false);
      File exists1=U.fileExists(mPath, testName, extDat);
      //Log.d(U.TAG,"Tests1:"+exists1.getName());
      th.assertTrue(null != exists1, "Failed to create file", "File created");
      String[] dirFull=U.getCatalog(mPath);
      th.assertTrue(U.arrayContains(dirFull,testName), "Missing test dat file from full directory","The test dat is visible");
      String[] dirDat=U.getCatalog(mPath, extDat);
      th.assertTrue(U.arrayContains(dirDat,testName), "Missing test dat file from filtered directory","The test dat is visible");
      String readBack=U.fileGetContents(mPath, testName);
      th.assertEquals(testString, readBack, "Wrong readBack", "Contents Ok");
 
      String append="append_довесок";
      //String testStringPlus = testString.concat(append);
      U.filePutContents(mPath, testName, append, true);
      String readBack2=U.fileGetContents(mPath, testName);
      th.assertEquals(testString.concat(append), readBack2, "Wrong readBack after append", "Append Ok");
      
      U.Summary isRemoved=U.unlink(mPath, testName);
      th.assertEquals("deleted", isRemoved.act, "Wrong unlink result", "Unlink: deleted");
      th.assertEquals(testName, isRemoved.fileName, "Wrong unlink fileName", "Unlink: fileName Ok");
      File exists2=U.fileExists(mPath, testName, extDat);
      th.assertTrue(null == exists2,"The target file still exists","File "+testName+" is gone");
    }

    private void testTimeConversion() throws IOException, TestFailure, U.DataException  {
      th.printlnln("Time conversion -----");
      th.printlnln("Local offset (hr):"+String.valueOf(U.getTimeOffsetHr()));
      String utc1="2019-10-17T00:58:43Z";
      th.println("UTC:"+utc1+">Local:"+U.utcToLocalTime(utc1));
      String utc2="2019-10-17T23:58:43Z";
      th.println("UTC:"+utc2+">Local:"+U.utcToLocalTime(utc2));
      String loc1="2019-11-17 10:01";
      th.println("Local:"+loc1+">UTC:"+U.localTimeToUTC(loc1));
    }

    private void testDistancesPresentation() {
      th.printlnln("Distance presentation -----");
      th.println("123>"+ListHelper.proximityToKm(123));
      th.println("1234>"+ListHelper.proximityToKm(1234));
      th.println("12345>"+ListHelper.proximityToKm(12345));
      th.println("123456>"+ListHelper.proximityToKm(123456));
      th.println("1234567>"+ListHelper.proximityToKm(1234567));
    }
    
    private String getTestCsv1() { return getTestCsv1(-1); }
    
    private String getTestCsv1(int i) {
      String NL="\n";
      String[] lines=new String[4];
      lines[0]="id;type;comment;protect;lat;lon;alt;range;time;cellData;note;sym";
      lines[1]="1;cell;Москва;;55.75321578979492;37.62250518798828;;100000.0;2019-01-28 16:59;{\"type\":\"WCDMA\",\"MCC\":250,\"MNC\":99,\"LAC\":27678,\"CID\":18654};;";
      lines[2]="2;mark;Sh_E_br;true;55.767812811849424;37.80133621206915;;;2019-01-28 17:13;;Has_a_note!;";
      lines[3]="8;gps;G8;;56.023113333333335;37.12359166666667;202.3;7.8;2019-02-04 23:10;;;";
      if (i >= 0 && i < lines.length) return lines[i];
      String all=TextUtils.join(NL,lines)+NL;
      return all;
    }
    
    private String getTestCsv2(int i) {
      String[] lines=new String[4];
      lines[0]="id;type;comment;protect;lat;lon;alt;range;time;cellData;note;sym";
      lines[1]="9;cell;СПб;true;59.93894958496094;30.31563568115234;;100000.0;2019-02-05 06:28;{\"type\":\"WCDMA\",\"MCC\":250,\"MNC\":99,\"LAC\":14782,\"CID\":15258};;";
      lines[2]="11;mark;Тверь_вкз;;56.83574380308231;35.8939038708013;;;2019-03-06 18:09;;;";
      lines[3]="10;mark;МосВок;;59.92962552552448;30.36255102990254;;;2019-02-05 06:32;;;";
      if (i >= 0 && i < lines.length) return lines[i];
      return "";
    }
    
    private String getTestCsv3(int i) {
      String NL="\n";
      String[] lines=new String[2];
      lines[0]="id;type;comment;protect;lat;lon;alt;range;time;cellData;note;sym";
      lines[1]="1;cell;Cell275;;55.75551986694336;37.64715957641602;;470.6525573730469;2019-05-14 13:12;{\"type\":\"LTE\",\"MCC\":250,\"MNC\":99,\"LAC\":4077,\"CID\":197175046,\"PCI\":275};;";
      if (i >= 0 && i < lines.length) return lines[i];
      String all=TextUtils.join(NL,lines)+NL;
      return all;
    }
    
    private PointList testCsvImport() throws IOException, TestFailure, U.DataException {
      th.printlnln("Testing basic csv import -----");
      String extCsv="csv";
      String testName0="test_".concat(Point.getDate());
      String testNameCsv=testName0+"."+extCsv;
      U.filePutContents(mPath, testNameCsv, getTestCsv1(), false);
      String headerLine=TextUtils.join(Point.SEP, Point.FIELDS)+Point.NL;
      th.print("Checking the csv header: ");
      th.csvLineDiff(headerLine, getTestCsv1(0), Point.SEP);
      
      mSh.init(getActivity(), testNameCsv);
      int maxPointCount=3;
      mPl=new PointList(maxPointCount, mSh);
      String[] dir=U.getCatalog(mSh.getMyDir(), extCsv);
      th.assertTrue(U.arrayContains(dir,testNameCsv), "Missing test csv file from directory",
              "The test csv is visible");
      int tryCount=mSh.getPointCount(testNameCsv);
      //th.println("found points:"+tryCount);
      th.assertEquals(3, tryCount, "Wrong point count of the test csv", "Point count Ok ("+tryCount+")");
      
      U.Summary loaded=mPl.load();
      th.assertEquals(tryCount, loaded.adopted, "Wrong loaded point count",
              "Loaded point count Ok (".concat(String.valueOf(loaded.adopted)).concat(")"));
      Point p1=mPl.getById(1);
      th.assertEquals("cell", p1.getType(), "Wrong point 1 type", "Point 1:"+p1.getType());
      th.assertEquals("Москва", p1.getComment(), "Wrong point 1 name", "Point 1:"+p1.getComment());
      th.assertTrue(! p1.isProtected(),"Wrongly protected Point 1","unprotected");
      th.assertTrue(p1.hasCoords(),"Missing coords from Point 1","coords present");
      th.assertEquals("100000.0", p1.range, "Wrong RANGE", "RANGE present");
      th.assertContains("16:59", p1.time, "Wrong TIME", "TIME present");
      th.assertContains("MCC", p1.cellData, "Wrong CELLDATA", "CELLDATA present");
      Point p2=mPl.getById(2);
      th.assertEquals("mark", p2.getType(), "Wrong point 2 type", "Point 2:"+p2.getType());
      th.assertEquals("Sh_E_br", p2.getComment(), "Wrong point 2 name", "Point 2:"+p2.getComment());
      th.assertTrue(p2.isProtected(),"Wrongly unprotected Point 2","protected");
      th.assertEquals("Has_a_note!", p2.getNote(), "Wrong NOTE", "NOTE present");
      Point p8=mPl.getById(8);
      th.assertEquals("gps", p8.getType(), "Wrong point 8 type", "Point 8:"+p8.getType());
      th.assertEquals("G8", p8.getComment(), "Wrong point 8 name", "Point 8:"+p8.getComment());
      th.assertEquals("202.3", p8.alt, "Wrong point 8 altitude", "ALT present");
      
      U.Summary isRemoved=U.unlink(mPath, testNameCsv);
      th.assertEquals("deleted", isRemoved.act, "Wrong unlink result", "Unlink: deleted");
      th.assertEquals(testNameCsv, isRemoved.fileName, "Wrong unlink fileName", "Unlink: "+testNameCsv+" Ok");
      return mPl;
    }
    
    private void testDistances() throws TestFailure, U.DataException {
      th.printlnln("Testing distances and findNearest -----");
      Point p1=mPl.getById(1);
      Point p2=mPl.getById(2);
      Point p8=mPl.getById(8);
      mPl.setProximityOrigin(p2);
      String p1_p2 = ListHelper.proximityToKm(U.proximityM(p1, mPl.getProximityOrigin()));
      th.assertEquals("11.31km", p1_p2, "Wrong p1-p2", "p1-p2 : "+p1_p2);
      String p8_p2 = ListHelper.proximityToKm(U.proximityM(p8, mPl.getProximityOrigin()));
      th.assertEquals("50.91km", p8_p2, "Wrong p8-p2", "p8-p2 : "+p8_p2);
      String p2_p2 = ListHelper.proximityToKm(U.proximityM(p2, mPl.getProximityOrigin()));
      th.assertEquals("0m", p2_p2, "Wrong p2-p2", "p2-p2 : "+p2_p2);
      //th.println("p1 to p2:"+U.proximityM(p1, pl.getLocation()));//11305.53437084871
      //th.println("p8 to p2:"+U.proximityM(p8, pl.getLocation()));//50905.7112442492
      //th.println("p2 to itself:"+U.proximityM(p2, pl.getLocation()));//0.0
      th.println("azimuth p1 to p2:"+U.azimuth(p1, p2));
      th.println("azimuth p2 to p1:"+U.azimuth(p2, p1));
      th.println("azimuth p8 to p1:"+U.azimuth(p8, p1));
      th.println("azimuth p1 to p8:"+U.azimuth(p1, p8));
      Point nearP2=new Point("mark", p2.lat+1, p2.lon+5);
      th.assertEquals(2, mPl.findNearest(nearP2), "Failed to find p2 from nearP2",
              "Found p2");
      th.assertEquals(8, mPl.findNearest(p8), "Failed to find p8 from itself",
              "Found p8");
      PointList pl2=new PointList(5);
      th.assertEquals(-1, pl2.findNearest(p8), "Wrong result on empty pointlist",
              "Empty pointlist gives -1");
      String uncell="9;cell;unresolved;;;;;;2019-02-05 06:28;{\"type\":\"WCDMA\",\"MCC\":250,\"MNC\":99,\"LAC\":14782,\"CID\":15258};;";
      Point puc=(new Point()).fromCsv(uncell);
      pl2.addAndShiftNext(puc);
      th.assertEquals(1, pl2.getSize(), "Wrong size of one unresolved cell",
              "one unresolved cell: size=1");
      th.assertEquals(-1, pl2.findNearest(p8), "Wrong result on one unresolved cell",
              "one unresolved cell gives -1");
    }

    private String getTestTrackGpx() {
      String s="";
      s+="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>" +
              "<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"truewatcher.tower\" >";
      s+="<wpt lat=\"1\" lon=\"2\"><ele>123.45</ele><time>2019-12-09T00:01:02Z</time></wpt>";
      s+="<trk><trkseg><trkpt lat=\"1\" lon=\"2\"><ele>123.45</ele><time>2019-12-09T00:01:02Z</time></trkpt>" +
              "<trkpt lat=\"3\" lon=\"4\"><ele>123.45</ele><time>2019-12-09T00:01:03Z</time></trkpt>" +
              "<trkpt lat=\"5\" lon=\"6\"><ele>123.45</ele><time>2019-12-09T00:01:04Z</time></trkpt>" +
              "</trkseg>";
      s+="<wpt lat=\"1\" lon=\"2\"><ele>123.45</ele><time>2019-12-09T00:01:02Z</time></wpt>";
      s+="<trkseg><trkpt lat=\"7\" lon=\"8\"><ele>23.45</ele></trkpt>" +
              "<trkpt lat=\"9\" lon=\"10\"><ele>23.45</ele></trkpt>" +
              "<trkpt lat=\"-1.10\" lon=\"1.11\"><ele>23.45</ele></trkpt>" +
              "<trkpt lat=\"12\" lon=\"13\"><ele>23.45</ele></trkpt>" +
              "</trkseg></trk>";
      s+="<wpt lat=\"1\" lon=\"2\"><ele>123.45</ele><time>2019-12-09T00:01:02Z</time></wpt>";
      s+="</gpx>";
      return s;
    }

    private String getTestTrackCsv() {
      String header = TextUtils.join(Trackpoint.SEP, Trackpoint.FIELDS);
      String s="";
      s+=header+Point.NL;
      s+="note;;2020-02-17 23:19:58;;;;;onStartCommand;intnt:true,flags:0,id:1"+Point.NL;
      s+="T;1;2020-02-17 23:21:13;1;2;123.45;8.0;;"+Point.NL;
      s+="T;;2020-02-17 23:21:13;3;4;123.45;8.0;;"+Point.NL;
      s+="T;;2020-02-17 23:21:13;5;6;123.45;8.0;;"+Point.NL;
      s+="note;;2020-02-18 00:22:55;;;;;onStartCommand;intnt:true,flags:0,id:1"+Point.NL;
      s+="T;2;2020-02-17 23:21:13;7;8;23.45;8;;"+Point.NL;
      s+="T;;2020-02-17 23:21:13;9;10;23.45;8.0;;"+Point.NL;
      s+="note;;2020-02-18 00:22:55;;;;;onStartCommand;intnt:true,flags:0,id:1"+Point.NL;
      s+="note;;2020-02-18 00:22:55;;;;;onStartCommand;intnt:true,flags:0,id:1"+Point.NL;
      s+="T;;2020-02-17 23:21:13;-1.10;1.11;23.45;8.0;;"+Point.NL;
      s+="T;;2020-02-17 23:21:13;12;13;23.45;8.0;;"+Point.NL;
      return s;
    }


    private void testTrackUtils() throws TestFailure, IOException {
      th.printlnln("Testing track infrastructure -----");
      th.println("Joining JSON arrays");
      String json1="[[1,2],[3,4],[5,6]]";
      String json2="[[7,8],[9,10],[-1.10,1.11],[12,13]]";
      String json3="[[[1,2],[3,4],[5,6]],[[7,8],[9,10],[-1.10,1.11],[12,13]]]";
      String add12=U.joinJsonArrays(json1,json2);
      th.println(add12);
      th.assertEquals(8, U.countEntries(add12,"["),"Wrong [ count",
              "found [:".concat(String.valueOf(U.countEntries(add12,"["))));
      th.assertEquals(U.countEntries(add12,"["), U.countEntries(add12,"]"),"Unequal [/] count",
              "found ]:same");
      String add21=U.joinJsonArrays(json2,json1);
      th.println(add21);
      th.assertEquals(8, U.countEntries(add21,"["),"Wrong [ count",
              "found [:".concat(String.valueOf(U.countEntries(add21,"["))));
      th.assertEquals(U.countEntries(add21,"["), U.countEntries(add21,"]"),"Unequal [/] count",
              "found ]:same");

      mGh=new GpxHelper();
      String latLonJson = "[]";
      try {
        th.println("Parsing a test GPX track");
        latLonJson = mGh.track2latLonJson(getTestTrackGpx());
        th.println(latLonJson);
        U.Summary res=mGh.getResults();
        th.assertEquals(2,res.segments,"Wrong segment count",
                "found segments:".concat(String.valueOf(res.segments)));
        th.assertEquals(7,res.adopted,"Wrong point count",
                "found points:".concat(String.valueOf(res.adopted)));
        th.assertEquals(json3, latLonJson,"Wrong trackGpx to latLonJson conversion", "latLonJson OK");
        int foundCommas=U.countEntries(latLonJson,",");
        th.assertEquals(2*res.adopted-1, foundCommas,"Wrong , count",
                "found commas:".concat(String.valueOf(foundCommas)));
        th.assertEquals(1+res.segments+res.adopted, U.countEntries(latLonJson,"["),"Wrong [ count",
                "found [:".concat(String.valueOf(U.countEntries(latLonJson,"["))));
        th.assertEquals(U.countEntries(latLonJson,"["),U.countEntries(latLonJson,"]"),"Unequal [/] count",
                "found ]:same");
      }
      catch (U.DataException e) {
        th.println("Error while converting:"+e.getMessage());
      }

      th.println("Adding same array");
      String added=U.joinJsonArrays(json3,latLonJson);
      th.println(added);
      th.assertEquals(27, U.countEntries(added,","),"Wrong , count",
              "found commas:".concat(String.valueOf(27)));
      th.assertEquals(1+4+14, U.countEntries(added,"["),"Wrong [ count",
              "found [:".concat(String.valueOf(U.countEntries(added,"["))));
      th.assertEquals(U.countEntries(added,"["),U.countEntries(added,"]"),"Unequal [/] count",
              "found ]:same");

      latLonJson = "[]";
      try {
        mTs=new TrackStorage();
        TrackStorage.Track2LatLonJSON converter=mTs.new Track2LatLonJSON();
        th.println("Parsing a test CSV track");
        latLonJson = converter.csv2LatLonJSON(getTestTrackCsv());
        th.println(latLonJson);
        U.Summary res=converter.getResults();
        th.assertEquals(2,res.segments,"Wrong segment count",
                "found segments:".concat(String.valueOf(res.segments)));
        th.assertEquals(7,res.adopted,"Wrong point count",
                "found points:".concat(String.valueOf(res.adopted)));
        th.assertEquals(json3, latLonJson,"Wrong trackGpx to latLonJson conversion", "latLonJson OK");
      }
      catch (U.DataException e) {
        th.println("Error while converting:"+e.getMessage());
      }

      th.println("Removing last segment");
      String truncated = "***";
      try {
        truncated = new TrackStorage().deleteLastSegmentString(getTestTrackCsv());
        TrackStorage.Track2LatLonJSON converter=mTs.new Track2LatLonJSON();
        latLonJson = converter.csv2LatLonJSON(truncated);
        th.println(latLonJson);
        U.Summary res=converter.getResults();

        th.assertEquals(1,res.segments,"Wrong segment count",
              "found segments:".concat(String.valueOf(res.segments)));
        th.assertEquals(3,res.adopted,"Wrong point count",
              "found points:".concat(String.valueOf(res.adopted)));
        int foundCommas=U.countEntries(latLonJson,",");
        th.assertEquals(2*res.adopted-1, foundCommas,"Wrong , count",
              "found commas:".concat(String.valueOf(foundCommas)));
        th.assertEquals(1+res.segments+res.adopted, U.countEntries(latLonJson,"["),"Wrong [ count",
              "found [:".concat(String.valueOf(U.countEntries(latLonJson,"["))));
        th.assertEquals(U.countEntries(latLonJson,"["),U.countEntries(latLonJson,"]"),"Unequal [/] count",
              "found ]:same");
      }
      catch (U.DataException e) {
        th.println("Error while removing last segment:"+e.getMessage());
      }

      try {
        th.println("Parsing a test CSV converted to GPX");
        TrackStorage.TrackToGpx converter = mTs.new TrackToGpx();
        String converted = converter.csv2gpx("test_track", getTestTrackCsv());
        th.println("Produced a GPX of "+converted.length()+" bytes");
        latLonJson = mGh.track2latLonJson(converted);
        th.println(latLonJson);
        U.Summary res=converter.getResults();
        U.Summary res2=mGh.getResults();
        th.assertEquals(res2.segments,res.segments,"Wrong segment count",
                "found segments:".concat(String.valueOf(res.segments)));
        th.assertEquals(res2.adopted,res.adopted,"Wrong point count",
                "found points:".concat(String.valueOf(res.adopted)));
        th.assertEquals(json3, latLonJson,"Wrong trackGpx to latLonJson conversion", "latLonJson OK");
      }
      catch (U.DataException e) {
        th.println("Error while converting:"+e.getMessage());
      }


    }
    
    private void testCsvExport() throws TestFailure {
      th.printlnln("Testing conversion back to csv -----");
      Point p1=mPl.getById(1);
      Point p2=mPl.getById(2);
      Point p8=mPl.getById(8);
      th.print("1: ");
      th.csvLineDiff(getTestCsv1(1), p1.toCsv(), Point.SEP);
      th.print("2: ");
      th.csvLineDiff(getTestCsv1(2), p2.toCsv(), Point.SEP);
      th.print("8: ");
      th.csvLineDiff(getTestCsv1(3), p8.toCsv(), Point.SEP);
    }
    
    private void testGpxConversions() throws U.DataException, TestFailure, IOException {
      th.printlnln("Testing basic GPX conversions -----");
      GpxHelper gh=new GpxHelper();        
      String gpx=gh.csv2gpx(getTestCsv1());
      th.assertEquals(3, gh.mCount, "Wrong export point count", "Point count Ok ("+gh.mCount+")");
      Point p1=mPl.getById(1);
      Point p2=mPl.getById(2);
      Point p8=mPl.getById(8);
      th.assertContains(p1.getComment(), gpx, "Missing point 1 name", "Point 1:"+p1.getComment());
      th.assertContains(p2.getComment(), gpx, "Missing point 2 name", "Point 2:"+p2.getComment());
      th.assertContains(p8.getComment(), gpx, "Missing point 8 name", "Point 8:"+p8.getComment());
      mGpxBuffer=gpx;
      
      String csv=gh.gpx2csv(gpx);
      //th.assertContains(p1.getComment(), csv, "Missing point 1 name", "Point 1:"+p1.getComment());
      //th.assertContains(p2.getComment(), csv, "Missing point 2 name", "Point 2:"+p2.getComment());
      //th.assertContains(p8.getComment(), csv, "Missing point 8 name", "Point 8:"+p8.getComment());
      String[] csvLines=TextUtils.split(csv, Point.NL);
      String headerLine=TextUtils.join(Point.SEP, Point.FIELDS)+Point.NL;
      th.print("header: ");
      th.csvLineDiff(headerLine, csvLines[0], Point.SEP);
      th.print("1: ");
      th.csvLineDiff(getTestCsv1(1), csvLines[1], Point.SEP);
      th.print("2: ");
      th.csvLineDiff(getTestCsv1(2), csvLines[2], Point.SEP);
      th.print("8: ");
      th.csvLineDiff(getTestCsv1(3), csvLines[3], Point.SEP);
    }
    
    private void testListRotation() throws U.DataException, TestFailure, IOException {
      th.printlnln("Testing list rotation -----");
      String trashFile="trash.csv";
      if (null != U.fileExists(mPath, trashFile, "csv")) { U.unlink(mPath, trashFile); }
      th.assertTrue(null == U.fileExists(mPath, trashFile, "csv"), "Trash file still present",
              "No more trash file");
      mPl.forceUseTrash();
      th.assertEquals(3, mPl.getMax(), "Wrong MAXCOUNT="+mPl.getMax(),"MAXCOUNT Ok");
      th.assertEquals(mPl.getMax(), mPl.getSize(), "Wrong SIZE="+mPl.getMax(),"SIZE Ok");
      th.assertEqualsList(Arrays.asList("1","2","8"), mPl.getIndices(), "Wrong index list",
              "Point list Ok: 1 2 8");
      th.assertTrue( ! mPl.isDirty(), "DIRTY is set", "DIRTY is false");
      int sizeBeforeAdd=mPl.getSize();
      
      th.println("Adding over an unprotected point, expecting replacement");
      Point edge1=mPl.getEdge();
      int eid1=mPl.getEdge().getId();
      th.assertEquals(1, eid1, "Wrong edge point="+eid1, "About to remove:"+eid1);
      Point p9=(new Point()).fromCsv(getTestCsv2(1));
      mPl.addAsNext(p9);
      th.assertEquals(sizeBeforeAdd, mPl.getSize(), "Wrong SIZE="+mPl.getMax(),"SIZE same");
      th.assertTrue(mPl.isDirty(), "DIRTY is not set", "DIRTY is set");
      Point p9back=mPl.getById(9);
      th.assertEquals("СПб",p9back.getComment(),"Wrong added COMMENT","Added:"+p9back.getComment());
      th.print("9:");
      th.csvLineDiff(getTestCsv2(1), p9back.toCsv(), Point.SEP);
      th.assertTrue(null == mPl.getById(edge1.getId()), "Edge point not removed",
              "Edge point removed");
      th.assertEqualsList(Arrays.asList("2","8","9"), mPl.getIndices(), "Wrong index list",
              "Removed #1, added #9");
      
      th.println("Adding over a protected point, expecting replacement of another unprotected");
      Point edge2=mPl.getEdge();// #2 is protected, next is #8
      int eid2=mPl.getEdge().getId();
      th.assertEquals(8, eid2, "Wrong edge point="+eid2, "About to remove:"+eid2);
      Point p11=(new Point()).fromCsv(getTestCsv2(2));
      mPl.addAndShiftNext(p11);
      th.assertEqualsList(Arrays.asList("2","9","11"), mPl.getIndices(), "Wrong index list",
              "Removed #8, added #11");
      
      th.println("Adding over all protected points, expecting exception");
      th.assertTrue( ! p11.isProtected(), "Point is wrongly protected",
              "Point "+p11.getId()+" is not protected");
      mPl.getById(11).protect();
      th.assertTrue(mPl.getById(11).isProtected(), "Point is not set protected",
              "Point "+p11.getId()+" is set to protected");
      Point p12=(new Point()).fromCsv(getTestCsv2(3));
      String exceptionThrown="";
      try { mPl.addAndShiftNext(p12); } catch (U.DataException e) { exceptionThrown=e.getMessage(); }
      th.assertContains("No room",exceptionThrown,"No or wrong exception on all protected",
              "Exception on all protected");
      th.assertEqualsList(Arrays.asList("2","9","11"), mPl.getIndices(), "Wrong index list",
              "Point list unchanged");
      
      th.println("Unprotecting one point and adding over it");
      mPl.getById(2).unprotect();
      th.assertTrue( ! mPl.getById(2).isProtected(), "Point is not set unprotected",
              "Point "+"2"+" is set to unprotected");
      int eid3=mPl.getEdge().getId();
      th.assertEquals(2, eid3, "Wrong edge point="+eid3, "About to remove:"+eid3);
      mPl.addAndShiftNext(p12);
      th.assertEqualsList(Arrays.asList("9","11","12"), mPl.getIndices(), "Wrong index list",
              "Added #12, removed #2");
      th.assertEquals("МосВок",mPl.getById(12).getComment(),"Wrong added COMMENT",
              "Added:"+mPl.getById(12).getComment());
      
      th.println("Trying to low-level remove a protected point, expecting failure");
      mPl.moveUnprotectedToTrash(11);// it is protected
      th.assertEqualsList(Arrays.asList("9","11","12"), mPl.getIndices(), "Wrong index list",
              "Protected point not removed");
      
      th.println("Renumber points from 1");
      mPl.renumber();
      th.assertEqualsList(Arrays.asList("1","2","3"), mPl.getIndices(), "Wrong index list",
              "Point list Ok after renumbering");
      th.assertEquals("МосВок",mPl.getById(3).getComment(),"Wrong added COMMENT",
              "Name is same after renumbering");
    }
    
    private void testSavedFiles()
        throws TestFailure, IOException, U.DataException, U.FileException {
      th.printlnln("Testing saved points and trash -----");
      mPl.save();
      th.assertTrue( ! mPl.isDirty(), "DIRTY is set", "DIRTY is cleared on save");
      th.assertEqualsList(Arrays.asList("1","2","3"), mPl.getIndices(), "Wrong index list",
              "Point list Ok");
      
      th.println("Trying to open csv with more than MAXCOUNT lines, expecting exception");
      int maxPointCount=2;// deliberately too small
      PointList newPl=new PointList(maxPointCount, mSh);
      String exceptionThrown="";
      try { U.Summary loaded0=newPl.load(); } catch (U.DataException e) { exceptionThrown=e.getMessage(); }
      th.assertContains("Set max point count",exceptionThrown,"No or wrong exception on too large csv file",
              "Exception on too large csv Ok");
      
      th.println("Load into a fresh PointList and verify");
      newPl.adoptMax(mPl.getSize());
      newPl.load();
      th.assertEqualsList(Arrays.asList("1","2","3"), newPl.getIndices(), "Wrong index list",
              "Point list Ok");
      th.print(newPl.getById(1).getComment()+" : ");
      th.csvLineDiff(getTestCsv2(1), newPl.getById(1).toCsv(), Point.SEP);
      th.print(newPl.getById(2).getComment()+" : ");
      th.csvLineDiff(getTestCsv2(2), newPl.getById(2).toCsv(), Point.SEP);
      th.print(newPl.getById(3).getComment()+" : ");
      th.csvLineDiff(getTestCsv2(3), newPl.getById(3).toCsv(), Point.SEP);
      
      String testNameCsv=mSh.getMyFile();
      U.unlink(mPath, testNameCsv);
      
      th.println("Open the trash and verify");
      newPl.forceNotUseTrash();
      mSh.trySetMyFile("trash.csv");
      newPl.clearAndLoad();
      th.assertEqualsList(Arrays.asList("1","8","9"), newPl.getIndices(), "Wrong index list",
              "Point list Ok");
      th.print(newPl.getById(1).getComment()+" : ");
      th.csvLineDiff(getTestCsv1(1), newPl.getById(1).toCsv(), Point.SEP);
      th.print(newPl.getById(8).getComment()+" : ");
      th.csvLineDiff(getTestCsv1(3), newPl.getById(8).toCsv(), Point.SEP);
      th.print(newPl.getById(9).getComment()+" : ");
      th.csvLineDiff(getTestCsv1(2), newPl.getById(9).toCsv(), Point.SEP);      
      U.unlink(mPath, "trash.csv");
    }
    
    private void testSavedFiles2()
        throws TestFailure, IOException, U.DataException, U.FileException {
      th.printlnln("Testing more list operations -----");
      String extCsv="csv";
      String testName2="test2_".concat(Point.getDate());
      String testName2Csv=testName2+"."+extCsv;
      U.filePutContents(mPath, testName2Csv, getTestCsv3(-1), false);
      
      th.println("Protecting a point, expecting it to be removed anyway on File-open");
      mPl.getById(2).protect();
      th.assertTrue(mPl.getById(2).isProtected(), "Failed to protect point","Point protected");
      mSh.trySetMyFile(testName2Csv);
      mPl.forceNotUseTrash();
      mPl.clearAndLoad();
      th.assertTrue(null == mPl.getById(2), "Protected point not removed",
              "Protected point removed on File-open");
      th.assertEqualsList(Arrays.asList("1"), mPl.getIndices(), "Wrong index list",
              "Point list Ok, count starts from 1");
      th.print(mPl.getById(1).getComment()+" : ");
      th.csvLineDiff(getTestCsv3(1), mPl.getById(1).toCsv(), Point.SEP);
      String next=mPl.getNextS();
      th.assertEquals("2", next, "Wrong NEXT="+next, "NEXT is Ok:"+next);
      U.unlink(mPath, testName2Csv);
      
      th.println("Testing GPX import");
      th.assertTrue(null != mGpxBuffer,"No GPX string, make sure testGpxConversions was run",
              "Preparing GPX");
      String extGpx="gpx";
      String testName2Gpx=testName2+"."+extGpx;
      U.filePutContents(mPath, testName2Gpx, mGpxBuffer, false);
      mPl.adoptMax(5);
      U.Summary imported=mSh.readPoints(mPl, testName2Gpx, mPl.getSize(), "gpx");      
      th.assertEquals(3, imported.adopted, "Gpx import failed",
              "Imported "+imported.adopted+" points from GPX");
      th.assertEqualsList(Arrays.asList("1","2","3","8"), mPl.getIndices(), "Wrong index list",
              "Point list Ok");
      next=mPl.getNextS();
      th.assertEquals("9", next, "Wrong NEXT="+next, "NEXT is Ok:"+next);
      th.assertTrue(null == mPl.getEdge(), "Non-empty EDGE", "EDGE is null");
      
      th.println("Testing repeated GPX import, expecting exception");
      String exceptionThrown="";
      try { U.Summary imported2=mSh.readPoints(mPl, testName2Gpx, mPl.getSize(), "gpx"); }
      catch (U.DataException e) { exceptionThrown=e.getMessage(); }
      th.assertContains("Set max point count",exceptionThrown,"No or wrong exception on too large GPX file",
          "Exception on too large GPX file");
      U.unlink(mPath, testName2Gpx);
      
      th.println("Testing partial CSV import");
      String testName3="test3_".concat(Point.getDate());
      String testName3Csv=testName3+"."+extCsv;
      U.Summary exported=mSh.writePoints(mPl, testName3Csv, 2, 3, "csv");
      th.assertEquals(2, exported.adopted, "Partial csv export failed",
              "Exported "+exported.adopted+" points as CSV");
      String csvReadback=U.fileGetContents(mPath, testName3Csv);
      U.unlink(mPath, testName3Csv);
      String[] lines=TextUtils.split(csvReadback, Point.NL);
      th.assertEquals(4, lines.length, "Wrong exported lines count="+lines.length,
              "Exported lines count Ok");
      th.print(mPl.getById(2).getComment()+" : ");
      th.csvLineDiff(lines[1], getTestCsv1(1), Point.SEP);
      th.print(mPl.getById(3).getComment()+" : ");
      th.csvLineDiff(lines[2], getTestCsv1(2), Point.SEP);
    }  
    
  }// end Tests1
  
  @Override
  protected android.support.v4.app.Fragment createFragment() { return new Tests1Fragment(); }

}
