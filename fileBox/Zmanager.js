"use strict";
if ( ! wm.hasOwnProperty("fb")) wm.fb={};

wm.fb.Zmanager=function() {
  var _this = this,
      defaultParams = { dataField: "dBm", scale: "auto", low: -120, high: -85, lowYetReal:-150, colorScheme: 'orange', cellField: 'CID' },// scale: "static"
      params = Object.assign({}, defaultParams),
      colorSchemes = { orange: wm.utils.linearRGB2, redTrans: wm.utils.linearTranslucent, poly7: poly7 },
      cellList = [ 0 ];

  this.searchCsvParts = function(parts, header) {
    if (parts[header.COMMENT] != 'S') return false;
    if (parts[header.DATA] == "") return false;
    if (parts[header.DATA1] == "") return false;
    //alert(parts[header.ID]+": "+parts[header.DATA]+", "+parts[header.DATA1]);
    return [parts[header.DATA], parts[header.DATA1]];
  };

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
      if (extra[1] != prevCell) {
        prevCell=extra[1];
        breaks.push(extra[1]);
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
      signal = getSignalValue(extras[i][0]);
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
      if (params.scale == "cell") { colors.push( makeCellColor(extras[i][1]) ); }
      else { colors.push( makeSignalColor(extras[i][0], bounds) ); }
    }
    return colors;
  }

  function makeSignalColor(dataField, bounds) {
    var asInt = getSignalValue(dataField, params.dataField);
    var as01 = normalize(asInt, bounds);
    var asRgb = colorSchemes[params.colorScheme] (as01);
    return asRgb;
  }

  function makeCellColor(cellField) {
    var asInt = getSignalValue(cellField, params.cellField);
    var asIndex = indexCells(asInt);
    console.log("Cells by "+params.cellField+":"+asInt+" ("+cellList.length+")");
    var asRgb = poly7(asIndex);
    return asRgb;
  }

  function getSignalValue(dataField, targetFieldName) {
    var asInt, asJson;
    if (! targetFieldName) targetFieldName = params.dataField;
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
    var found;
    if (! id) return false;
    found = cellList.indexOf(id);
    if (found >= 0) return found;
    cellList.push(id);
    return cellList.indexOf(id);
  }

  function resetCellList() { cellList = [ 0 ]; }

  this.setZscale=function(optStr) {
    var parts = optStr.split(' ');
    if (parts[0] == "auto") {
      params.scale = "auto";
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
    }
    else if (parts[0] == "cell") {
      params.scale = "cell";
      params.cellField = parts[1];
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
