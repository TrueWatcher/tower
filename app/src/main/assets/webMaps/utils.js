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
  var _zoom="16";
  var _lat="55.79038894644";
  var _lon="37.7759262544";
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
      if ( ! yaKey) ind.fail("No key found for Yandex");
      return yaKey;
    }  
    ind.fail("No key for "+provider);
  };
  this.getNamelessMarker=function() { return '["gps","'+_lat+'","'+_lon+'"]'; };
  this.getMarkers=function() { return '[["mark","'+(parseFloat(_lat)+1e-3)+'","'+_lon+'","upper_mark"]]'; };
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