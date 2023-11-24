"use strict";
if ( ! wm.hasOwnProperty("fb")) wm.fb={};

wm.fb.Parser=function() {
  var type="",
      segMap=[],
      csv={ SEP:";", NL:"\n"},
      names={ LAT:"lat", LON:"lon", NEW_TRACK:"new_track", ID:"id", TYPE:"type", COMMENT:"comment", CELLDATA:"cellData", DATA:"data", DATA1:"data1" },
      header={};

  // @returns { trkPoints : [lat,lon][][], wayPoints : [lat,lon][], res : String };
  this.go=function(text) {
    segMap=[];
    header={};
    var data=false;
    type=deduceType(text);
    //console.log("typed as "+type);
    switch (type) {
      case "gpx":
        var domparser = new DOMParser();
        var doc = domparser.parseFromString(text, "text/xml");
        if (parsingFailed(doc)) {
          reportErr(doc);
          return;
        }
        data=getDataFromDom(doc);
        break;
      case "csv_wpt":
      case "csv_track":
        data=getDataFromCsv(text);
        break;
      default:
        throw new Error(type);
    }
    return data;
  };

  function deduceType(buf) {
    if ( ! buf) return "empty file";
    if (buf.includes("<gpx") && buf.includes("</gpx>")) return "gpx";
    if (buf.includes("<?xml")) return "unknown XML";
    var firstnl=buf.indexOf("\n");
    var firstline=buf.substring(0,firstnl-1);
    if (firstline.includes(names.LAT) && firstline.includes(names.LON)) {
      if (firstline.includes(names.NEW_TRACK)) return "csv_track";
      if (firstline.includes(names.ID)) return "csv_wpt";
      return "csv_track";// we are permissive
    }
    return "unknown format";
  }

  function parsingFailed(doc) {
    return ( doc.getElementsByTagName('parsererror').length > 0 );
  }

  function reportErr(doc) {
    var errors=doc.getElementsByTagName('parsererror');
    var report="";
    for (var i=0; i < errors.length; i+=1) { report += errors[i].innerHTML+"<br />"; }
    throw new Error(report);
  }

  // @returns { trkPoints : [lat,lon][][], wayPoints : [lat,lon][], res : String };
  function getDataFromDom(doc) {
    var seg,points=[],pCount,pt,tpCount=0,trkPoints=[],wayPoints=[],pointGroup,
        res=type;
    var trksegs=doc.getElementsByTagName("trkseg");
    var tsCount=trksegs.length;
    for (seg=0; seg < tsCount; seg+=1) {
      points=trksegs[seg].getElementsByTagName("trkpt");
      tpCount += points.length;
      pointGroup=parseTrkPointGroup(points);
      trkPoints.push(pointGroup);
      segMap.push(pointGroup.length);
    }
    res+=" , found "+tpCount+" trackpoints";
    if (tpCount) {
      var sum = wm.utils.sumUp(segMap);
      if (sum != tpCount) throw new Error("Processed "+tpCount+" points, segments give "+sum);
      res += "("+tsCount+" segments:"+segMap.join(" ")+")";
    }
    points=doc.getElementsByTagName("wpt");
    pointGroup=parseWayPointGroup(points);
    wayPoints=pointGroup;
    res+=" and "+points.length+" waypoints";
    return { trkPoints : trkPoints, wayPoints : wayPoints, res : res };
  }

  // @returns [ [lat,lon], ... ]
  function parseTrkPointGroup(points) {
    var pt,trkpt,arr=[],lat,lon,
        pCount=points.length;;
    for (pt=0; pt < pCount; pt+=1) {
      trkpt=points[pt];
      lat=trkpt.getAttribute("lat");
      lon=trkpt.getAttribute("lon");
      if (lat && lon) { arr.push( [lat, lon] ); }
    }
    return arr;
  }

  // @returns [ [lat,lon], ... ]
  function parseWayPointGroup(points) {
    var pt,wpt,arr=[],lat,lon,type,text,s,cmts,cmt,t,ext,te,
        pCount=points.length;

    for (pt=0; pt < pCount; pt+=1) {
      wpt=points[pt];
      lat=wpt.getAttribute("lat");
      lon=wpt.getAttribute("lon");
      s=wpt.getElementsByTagName("name");
      if ( ! lat || ! lon || ! s) continue;

      text="";
      cmt="";
      if (s.length) text=s[0].innerHTML;
      cmts=wpt.getElementsByTagName("cmt");
      if (cmts.length) {
        cmt=cmts[0].innerHTML;
        if (cmt) text += "."+cmt;
      }
      t=wpt.getAttribute("type");
      ext=wpt.getElementsByTagName("extensions");
      if (ext && ext.length) te=ext[0].getElementsByTagName("type");
      if (te && te.length) te=te[0].innerHTML;
      //console.log("type:"+te+"/"+t);
      type = te || t || "mark";
      if (lat && lon) { arr.push( [type, lat, lon, text] ); }
    }
    return arr;
  }

  // @returns { trkPoints : [lat,lon][][], wayPoints : [lat,lon][], res : String };
  function getDataFromCsv(text) {
    var lines=splitCsv(text);
    var r=readCsvLines(lines);
    return r;
  }

  // @returns String[]
  function splitCsv(text) {
    if ( ! text) throw new Error("Empty TEXT");
    var lines=text.split(csv.NL);
    if (lines.length < 2) throw new Error("Too few lines, is NL \\n?");
    var headline=lines[0].trim();
    header=parseCsvHeader(headline);
    return lines;
  }

  // @returns { ::: }
  function parseCsvHeader(headline) {
    var i,
        found={},
        fields=headline.split(csv.SEP);

    //console.log("header fields:"+wm.utils.dumpArray(fields));
    if (fields.length < 2) throw new Error('Too few fields, SEP must be "'+names.SEP+'"');
    for (i in names) {
      if ( ! names.hasOwnProperty(i)) continue;
      found[i]=fields.indexOf(names[i]);
    }
    //console.log(wm.utils.dumpArray(found));
    if (found.LAT < 0 || found.LON < 0) throw new Error("Missing "+names.LAT+" or "+names.LON+" from the header");
    if (type == "csv_wpt" && found.ID < 0) throw new Error("Missing "+names.ID+" from the header");
    found.count=fields.length;
    found.fields=fields;
    return found;
  }

  // @returns { trkPoints : [lat,lon][][], wayPoints : [lat,lon][], res : String };
  function readCsvLines(lines) {
    var i,processedCount=0,line,parts,lat,lon,newTrack,arr=[],segPositions=[],res,entry,makeEntry,extras=[],pointData,ret,
    getPointData=function() { return false; }, onExtraData=function(x) { return x; };

    if (type == "csv_wpt") { makeEntry=makeMarker; }
    else { makeEntry=makeTrackLatLon; getPointData=searchSignalData; onExtraData=packSignalData; }

    for (i=1; i<lines.length; i+=1) {
      line=lines[i].trim();
      if ( ! line) continue;
      parts=line.split(csv.SEP);
      //console.log(wm.utils.dumpArray(parts));
      if (parts.length != header.count) throw new Error("Line "+i+": "+parts.length+" fields instead of "+header.count);
      lat=parts[header.LAT];
      lon=parts[header.LON];
      if ( ! lat || ! lon) continue;
      entry=makeEntry(lat,lon,parts);
      arr.push(entry);
      pointData=getPointData(parts);
      if (pointData) extras.push(pointData);
      processedCount+=1;
    }
    res={total:lines.length, processed:processedCount, segments:1, segMap:[]};

    if (type == "csv_wpt") {
      res = type+" , "+res.total+" lines, found "+res.processed+" waypoints";
      ret = { trkPoints : [], wayPoints : arr, res : res };
    }
    else {
      //console.log(wm.utils.dumpArray(segPositions));
      var segmented=cutToSegments(arr,segPositions,res);
      //console.log(wm.utils.dumpArray(segPositions));
      var sum = wm.utils.sumUp(segMap);
      if (sum != res.processed) throw new Error("Processed "+res.processed+" points, segments give "+sum);
      res = type+" , "+res.total+" lines, found "+res.processed+" trackpoints ("+res.segments+" segments: "+res.segMap.join(" ")+")";
      ret = { trkPoints : segmented, wayPoints : [], res : res };
    }
    console.log("Parsed lengths:"+extras.length+"/"+arr.length);
    if (extras.length == arr.length) ret = onExtraData(ret, extras);
    return ret;

    function makeTrackLatLon(lat,lon,parts) {
      if (header.NEW_TRACK >= 0) {
        if (parts[header.NEW_TRACK]) segPositions.push(processedCount);
      }
      return [lat,lon];
    }

    function makeMarker(lat,lon,parts) {
      var wtype="mark";
      if (header.TYPE >= 0 && parts[header.TYPE]) wtype=parts[header.TYPE];
      var text=parts[header.ID];
      if (header.COMMENT >= 0 && parts[header.COMMENT]) text += "."+parts[header.COMMENT];
      return [wtype, lat, lon, text];
    }

    function searchSignalData(parts) {
      if (parts[header.COMMENT] != 'S') return false;
      if (parts[header.DATA] == "") return false;
      //alert(parts[header.ID]+": "+parts[header.DATA]+", "+parts[header.DATA1]);
      return [parts[header.DATA], parts[header.DATA1]];
    }

    function packSignalData(ret,extras) {
      var i=0, prevCell='', extra, colors=[], breaks=[];
      for (; i<extras.length; i+=1) {
        extra=extras[i]; // [ dBm, cell ]
        colors.push( normalize(extra[0]) );
        if (extra[1] != prevCell) {
          prevCell=extra[1];
          breaks.push(extra[1]);
        }
        else { breaks.push(""); }
      }
      ret.colors=colors;
      ret.breaks=breaks;
      return ret;
    }

    function normalize(x) {
      var low=-120, high=-50, y;
      x = parseInt(x);
      if (x <= low) return 0;
      if (x == 0) return 0;
      if (x >= high) return 1;
      y = (x-low)/(high-low);
      //alert(x+" > "+y);
      return y;
    }
  }

  // @returns [lat,lon][][]
  function cutToSegments(arr,segPositions,res) {
    var segCount=1,i,slice,segmented=[];
    if (segPositions.length == 0) {
      console.log("no new_track marks");
      res.segMap=segMap=[arr.length];
      return [ arr ];
    }
    if (segPositions[0] != 0) {
      console.log("added missing 0th new_track mark");
      segPositions.unshift(0);
    }
    segPositions.push(arr.length);
    segCount=segPositions.length-1;
    res.segments=segCount;

    for (i=0; i < segCount; i+=1) {
      slice=arr.slice( segPositions[i], segPositions[i+1] );
      segmented.push(slice);
      segMap.push(slice.length);
    }
    res.segMap=segMap;
    return segmented;
  }

  this.getSegMap=function() { return segMap; };
  this.getType=function() { return type; };
  this.getHeader=function() { return header; };

};// end Parser
