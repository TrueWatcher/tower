"use strict";
if ( ! wm.hasOwnProperty("fb")) wm.fb={};

wm.fb.Controller=function(view) {
  var _this=this,
      dataForMap=new wm.fb.MyJSbridge("wallpaper"),
      dataForMapBak=false,
      mapFrame=window.frames[0],
      fileIndex=0,
      zManager=false,
      dataFile, files;

  this.addFiles=function() {
    files=view.getFiles();
    if ( ! files || files.length == 0) {
      view.alert("Choose a GPX or CSV file");
      return;
    }
    if (files.length == 1 && isLatestReloading(files[0])) {
      console.log("about to load the latest file again -- stepping back");
      _this.stepBack();
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

  function isLatestReloading(aFile) {
    var loadedFileNames=dataForMap.getLoadedFiles();
    console.log("now are loaded:"+wm.utils.dumpArray(loadedFileNames));
    if ( ! loadedFileNames || ! loadedFileNames.length) return false;
    var lastLoadedFile=loadedFileNames[loadedFileNames.length-1];
    if (! lastLoadedFile) throw new Error("Empty lastLoadedFile");
    return lastLoadedFile == aFile.name;
  }

  function afterFileIsRead(text, onSuccess) {
    var data=false, parser=false;
    //alert(text)
    try {
      parser = new wm.fb.Parser();
      if (wm.fb.Zmanager) {
        if (! zManager) zManager = new wm.fb.Zmanager();
        parser.addZplugin(zManager);
      }
      data = parser.go(text, dataFile.name);
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
    afterFileIsParsed(parser,data,onSuccess);
  }

  function afterFileIsParsed(parser,data,onSuccess) {
    var loadedFileNames=dataForMap.getLoadedFiles();
    loadedFileNames.push(dataFile.name);
    dataForMap.setLoadedFiles(loadedFileNames);
    var parsers=dataForMap.getUsedParsers();
    parsers[dataFile.name] = parser;
    dataForMap.setUsedParsers(parsers);
    if (parser.hasZdata()) view.enableZcontrols();
    dataForMap.addMarkers(JSON.stringify(data.wayPoints));
    view.alert(dataFile.name+": "+data.res);
    if (view.getShouldResize()) { dataForMap.setBounded("*"); }
    else { dataForMap.setBounded(""); }
    if (data.trkPoints instanceof Array && data.trkPoints.length > 0) {
      //if (data.colors instanceof Array && data.colors.length > 0) {
      if (parser.hasZdata()) {
        if (! (data.colors instanceof Array && data.colors.length > 0)) throw new Error("Invalid color data");
        dataForMap.addSignalTrack(data); //dataForMap.exportSignalTrack(data);
        _this.updateSignalTrackColors("noRender");
      }
      else {
        dataForMap.pushViewTrack(data.trkPoints);
        dataForMap.pushViewTrackName(dataFile.name);
      }
    }
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
    if (! (dataForMapBak instanceof wm.fb.MyJSbridge)) throw new Error("Wrong backup");
    if (! (dataForMap instanceof wm.fb.MyJSbridge)) throw new Error("Wrong dataForMap");
    //dataForMap=dataForMapBak; // work only with reloading the bloody iframe
    console.log("restoring backup");
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
  }

  this.getBackup=function() { return dataForMapBak; };

  this.getDataForCenter = function() {
    //alert("click\n");
    var ll = dataForMap.importLatLon().split(',');
    //alert(ll);
    var lat = + ll[0];
    var lon = + ll[1];
    if (lat != lat || lon != lon) {
      console.log("Center lat or lon is NaN");
      return false;
    }
    //var parser = getLastParser(dataForMap);
    //var data = parser.getDataForCoords(lat,lon);
    var data = searchAcrossParsers(lat,lon,dataForMap);
    alert(view.presentUniPoint(data));
    return false;
  };

  function getLastParser(dataForMap) {
    var loadedFileNames=dataForMap.getLoadedFiles();
    var lastLoadedFile=loadedFileNames[loadedFileNames.length-1];
    if (! lastLoadedFile) throw new Error("No files loaded");
    var parsers = dataForMap.getUsedParsers();
    //alert(parsers);
    var parser = parsers[lastLoadedFile];
    if ( ! (parser instanceof wm.fb.Parser)) throw new Error("Invalid parser at "+lastLoadedFile+":"+parser);
    return parser;
  }

  function searchAcrossParsers(lat,lon,dataForMap) {
    var k,parser,p,
        minDistance=1E+20,
        found={},
        parsers = dataForMap.getUsedParsers(),
        loadedFileNames=dataForMap.getLoadedFiles();

    for (k of loadedFileNames) {
      parser = parsers[k];
      if (! parser) throw new Error("Empty usedParser at "+k);
      p = parser.getDataForCoords(lat,lon);
      if (p.range > minDistance) continue;
      minDistance = p.range;
      found = p;
    }
    if (minDistance > 1E+6) found = {};
    return found;
  }

  this.updateSignalTrackColors = function(noRender) {
    var  zscaleOpt,  allColors = [], allExtras = [];

    if (! zManager) {
      console.log("controller :: updateSignalTrackColors: no zManager attached");
      return false;
    }
    zscaleOpt = view.getZscale();
    zManager.setZscale(zscaleOpt);
    allExtras = getAllExtras(dataForMap);
    allColors = zManager.updateSignalColors(allExtras);
    dataForMap.exportSignalTrackColors(allColors);
    dataForMap.setDirty(2);
    if (noRender !== "noRender") view.render(dataForMap);// zscaleSelect.onchange vs afterFileIsParsed
  };

  function getAllExtras(dataForMap) {
    var k, parser, extras, allExtras = [],
        parsers = dataForMap.getUsedParsers(),
        loadedFileNames=dataForMap.getLoadedFiles();

    for (k of loadedFileNames) {
      parser = parsers[k];
      if (! parser) throw new Error("Empty usedParser at "+k);
      extras = parser.getExtras();
      if (! extras || extras.length == 0) { console.log("Empty EXTRAS at "+k); }
      else { allExtras = allExtras.concat(extras); }
    }
    return allExtras;
  }

};// end Controller
