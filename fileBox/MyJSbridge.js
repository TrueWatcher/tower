"use strict";

wm.fb.MyJSbridge=function(aProvider) {
  var _this=this,
      defaultZoom="16",
      zoom=defaultZoom,
      lat="123",
      lon="321",
      isBounded="*",
      markersJson="[]",
      viewTrackJson="[]",
      viewTrackNamesJson="[]",
      currentTrackJson='[]',
      provider=aProvider,
      isDirty=3;
  
  if ( ! aProvider) provider="wallpaper";   
      
  this.dHeightEm=0;
  this.dWidthEm=0;
  
  this.importLatLon=function() { return lat.concat(",").concat(lon); };
  
  this.importZoom=function() { return zoom; };
  this.importDefaultZoom=function() { return defaultZoom; };
  this.saveZoom=function(z) { zoom=z; };
  
  this.importMapType=function() { return provider; };
  this.setMapType=function(aPr) { provider=aPr; };
  
  this.exportCenterLatLon=function(a,o) { lat=""+a; lon=""+o; };
  
  this.getKey=function() {
    var yaKey="";// <<<--- Put the Yandex Maps key here
    if (provider.indexOf("yandex") === 0) {
      //if ( ! yaKey) ind.fail("No key found for Yandex");
      return yaKey;
    }  
    console.log("No key for "+provider);
  };
  
  this.getNamelessMarker=function() { return false; };
  
  this.getMarkers=function() { return markersJson; };
  this.addMarkers=function(json) {
    markersJson=wm.utils.joinJsonArrays(markersJson,json);
  };
  this.setMarkers=function(json) { markersJson=json; };
  
  this.importViewTrackLatLonJson=function() { return viewTrackJson; };
  this.addViewTrackLatLonJson=function(json) {
    viewTrackJson=wm.utils.joinJsonArrays(viewTrackJson,json);
  };
  this.setViewTrack=function(json) { viewTrackJson=json; };
  this.pushViewTrack=function(track) {
    if (track instanceof Array) track=JSON.stringify(track);
    if (track.indexOf(",") >= 0 && ! track.startsWith("[[[")) { throw new Error("Non-empty and non-3d-array argument"); }
    viewTrackJson=wm.utils.pushJsonArray(viewTrackJson,track);
  };
  
  this.pushViewTrackName=function(name) {
    viewTrackNamesJson=wm.utils.joinJsonArrays( viewTrackNamesJson, "[\"".concat(name).concat("\"]") );
  };
  this.importViewTrackNamesJson=function() { return viewTrackNamesJson; }
  this.setViewTrackNamesJson=function(json) { viewTrackNamesJson=json; }
  
  this.importCurrentTrackLatLonJson=function() { return currentTrackJson; };
  this.replaceCurrentTrackLatLonJson=function(json) { currentTrackJson=json; };
  
  this.importViewCurrentTrack=function() { return false; }
  this.importFollowCurrentTrack=function() { return false; }
  
  this.setBounded=function(str) { isBounded=str; };
  this.getIsBounded=function() { return isBounded; };
  
  this.onMoveend=false;
  
  this.getDirty=function() { return isDirty; };
  this.setDirty=function(level) { isDirty=Math.max(isDirty,level); };
  this.clearDirty=function(level) { isDirty=0; };
  
  this.hasNoCoords=function() { return provider == "wallpaper"; };
};

wm.fb.MyJSbridge.clone=function(obj) {
  if (typeof obj != "object") { return obj; }
  var to=new this();
  this.copy(obj,to);
  return to;
};
wm.fb.MyJSbridge.copy=function(obj, target) {
  if (typeof obj != "object") { target=obj; return; }
  if (typeof target != "object") throw new Error("Copying to non-object");
  if ( ! (target instanceof this)) throw new Error("Copying to non-JSbridge");
  target.setMapType(obj.importMapType());
  target.saveZoom(obj.importZoom());// number
  target.setBounded(obj.getIsBounded());// string
  var ll=obj.importLatLon().split(",");
  target.exportCenterLatLon(ll[0],ll[1]);// string
  //target.addViewTrackLatLonJson(obj.importViewTrackLatLonJson());// string
  target.setViewTrack(obj.importViewTrackLatLonJson());// string
  //target.addMarkers(obj.getMarkers());// string
  target.setMarkers(obj.getMarkers());// string
  target.onMoveend=obj.onMoveend;// *function
  target.setViewTrackNamesJson(obj.importViewTrackNamesJson());// string
  return;
};
