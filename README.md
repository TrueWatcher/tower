WebMapViewer is a free Javascript app for viewing user's tracks and waypoints over free online maps.
It's designed primarily for CSV files, generated by [truewatcher.tower navigation app](https://github.com/TrueWatcher/tower), and for GPX files from Garmmin navigators.

This fork (>= 1.13) is supporting also mapping cell signal strength data and cell IDs, collected by [truewatcher.signaltrackwriter app](https://github.com/TrueWatcher/tower/tree/signaltrackwriter).

To run this app, copy all the package files to your local folder and open the index.html in your favorite browser. You may need to adjust the browser's settings to allow for access to local files.
Alternatively, just open this app [from the project site](http://tower.posmotrel.net/vs) It's a standalone Javascript app, not a web-service, and it does not send your data anywhere.

To view your cell data, just select the file currentSignalTrack.csv from the truewatcher.signaltrackwriter working folder on your device (somewhere like /storage/emulated/0/Android/Documents/truewatcher.signaltrackwriter/currentSignalTrack.csv ) from the app page. You may need to give your browser the Storage permission. If everything works, you'll see the map of signal strength along your track over OpenStreetMap.

You can reopen that track as often as you like, as long as you don't load other files _after_ it. Use the Back button to clear the latest loaded file. Just reload the page in the browser to clear all loaded files.

The Point button displays an info about the trackpoint/waypoint closest to the central crosshair. Currently works only with CSV data, intended to provide a detailed info about various signal and cell params.

The zScale selector allows you to choose your data to be mapped. Default is signal strength ["dBm"](https://developer.android.com/reference/android/telephony/CellSignalStrength#getDbm()) auto scaled. Color scale is black for no signal, dark red for weak signal, brown for moderate and orange for strong. There are also choices of fixed strength ranges and additional options to paint your track according to cell ID. Our qualitative color scheme has only 7 colors, so if your track has more cells some of colors will be reused: check the Point data. Letest versions also support eNB ID, so that you can compare your towers to [Cellmapper](https://www.cellmapper.net)

Other kinds of signal strength/quality (there are [lots of them](https://developer.android.com/reference/android/telephony/CellSignalStrengthNr) in 5G) may be added later.

-----------------------------------------------
This package contains free code from Leafletjs library and leaflet-polycolor plugin.
