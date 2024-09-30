_SignalTrackWriter_ is a free mobile cell collecting tool for OS Android >= 7.0

No Google Services dependencies, only necessary permissions (ACCESS_FINE_LOCATION, FOREGROUND_SERVICE).
Uses a foreground service for prolonged operation.

The app writes a GPS track with cell ID and signal strength data to a local storage.

To start recording, press _Start_ or _Start New Segment_ and wait for your device's GPS to become operational.
Keep the app window maximized to avoid sudden stopping by your OS. 
Your can turn the screen off with the Power button.
To stop recording, press _Stop_.

The resulting file is called currentSignalTrack.csv and is stored in the app data folder. As Android 10+ do not allow third-party access to Androis/data, [newer versions of the app](http://tower.posmotrel.net/index.html#stw) (since 2.7.0) use Documents or Android/media
(somewhere like /storage/emulated/0/Documents/truewatcher.signaltrackwriter or /storage/emulated/0/Android/media/truewatcher.signaltrackwriter).
You can rename, remove or edit that file if you like.

To visualize the track path, cell IDs, and signal strength on a map, use our inventive [WebMapViewer](https://github.com/TrueWatcher/tower/tree/webmapviewer_exp)
You may open the Viewer in a web browser locally after downloading its files,
or simply from the [project site's page](http://tower.posmotrel.net/vs/).
It can open several tracks and is compatible with truewatcher.tower CSV and GPX files.

--------------------------------------
_SignalTrackWriter_ is a free software distributed under GPL 3.0 license without any warranty.
