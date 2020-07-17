
# <img src="app/src/main/assets/icons_readme/ic_tower1.svg" /> Tower: a navigation tool

_Tower_ is a navigation program for OS Android
for finding user location (by phone cell or GPS), viewing online maps, creating and storing waypoints and tracks, with minimal permissions and no background activity

## Features:

* no Google Services dependencies, only necessary permissions (fine and coarse location, internet)
* may connect to several map providers
* acceptable performance on slow GPRS-EDGE networks
* may find coarse location by phone cell without GPS
* displays and saves cell info ( MCC, MNC, LAC, CID )
* may create waypoints with GPS without phone and internet connections
* all waypoints are stored on a memory card
* waypoint lists may be stored in any number of files (that is, be grouped by locality or in any other way)
* waypoint lists may be exported/imported in the GPX format, compatible with many navigators and software
* finds location only by explicit user's command, so is very mild on the battery
* since v2.6 there are options of writing tracks (by means of a foreground service) and exporting them in the GPX format; GPX tracks from other devices may be viewed
* NOT implemented: map offline caching, routing, photo and video point attachments, serving cold beer :)
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
    
