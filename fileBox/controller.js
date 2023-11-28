"use strict";
if ( ! wm.hasOwnProperty("fb")) wm.fb={};

wm.fb.Controller=function(view) {
  var _this=this,
      dataForMap=new wm.fb.MyJSbridge("wallpaper"),
      dataForMapBak=false,
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
      parser.addZplugin(new wm.fb.Zmanager);
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
    afterFileIsParsed(parser,data,onSuccess);
  }

  function afterFileIsParsed(parser,data,onSuccess) {
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
    var loadedFileNames=dataForMap.getLoadedFiles();
    loadedFileNames.push(dataFile.name);
    dataForMap.setLoadedFiles(loadedFileNames);
    var parsers=dataForMap.getUsedParsers();
    parsers[dataFile.name] = parser;
    dataForMap.setUsedParsers(parsers);
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
  }

  this.getBackup=function() { return dataForMapBak; };

  this.getDataForCenter = function() {
    //alert("click\n");
    var parser = getLastParser(dataForMap);
    var ll = dataForMap.importLatLon().split(',');
    //alert(ll);
    var lat = + ll[0];
    var lon = + ll[1];
    if (lat != lat || lon != lon) {
      console.log("Center lat or lon is NaN");
      return false;
    }
    var data = parser.getDataForCoords(lat,lon);
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

};// end Controller
