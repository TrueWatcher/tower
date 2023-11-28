"use strict";
if ( ! wm.hasOwnProperty("fb")) wm.fb={};

wm.fb.Zmanager=function() {
  var _this = this,
      params = { dataField: "dBm", scale: "", low: -120, high: -85, lowYetReal:-140, colorScheme: 'orange' },// scale: "static"
      colorSchemes = { orange: wm.utils.linearRGB2, redTrans: wm.utils.linearTranslucent };

  this.searchCsvParts = function(parts, header) {
    if (parts[header.COMMENT] != 'S') return false;
    if (parts[header.DATA] == "") return false;
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
    var defa = { low: params.low, high: params.high };
    if (params.scale == "static") return defa;
    for (i=0; i<extras.length; i+=1) {
      signal = getSignalValue(extras[i][0]);
      if (signal === false) continue;
      if (signal < params.lowYetReal) continue; // allows -999 instead of false
      if (signal < low) low = signal;
      if (signal > high) high = signal;
    }
    if (low > high || low == high) {
      alert("Failed to autoscale Z-data");
      return defa;
    }
    console.log("auto scaled to "+low+".."+high);
    return { low: low, high: high };
  }

  function makeColors(extras, bounds) {
    var i, colors = [];
    for (i=0; i<extras.length; i+=1) {
      colors.push( makeSignalColor(extras[i][0], bounds) );
    }
    return colors;
  }

  function makeSignalColor(dataField, bounds) {
    var asInt = getSignalValue(dataField);
    var as01 = normalize(asInt, bounds);
    var asRgb = colorSchemes[params.colorScheme] (as01);
    return asRgb;
  }

  function getSignalValue(dataField) {
    var asInt = parseInt(dataField);
    if (asInt != asInt || (typeof asInt) == "undefined") {
      var asJson = JSON.parse(dataField);
      //alert(typeof asJson);
      if (! (typeof asJson == "object")) {
        console.log("failed to parse json");
        return false;
      }
      var targetFieldName = params.dataField;
      if ( ! asJson.hasOwnProperty(targetFieldName)) {
        console.log("failed to find Z data "+targetFieldName+" in json");
        return false;
      }
      asInt = parseInt(asJson[targetFieldName]);
      if (asInt != asInt || (typeof asInt) == "undefined") {
        console.log("failed to get Z data "+targetFieldName+" from json");
        return false;
      }
    }
    return asInt;
  }

  function normalize(x, bounds) {
    var low=-120, high=-50, y;
    if (bounds && bounds?.high && bounds?.low) {
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



















}
