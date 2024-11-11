"use strict";
if ( ! wm.hasOwnProperty("fb")) wm.fb={};

wm.fb.Zmanager=function() {
  var _this = this,
      defaultParams = { dataField: "dBm", scale: "auto", low: -120, high: -85, lowYetReal:-150, colorScheme: 'orange', cellField: 'CID' },// scale: "static"
      params = Object.assign({}, defaultParams),
      makeColorFn = makeSignalColor,
      colorSchemes = { orange: wm.utils.linearRGB2, redTrans: wm.utils.linearTranslucent, poly7: poly7 },
      cellList = [ 0 ];
  const SIGNAL=0, CELL=1;

  this.searchCsvParts = function(parts, header) {
    if (parts[header.COMMENT] != 'S') return false;
    if (parts[header.DATA] == "") return false;
    if (parts[header.DATA1] == "") return false;
    //alert(parts[header.ID]+": "+parts[header.DATA]+", "+parts[header.DATA1]);
    return [parts[header.DATA], parts[header.DATA1]];
  };
  
  this.addCellEnb = function(pointData, parts, header) {
    const ENB = "ENB_ID";
    var data1 = pointData[CELL];
    var fromJson = JSON.parse(data1);
    //alert(typeof asJson);
    if (! (typeof fromJson == "object")) {
      console.log("failed to parse json");
      return pointData, false;
    }
    if (fromJson.hasOwnProperty(ENB) || ! fromJson.CID) {
      return pointData, false;
    }
    var enb = 0;
    if (fromJson.type == "LTE") enb = fromJson.CID >> 8;
    fromJson[ENB] = enb;
    var reJson = JSON.stringify(fromJson);
    pointData[CELL] = reJson;
    parts[header.DATA1] = reJson;
    return pointData, parts;
  }

  this.packSignalData=function(ret,extras) {
    var colors=[], breaks=[], bounds={};
    breaks = findHandovers(extras);
    bounds = detectBounds(extras);
    colors = makeColors(extras, bounds);
    ret.colors=colors;
    ret.breaks=breaks;
    return ret;
  };

  function findHandovers(extras) {
    var i, prevCell='', extra, breaks=[];
    for (i=0; i<extras.length; i+=1) {
      extra=extras[i]; // [ dBm, cell ]
      if (extra[CELL] != prevCell) {
        prevCell=extra[CELL];
        breaks.push(extra[CELL]);
      }
      else { breaks.push(""); }
    }
    return breaks;
  }

  function detectBounds(extras) {
    var i, low=+1E+20, high=-1E+20, signal=false;
    var statiic = { low: params.low, high: params.high };
    var failSafe = { low: defaultParams.low, high: defaultParams.high };
    if (params.scale == "static" || params.scale == "cell" ) return statiic;
    for (i=0; i<extras.length; i+=1) {
      signal = getFieldValue(extras[i][SIGNAL], params.dataField);
      if (signal === false) continue;
      if (signal < params.lowYetReal) continue; // allows -999 instead of false
      if (signal < low) low = signal;
      if (signal > high) high = signal;
    }
    if (low > high || low == high) {// found 0 or 1 points
      alert("Failed to autoscale Z-data");
      return failSafe;
    }
    console.log("auto scaled to "+low+".."+high);
    return { low: low, high: high };
  }

  function makeColors(extras, bounds) {
    var i, colors = [];
    for (i=0; i<extras.length; i+=1) {
      colors.push( makeColorFn (extras[i], bounds) );
    }
    return colors;
  }

  function makeSignalColor(extra, bounds) {
    var signalAsInt = getFieldValue(extra[SIGNAL], params.dataField);
    var as01 = normalize(signalAsInt, bounds);
    var asRgb = colorSchemes[params.colorScheme] (as01);
    return asRgb;
  }

  function makeCellColor(extra) {
    var cellAsInt = getFieldValue(extra[CELL], params.cellField);
    var asIndex = indexCells(cellAsInt);
    var asRgb = poly7(asIndex);
    return asRgb;
  }

  function getFieldValue(dataField, targetFieldName) {
    var asInt, asJson;
    if (! targetFieldName) throw new Error("Empty targetFieldName");
    asJson = JSON.parse(dataField);
    //alert(typeof asJson);
    if (! (typeof asJson == "object")) {
      console.log("failed to parse json");
      return false;
    }
    if ( ! asJson.hasOwnProperty(targetFieldName)) {
      console.log("failed to find Z data "+targetFieldName+" in json");
      return false;
    }
    asInt = parseInt(asJson[targetFieldName]);
    if (asInt != asInt || (typeof asInt) == "undefined") {
      console.log("failed to get Z data "+targetFieldName+" from json");
      return false;
    }
    return asInt;
  }

  function normalize(x, bounds) {
    var low=-120, high=-50, y;
    if (bounds && bounds.hasOwnProperty(high) && bounds.hasOwnProperty(low)) {
      low = bounds.low; high = bounds.high;
    }
    x = parseInt(x);
    if (x != x || (typeof x) == "undefined") return false;
    if (x <= low) return 0;
    if (x == 0) return 0;
    if (x >= high) return 1;
    y = (x-low)/(high-low);
    //alert(x+" > "+y);
    return y;
  }

  function indexCells(id) {
    if (! id) return false;
    var found = cellList.indexOf(id);
    if (found >= 0) return found;
    cellList.push(id);
    console.log("Cells by "+params.cellField+": "+id+" ("+cellList.length+")");
    return cellList.indexOf(id);
  }

  function resetCellList() { cellList = [ 0 ]; }

  this.setZscale=function(optStr) {
    var parts = optStr.split(' ');
    if (parts[0] == "auto") {
      params.scale = "auto";
      makeColorFn = makeSignalColor;
      return;
    }
    else if (parts[0] == "static") {
      if (parts.length < 3) throw new Error("Missing LOW or HIGH");
      var low = parseInt(parts[1]);
      var high = parseInt(parts[2]);
      if (low != low || high != high) throw new Error("Invalid LOW or HIGH");
      params.scale = "static";
      params.low = low;
      params.high = high;
      makeColorFn = makeSignalColor;
    }
    else if (parts[0] == "cell") {
      params.scale = "cell";
      params.cellField = parts[1];
      makeColorFn = makeCellColor;
      resetCellList();
    }
    else throw new Error("Wrong SCALE");
    //console.log("params set to "+wm.utils.dumpArray(params));
  };

  this.updateSignalColors=function(extras) {
    var colors=[], bounds={};
    if (! extras || extras.length == 0) {
      console.log("Empty EXTRAS");
      return [];
    }
    bounds = detectBounds(extras);
    colors = makeColors(extras, bounds);
    return colors;
  };

  function poly7(n) {
    var colors = [
      "rgba(11,132,165,1)","rgba(246,200,95,1)","rgba(111,78,124,1)",
      "rgba(157,216,102,1)","rgba(202,71,47,1)","rgba(255,160,86,1)","rgba(141,221,208,1)" ],
        black = "rgba(0,0,0,1)",
        count = colors.length;

    if (! n) return black;
    return colors[ n % count ];
  }

};
