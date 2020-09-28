"use strict";
if ( ! wm) var wm={};
wm.utils={};

wm.utils.getScreenParams=function() {
  var emPx = parseFloat(getComputedStyle(document.documentElement).fontSize);
  var isMobile = null;
  if (typeof window.matchMedia == "function") isMobile = window.matchMedia("only screen and (max-width: 760px)");
  var width = screen.width || window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
  var height = screen.height || window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
  var isPortrait=(width < height);
  return { isPortrait:isPortrait, width:width, height:height, isMobile : isMobile, emPx : emPx };
};

wm.utils.getViewportParams=function() {
  var emPx = parseFloat(getComputedStyle(document.documentElement).fontSize);
  var isMobile = null;
  if (typeof window.matchMedia == "function") isMobile = window.matchMedia("only screen and (max-width: 760px)");
  var width = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
  var height = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
  var isPortrait=(width < height);
  return { isPortrait:isPortrait, width:width, height:height, isMobile : isMobile, emPx : emPx };
};

wm.utils.getScreenParams2=function() {
  var emPx = parseFloat(getComputedStyle(document.documentElement).fontSize);
  var isMobile = null;
  if (typeof window.matchMedia == "function") isMobile = window.matchMedia("only screen and (max-width: 760px)");
  // https://software.intel.com/en-us/html5/hub/blogs/how-to-get-the-correct-android-screen-dimensions
  var width = screen.height;
  var height = screen.width;
  var screenRatio;
  var realWidth;
  var realHeight;
  if (width > height) { realWidth=width; realHeight=height; screenRatio=(height/width); }
  else { realWidth=height; realHeight=width; screenRatio=(width/height); }
  if (isNaN(screenRatio)) {
    if (window.innerHeight > window.innerWidth) {
      realWidth=window.innerHeight; realHeight=window.innerWidth; screenRatio = (window.innerWidth/window.innerHeight);
    }
    else { 
      realWidth=window.innerWidth; realHeight=window.innerHeight; screenRatio = (window.innerHeight/window.innerWidth);
    }
  }
  var isPortrait=(width < height);
  return { isPortrait:isPortrait, width:realWidth, height:realHeight, isMobile : isMobile, emPx : emPx };
};

wm.utils.Ind=function(el) {
  this.show=function() {
    el.style.display="";
  };
  this.fail=function(msg) {
    this.show();
    el.innerHTML="Error! ".concat(msg);
    throw new Error(msg);
  };
  this.say=function(msg) {
    this.show();
    var s=el.innerHTML;
    if (s) s+=", ";
    el.innerHTML=s.concat(msg);
  };
  this.hide=function() {
    el.style.display="none";
  };
  this.init=function() {
    el.innerHTML="";
    this.show();
  };
  this.show();
};

wm.utils.MockJSbridge=function(provider,ind) {
  var _this=this;
  var _zoom="16";
  var _lat="55.79038894644";
  var _lon="37.7759262544";
  var _isBounded="";
  this.importLatLon=function() { return _lat.concat(",").concat(_lon); };
  this.importZoom=function() { return _zoom; };
  this.importMapType=function() { return provider; };
  this.exportCenterLatLon=function(a,o) { 
    ind.init(); ind.say( ">>>"+a+","+o );
  };
  this.saveZoom=function(z) { _zoom=z; };
  this.getKey=function() {
    var yaKey="";// <<<--- Put the Yandex Maps key here
    if (provider.indexOf("yandex") === 0) {
      //if ( ! yaKey) ind.fail("No key found for Yandex");
      return yaKey;
    }  
    ind.fail("No key for "+provider);
  };
  this.getNamelessMarker=function() { return '["gps","'+_lat+'","'+_lon+'"]'; };
  this.getMarkers=function() { return '[["mark","'+(parseFloat(_lat)+1e-3)+'","'+_lon+'","upper_mark"]]'; };
  this.importViewTrackLatLonJson=function() { return '[\
    [[55.7904,37.7760],[55.791,37.777],[55.792,37.778],[55.793,37.779]],\
    [[55.791,37.778],[55.792,37.779],[55.793,37.780]]\
  ]'; };
  this.importCurrentTrackLatLonJson=function() { return '[\
    [[55.7906,37.7760],[55.7912,37.777],[55.7922,37.778],[55.7932,37.779]],\
    [[55.7912,37.778],[55.7922,37.779],[55.7932,37.780]]\
  ]'; };
  this.importViewCurrentTrack=function() { return true; };
  this.importFollowCurrentTrack=function() { return true; };
  this.setBounded=function(str) { _isBounded=str; };
  this.getIsBounded=function() { return _isBounded; };
  this.moveLatLon=function() {
    _lat=(parseFloat(_lat)-0.001).toString();
    _lon=(parseFloat(_lon)-0.001).toString();
  };
  this.setupEventThrower=function() {
    document.onkeyup=function(e) { 
      console.log("keyup:"+e.code);
      if (e.code != "Space") return false;
      _this.moveLatLon();
      (function() { window.dispatchEvent(onTrackreloadEvent); }) ();// onDatareloadEvent onTrackreloadEvent
      return false;
    }    
  };
};


wm.utils.putMarkers=function(oMap, ind, markersJson, putOneMarker) {
  var markersArr=[];
  var m=[];
  var i,l;
  if ( ! markersJson || markersJson.length < 3 ) return;
  try {
    markersArr=JSON.parse(markersJson);
    l=markersArr.length;
  }
  catch (e) {
    ind.fail("Unparsable markersJson");
  }
  for (i=0; i < l; i+=1) {
    m=markersArr[i];
    putOneMarker(oMap,m);
  }  
};

wm.utils.LimitFinder=function() {
  
  this.go=function(JSbridge) {
    var mode = JSbridge.getIsBounded();
    if ( ! mode || mode == "") return false;
    var currentTracklimits=false, viewTracklimits=false, waypointlimits=false;
    if (mode == "*" || mode == "t" || mode == "ct") currentTracklimits=findTrackLimits(JSbridge.importCurrentTrackLatLonJson());
    if (mode == "*" || mode == "t" || mode == "vt") viewTracklimits=findTrackLimits(JSbridge.importViewTrackLatLonJson());
    if (mode == "*" || mode == "w") waypointlimits=findWaypointLimits(JSbridge.getMarkers());
    var limits = mergeLimits( [ currentTracklimits, viewTracklimits, waypointlimits ] );
    if ( ! limits) return false;
    //console.log(wm.utils.dumpArray(limits));
    console.log(JSON.stringify(limits));
    return limits;    
  }
  
  function findTrackLimits(trackJson) {
    var nseg, np;
    var res = new wm.utils.Limits(); 
    var track=JSON.parse(trackJson);
    if ( ! track || ! (track instanceof Array) || track.length == 0 ) return false;
    if (! (track[0] instanceof Array) || ! (track[0][0] instanceof Array) ) throw new Error("Malformed track");
    var segCount=track.length;
    for (nseg=0; nseg < segCount; nseg+=1) {
      for (np=0; np < track[nseg].length; np+=1) {
        //console.log(nseg+"/"+np);
        res.addLatLon(track[nseg][np]);
      }
    }
    return res;  
  }

  function findWaypointLimits(markersJson) {
    var res = new wm.utils.Limits();
    var markersArr=[];
    var latLon=[];
    var i,l;
    if ( ! markersJson || markersJson.length < 3 ) return;
    try {
      //console.log(markersJson);
      markersArr=JSON.parse(markersJson);
      l=markersArr.length;
    }
    catch (e) {
      ind.fail("Unparsable markersJson");
    }
    for (i=0; i < l; i+=1) {
      //console.log(wm.utils.dumpArray(markersArr[i]));
      latLon = [ markersArr[i][1], markersArr[i][2] ];
      res.addLatLon(latLon);
    } 
    //console.log(wm.utils.dumpArray(res));
    return res;  
  }

  function mergeLimits(limArr) {
    var res = new wm.utils.Limits();
    var emptyCount=0;
    for (var i=0; i < limArr.length; i+=1) {
      if ( ! limArr[i] ) { 
        emptyCount+=1;
        continue;
      }
      res.addLimits(limArr[i]);
    }
    if (emptyCount == limArr.length) return false;
    return res;
  }
}; // end LimitFinder

wm.utils.Limits=function() {
  this.minLat=999;
  this.minLon=999;
  this.maxLat=-999;
  this.maxLon=-999;
  
  this.addLatLon=function(latLon) {
    if (latLon === false) return this;
    if ( ! (latLon instanceof Array) || latLon.length != 2) throw new Error("Wrong latLon="+latLon+"!");
    var lat=parseFloat(latLon[0]);
    var lon=parseFloat(latLon[1]);
    if (lat != lat) throw new Error("LAT is NaN");
    if (lon != lon) throw new Error("LON is NaN");
    this.minLat=Math.min(lat,this.minLat);
    this.minLon=Math.min(lon,this.minLon);
    this.maxLat=Math.max(lat,this.maxLat);
    this.maxLon=Math.max(lon,this.maxLon);
    return this;
  };

  this.addLimits=function(limits) {
    if ( ! limits) return this;
    if ( ! (limits instanceof wm.utils.Limits) ) throw new Error("Wrong limits");
    this.addLatLon( [limits.minLat, limits.minLon] )
        .addLatLon( [limits.maxLat, limits.maxLon] );
    return this;
  };  
};

wm.utils.dumpArray=function(x) {
  var res="",i,expanded;
  if (typeof x == "object") {
    res+="{ ";
    for (i in x) {
      if (x.hasOwnProperty(i)) {
        res+=" "+i+":"+wm.utils.dumpArray(x[i]);
      }  
    }
    res+=" }";
  }
  else res+=""+x;
  return res;  
};

wm.utils.joinJsonArrays=function(a1, a2) {
  var empty="[]";
  var l1=a1.length;
  var l2=a2.length;
  if ( ! (a1.startsWith("[") && a1.endsWith("]")) ) throw new Error("Wrong A1="+a1);
  if ( ! (a2.startsWith("[") && a2.endsWith("]")) ) throw new Error("Wrong A2="+a2);
  if (a1 == empty) return a2;
  if (a2 == empty) return a1;
  var res=a1.substring(0, l1-1).concat(",").concat(a2.substring(1,l2));
  return res;
};

wm.utils.findJSbridge=function (ind, mockMapType) {
  if ( ! window.hasOwnProperty("JSbridge")) {
    if (window.parent && window.parent.JSbridge) {// we are in an iframe
      //console.log("using window.parent.JSbridge");
      window.JSbridge=window.parent.JSbridge;
    }
    else { // we are in a browser
      ind.say("mock coords");
      //console.log("using wm.utils.MockJSbridge");
      window.JSbridge=new wm.utils.MockJSbridge(mockMapType,ind);//"google hyb"//"osm map"//"google sat"
      JSbridge.moveLatLon();
    }
  }
};

wm.utils.adjustScreen=function() {
  //alert(scrInfo.width+"/"+scrInfo.height);
  var mapDiv=document.getElementById("mapDiv");
  var dWidthEm=0, dHeightEm=0;
  if (JSbridge.hasOwnProperty("dWidthEm")) dWidthEm=JSbridge.dWidthEm;
  if (JSbridge.hasOwnProperty("dHeightEm")) dHeightEm=JSbridge.dHeightEm;
  
  var w=""+Math.min(scrInfo.width-dWidthEm*scrInfo.emPx, 2000);// no exact limit known
  var h=""+Math.min(scrInfo.height-dHeightEm*scrInfo.emPx, 2000);
  //alert(w+"/"+h);
  mapDiv.style.width=w+"px";
  mapDiv.style.height=h+"px";
};

wm.utils.sumUp=function(arr) { return arr.reduce(function(a, b) { return a + b; }, 0); };
// https://stackoverflow.com/questions/1230233/how-to-find-the-sum-of-an-array-of-numbers



