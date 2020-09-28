
# <img src="app/src/main/assets/icons_readme/ic_tower1.svg" /> Tower: a navigation tool

_Tower_ is a navigation program for OS Android
for finding user location (by phone cell or GPS), viewing online maps, creating and storing waypoints and tracks, with minimal permissions and no background activity

## Features:

* no Google Services dependencies, only necessary permissions (fine and coarse location, internet)
* connects to several online map providers
* acceptable performance on slow GPRS-EDGE networks; may find coarse location by phone cell without GPS; may create waypoints with GPS without phone and internet connections
* displays and saves cell info ( MCC, MNC, LAC, CID )
* all waypoints are stored on a memory card and may be organized in any number of files (by regions etc.)
* waypoint lists may be exported or imported in the GPX format, compatible with many navigators and software
* finds location only by explicit user's command, so is very mild on the battery
* capable of writing tracks (by means of a foreground service) and exporting them in the GPX format
* tracks from other devices (GPX files) may be viewed along with your data
* NOT implemented: map offline caching, routing, editing tracks, photo and video attachments, serving cold beer :)
* for pure open distributions, that lack API keys and access to some services, there are options to enter user's own keys

## How to use it and where to get it

See [the project web page](http://tower.posmotrel.net) and

[<img src="http://tower.posmotrel.net/fdroid_readme.png"
     alt="Get it on F-Droid" />](https://f-droid.org/packages/truewatcher.tower/)

## External materials (see LICENSES):

This distribution includes the _LeafletJS_ javacsript library code ver. 1.3.4 (https://leafletjs.com, https://github.com/Leaflet/Leaflet),
which has BSD 2-Clause "Simplified" License

This program uses several web APIs and loads data and javascript code, details and references are included in the LICENSES file.

## API keys

Some of web services, despite being free of charge, require access keys. As these keys are not to be committed to public repositories, this app may appear in two kinds of distributions: full (with keys included in the binary) and pure open (without keys). The keyless distribution is fully operational, except for concerned services, and always has slots to enter user's own keys. See [the manual](http://tower.posmotrel.net/#external-materials-and-api-keys) for details.

Builders are free to provide their own keys with their distribution; the easiest way is via Gradle files as BuildConfig.yandexLocatorKey and BuildConfig.yandexMapKey ( [instruction](https://stackoverflow.com/questions/35722904/saving-the-api-key-in-gradle-properties) ).
    
