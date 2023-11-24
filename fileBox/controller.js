"use strict";
if ( ! wm.hasOwnProperty("fb")) wm.fb={};

function $(id) { return document.getElementById(id); }

wm.fb.View=function() {
  var panelTable=$("panelTable"),
      wmIframe=$("wmIframe"),
      alertTd=$("alertTd"),
      fileInput=$("fileInput"),
      backBtn=$("backBtn"),
      resizeInput=$("resizeInput"),
      providerSelect=$("providerSelect"),
      getDataBtn=$("getDataBtn"),
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

  this.setHandlers2=function(showDataForCenter) {
    getDataBtn.onclick=showDataForCenter;
  }

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

  this.disableBackBtn=function() { backBtn.disabled=true; };
  this.enableBackBtn=function() { backBtn.disabled=false; };

  this.render=function(dataForMap) {
    if ( ! (dataForMap instanceof wm.fb.MyJSbridge)) throw new Error("Rendering from non-JSbridge");
    var provider=dataForMap.importMapType();
    var mapFrame=window.frames[0];
    switch ( dataForMap.getDirty() ) {
    case 0:
      console.log("doing nothing");
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

}; // end View

wm.fb.Controller=function(view) {
  var _this=this,
      dataForMap=new wm.fb.MyJSbridge("wallpaper"),
      dataForMapBak=false,
      loadedFileNames=[],
      lastLoadedFileName="",
      parsers=[],
      mapFrame=window.frames[0],
      fileIndex=0,
      dataFile, files;

  this.addFiles=function() {
    files=view.getFiles();
    if ( ! files || files.length == 0) {
      view.alert("Choose a GPX or CSV file");
      return;
    }
    fileIndex=0;
    dataForMapBak=wm.fb.MyJSbridge.clone(dataForMap);
    dataForMapBak.clearDirty();// it will be re-decided on restoring
    view.enableBackBtn();
    if (files.length == 1) { addFile(files[0], commitToMap); }
    else { addFile(files[0], getAnotherFile); }
  };

  function getAnotherFile() {
    fileIndex += 1;
    if (fileIndex == files.length) {
      commitToMap();
      view.alert("loaded "+fileIndex+" files");
      return;
    }
    addFile(files[fileIndex], getAnotherFile);
  }

  function addFile(aFile, onSuccess) {
    if ( ! (aFile instanceof File)) throw new Error("Wrong AFILE");
    if ( ! (typeof onSuccess == "function")) throw new Error("Wrong ONSUCCESS");
    dataFile=aFile;
    console.log("Reading "+dataFile.name);
    var reader = new FileReader();
    reader.onload = function(event) { afterFileIsRead(event.target.result, onSuccess); };
    reader.readAsText(dataFile);
    return;
  };

  function afterFileIsRead(text, onSuccess) {
    var data=false, parser=false;
    //alert(text)
    try {
      parser = new wm.fb.Parser();
      data = parser.go(text);
    }
    catch (e) {
      view.alert("Cannot parse "+dataFile.name+": "+e);
      rollBack();
      throw e;
    }
    if ( ! data) {
      rollBack();
      throw new Error("Parser returned no data");
    }
    //console.log(wm.utils.dumpArray(data));
    if ( ( ! data.trkPoints || data.trkPoints.length == 0 || (data.trkPoints.length == 1 && data.trkPoints[0].length == 0)) && ( ! data.wayPoints || data.wayPoints.length == 0) ) {
      // processed and found nothing useful
      view.alert(dataFile.name+": found no useful data");//+data.res
      rollBack();
      return;
    }
    lastLoadedFileName=dataFile.name;
    loadedFileNames.push(dataFile.name);
    parsers[dataFile.name] = parser;
    afterFileIsParsed(data,onSuccess);
  }

  function afterFileIsParsed(data,onSuccess) {
    if (data.trkPoints instanceof Array && data.trkPoints.length > 0) {
      if (data.colors instanceof Array && data.colors.length > 0) {
        dataForMap.exportSignalTrack(data);
      }
      else {
        dataForMap.pushViewTrack(data.trkPoints);
        dataForMap.pushViewTrackName(dataFile.name);
      }
    }
    dataForMap.addMarkers(JSON.stringify(data.wayPoints));
    view.alert(dataFile.name+": "+data.res);
    if (view.getShouldResize()) { dataForMap.setBounded("*"); }
    else { dataForMap.setBounded(""); }
    onSuccess();
  }

  function rollBack() {
    if ( fileIndex == 0 ) {
      dataForMapBak=false;
      view.disableBackBtn();
      return;
    }
    console.log("rolling back after read error");
    fileIndex=0;
    doStepBack();
  }

  function commitToMap() {
    if (dataForMap.hasNoCoords()) {
      dataForMap.setMapType( view.getProvider() );
      dataForMap.onMoveend=view.showCoords;
      dataForMap.setBounded("*");
      dataForMap.setDirty(3);
    }
    else {
      dataForMap.setDirty(2);
    }
    view.render(dataForMap);
  }

  this.reloadMap=function() {
    if (dataForMap.hasNoCoords()) return;
    dataForMap.setMapType( view.getProvider() );
    dataForMap.setDirty(3);
    view.render(dataForMap);
  };

  this.stepBack=function() {
    if (dataForMap.hasNoCoords()) { view.alert("No data to dismiss"); return; }
    if (dataForMapBak === false) { view.alert("This goes only one step"); return; }
    view.alert("Dismissing last addition");
    doStepBack();
  };

  function doStepBack() {
    if (! (dataForMap instanceof wm.fb.MyJSbridge)) throw new Error("Wrong backup");
    //dataForMap=dataForMapBak; // work only with reloading the bloody iframe
    wm.fb.MyJSbridge.copy(dataForMapBak,dataForMap);
    dataForMapBak=false;
    view.disableBackBtn();
    if ( dataForMap.hasNoCoords()) { dataForMap.setDirty(3); }// back to the wallpaper
    else {
      if (view.getShouldResize()) { dataForMap.setBounded("*"); }
      else { dataForMap.setBounded(""); }
      dataForMap.setMapType( view.getProvider() );
      dataForMap.setDirty(2);
    }
    view.render(dataForMap);
    var i = loadedFileNames.indexOf(lastLoadedFileName);
    if ( i >= 0 ) {
      loadedFileNames[i] = null;
      loadedFileNames.splice(i,1);
      lastLoadedFileName="";
    }
  }

  this.getBackup=function() { return dataForMapBak; };

  this.getDataForCenter = function() {
    //alert("click\n"+lastLoadedFileName);
    if (! lastLoadedFileName) return false;
    var parser = parsers[lastLoadedFileName];
    if ( ! (parser instanceof wm.fb.Parser)) throw new Error("Invalid parser at "+lastLoadedFileName);

    var ll = dataForMap.importLatLon().split(',');
    //alert(ll);
    var lat = + ll[0];
    var lon = + ll[1];
    if (lat != lat || lon != lon) {
      console.log("Center lat or lon is NaN");
      return false;
    }
    var data = parser.getDataForCoords(lat,lon);
    alert(`id:${data.id},\ncell${data.data1},\nsignal:${data.data}`);
    return false;
  };

};// end Controller
