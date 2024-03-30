"use strict";
if ( ! wm.hasOwnProperty("fb")) wm.fb={};

function $(id) { return document.getElementById(id); }

wm.fb.View=function() {
  var panelTable=$('panelTable'),
      wmIframe=$('wmIframe'),
      alertTd=$('alertTd'),
      fileInput=$('fileInput'),
      backBtn=$('backBtn'),
      resizeInput=$('resizeInput'),
      providerSelect=$('providerSelect'),
      getDataBtn=$('getDataBtn'),
      zscaleSelect=$('zscaleSelect'),
      renderedLevel=0,
      _this=this;

  var emPx = parseFloat(getComputedStyle(document.documentElement).fontSize);
  var viewportHeight = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;

  this.adjust=function() {
    var panelHeight=panelTable.clientHeight;
    var remainder=viewportHeight-panelHeight-0.5*emPx;
    //console.log(panelHeight+"+"+remainder);
    wmIframe.style.height=remainder+"px";
    alertTd.innerHTML="Choose a GPX or CSV file";
  };

  this.setHandlers=function(doAdd, doReloadMap, doBack) {
    fileInput.onchange=doAdd;
    providerSelect.onchange=doReloadMap;
    backBtn.onclick=doBack;
  };

  this.setHandlers2=function(showDataForCenter, updateSignalTrackColors) {
    getDataBtn.onclick=showDataForCenter;
    zscaleSelect.onchange=updateSignalTrackColors;
  };

  this.loadUri=function(uri) {
    wmIframe.src=uri;
    // running from local files, Firefox >= 68 require privacy.file_unique_origin be set to false
  };

  this.getProvider=function() {
    return providerSelect.options[providerSelect.selectedIndex].value;
  };

  this.getFile=function() { return fileInput.files[0]; };

  this.getFiles=function() { return fileInput.files; };

  this.alert=function(str) { alertTd.innerHTML=str; };

  this.getShouldResize=function() { return resizeInput.checked; };

  this.getZscale=function() {
    return zscaleSelect.options[zscaleSelect.selectedIndex].value;
  };

  this.disableBackBtn=function() { backBtn.disabled=true; };
  this.enableBackBtn=function() { backBtn.disabled=false; };

  this.enableZcontrols=function() { zscaleSelect.style.display=''; };

  this.render=function(dataForMap) {
    if ( ! (dataForMap instanceof wm.fb.MyJSbridge)) throw new Error("Rendering from non-JSbridge");
    var provider=dataForMap.importMapType();
    var mapFrame=window.frames[0];
    var dirty=dataForMap.getDirty();
    switch (dirty) {
    case 0:
      console.log("doing nothing");
      break;

    case 1:
      // not tested in this environment
      console.log("firing onTrackreloadEvent");
      mapFrame.dispatchEvent(mapFrame.onTrackreloadEvent);
      break;

    case 2:
      //JSbridge=dataForMap; // does not work without reloading !
      console.log("firing onDatareloadEvent");
      mapFrame.dispatchEvent(mapFrame.onDatareloadEvent);
      break;

    case 3:
      JSbridge=dataForMap;
      console.log("loading map:"+provider);
      this.loadUri(getUri(provider));
      break;

    default:
      throw new Error("Wrong level="+dirty);
    }
    renderedLevel=dataForMap.getDirty();
    dataForMap.clearDirty();
  };

  function getUri(aProvider) {
    var isLeaflet=["osm","opentopo","google","marshruty.ru","blank"];
    var isYandex=["yandex"];
    var pr=aProvider.split(" ")[0];
    if (isLeaflet.includes(pr)) return "webMaps/leafletjs.html";
    if (isYandex.includes(pr)) return "webMaps/ya.html";
    if (pr == "wallpaper") return "webMaps/wallpaper.html";
    throw new Error("Unknown provider="+pr);
  }

  this.getRenderedLevel=function() { return renderedLevel; };

  this.showCoords=function() {
    var latLon=JSbridge.importLatLon().split(",");
    var hash="lat="+wm.utils.truncateAfterPoint(latLon[0],7)+"&lon="+wm.utils.truncateAfterPoint(latLon[1],7);
    window.location.hash=hash;
  };

  this.presentUniPoint=function(uniPoint) {
    var res="", k, resKeys=[];
    if (uniPoint.file) resKeys.push("file");
    if (uniPoint.id) resKeys.push("id");
    if (uniPoint.type && ["gps","cell","mark"].indexOf(uniPoint.type) >= 0) {
      if (uniPoint.name) resKeys.push("name");
      if (uniPoint.comment) resKeys.push("comment");
      if (uniPoint.note) resKeys.push("note");
    }
    if (uniPoint.alt) resKeys.push("alt");
    if (uniPoint.cellData) resKeys.push("cellData");
    if (uniPoint.data) resKeys.push("data");
    if (uniPoint.data1) resKeys.push("data1");
    if (resKeys.length == 0) return "No useful data";
    for (k of resKeys) { res = res.concat(""+k+": "+uniPoint[k]+"\n"); }
    //for (k in uniPoint) { res = res.concat(""+k+": "+uniPoint[k]+"\n"); }
    return res;
  };

}; // end View
