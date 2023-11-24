function getTestCsv1(i) {
  var NL="\n";
  var lines=[];
  lines[0]="id;type;comment;protect;lat;lon;alt;range;time;cellData;note;sym";
  lines[1]="1;cell;Москва;;55.75321578979492;37.62250518798828;;100000.0;2019-01-28 16:59;{\"type\":\"WCDMA\",\"MCC\":250,\"MNC\":99,\"LAC\":27678,\"CID\":18654};;";
  lines[2]="2;mark;Sh_E_br;true;55.767812811849424;37.80133621206915;;;2019-01-28 17:13;;Has_a_note!;";
  lines[3]="8;gps;G8;;56.023113333333335;37.12359166666667;202.3;7.8;2019-02-04 23:10;;;";
  if (i >= 0 && i < lines.length) return lines[i];
  var all=lines.join(NL)+NL;
  return all;
}
    
function getTestCsv2(i) {
  var lines=[];
  lines[0]="id;type;comment;protect;lat;lon;alt;range;time;cellData;note;sym";
  lines[1]="9;cell;СПб;true;59.93894958496094;30.31563568115234;;100000.0;2019-02-05 06:28;{\"type\":\"WCDMA\",\"MCC\":250,\"MNC\":99,\"LAC\":14782,\"CID\":15258};;";
  lines[2]="11;mark;Тверь_вкз;;56.83574380308231;35.8939038708013;;;2019-03-06 18:09;;;";
  lines[3]="10;mark;МосВок;;59.92962552552448;30.36255102990254;;;2019-02-05 06:32;;;";
  if (i >= 0 && i < lines.length) return lines[i];
  return "";
}
    
function getTestCsv3(i) {
  var NL="\n";
  var lines=[];
  lines[0]="id;type;comment;protect;lat;lon;alt;range;time;cellData;note;sym";
  lines[1]="1;cell;Cell275;;55.75551986694336;37.64715957641602;;470.6525573730469;2019-05-14 13:12;{\"type\":\"LTE\",\"MCC\":250,\"MNC\":99,\"LAC\":4077,\"CID\":197175046,\"PCI\":275};;";
  if (i >= 0 && i < lines.length) return lines[i];
  var all=lines.join(NL)+NL;
  return all;
}

function getTestTrackGpx() {
  var s="";
  s+="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>" +
          "<gpx version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"truewatcher.tower\" >";
  s+="<wpt lat=\"11\" lon=\"21\"><ele>123.45</ele><name>01</name><time>2019-12-09T00:01:02Z</time><cmt>first wpt</cmt></wpt>";
  s+="<trk><trkseg><trkpt lat=\"1\" lon=\"2\"><ele>123.45</ele><time>2019-12-09T00:01:02Z</time></trkpt>" +
          "<trkpt lat=\"3\" lon=\"4\"><ele>123.45</ele><time>2019-12-09T00:01:03Z</time></trkpt>" +
          "<trkpt lat=\"5\" lon=\"6\"><ele>123.45</ele><time>2019-12-09T00:01:04Z</time></trkpt>" +
          "</trkseg>";
  //s+="<wpt lat=\"1\" lon=\"2\"><ele>123.45</ele><time>2019-12-09T00:01:02Z</time></wpt>";
  s+="<trkseg><trkpt lat=\"7\" lon=\"8\"><ele>23.45</ele></trkpt>" +
          "<trkpt lat=\"9\" lon=\"10\"><ele>23.45</ele></trkpt>" +
          "<trkpt lat=\"-1.10\" lon=\"1.11\"><ele>23.45</ele></trkpt>" +
          "<trkpt lat=\"12\" lon=\"13\"><ele>23.45</ele></trkpt>" +
          "</trkseg></trk>";
  s+="<wpt lat=\"12\" lon=\"22\"><ele>123.45</ele><time>2019-12-09T00:01:02Z</time><extensions><type>gpx</type></extensions></wpt>";
  s+="</gpx>";
  return s;
}

function getTestTrackCsv() {
  var FIELDS = ["type","new_track","time","lat","lon","alt","range","name","data"];
  var SEP=";";
  var header = FIELDS.join(SEP);
  var s="";
  var NL="\n";
  s+=header+NL;
  s+="note;;2020-02-17 23:19:58;;;;;onStartCommand;intnt:true,flags:0,id:1"+NL;
  s+="T;1;2020-02-17 23:21:13;1;2;123.45;8.0;;"+NL;
  s+="T;;2020-02-17 23:21:13;3;4;123.45;8.0;;"+NL;
  s+="T;;2020-02-17 23:21:13;5;6;123.45;8.0;;"+NL;
  s+="note;;2020-02-18 00:22:55;;;;;onStartCommand;intnt:true,flags:0,id:1"+NL;
  s+="T;2;2020-02-17 23:21:13;7;8;23.45;8;;"+NL;
  s+="T;;2020-02-17 23:21:13;9;10;23.45;8.0;;"+NL;
  s+="note;;2020-02-18 00:22:55;;;;;onStartCommand;intnt:true,flags:0,id:1"+NL;
  s+="note;;2020-02-18 00:22:55;;;;;onStartCommand;intnt:true,flags:0,id:1"+NL;
  s+="T;;2020-02-17 23:21:13;-1.10;1.11;23.45;8.0;;"+NL;
  s+="T;;2020-02-17 23:21:13;12;13;23.45;8.0;;"+NL;
  return s;
}

var json1="[[1,2],[3,4],[5,6]]";
var json2="[[7,8],[9,10],[-1.10,1.11],[12,13]]";
var json3="[[[1,2],[3,4],[5,6]],[[7,8],[9,10],[-1.10,1.11],[12,13]]]";
//var json01='[[["1","2"],["3","4"],["5","6"]],[["7","8"],["9","10"],["-1.10","1.11"],["12","13"]]]';
