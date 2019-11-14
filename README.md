
# <img src="app/src/main/assets/icons_readme/ic_tower1.svg" /> Tower: a navigation tool

_Tower_ is a navigation program for OS Android
for finding user location (by phone cell or GPS), viewing online maps, creating and storing waypoints, with minimal permissions and no background activity

## Features:

* no Google Services dependencies, only necessary permissions (fine and coarse location, internet)
* may connect to several map providers
* acceptable performance on slow GPRS-EDGE networks
* may find coarse location by phone cell without GPS
* displays and saves cell info ( _MCC, MNC, LAC, CID_ )
* may create waypoints with GPS without phone and internet connections
* all waypoints are stored on a memory card
* waypoint lists may be stored in any number of files (that is, be grouped by locality or in any other way)
* waypoint lists may be exported/imported in the GPX format, compatible with many navigators and soft like OziExplorer
* finds location only by explicit user's command, doesn't perform any background activities, so is very mild on the battery
* NOT implemented: map offline caching, track recording, routing, photo and video point attachments, serving cold beer :)
* tracks may be implemented in later versions

## How to use it

See [the manual](http://tower.posmotrel.net)

## External materials (see LICENSES):

This distribution includes the _LeafletJS_ javacsript library code ver. 1.3.4 (https://leafletjs.com, https://github.com/Leaflet/Leaflet),
which has BSD 2-Clause "Simplified" License

This program uses several web APIs and loads data and javascript code, details and references are included in the LICENSES file.

## API keys

The keys for web services are not included in this repo, so, if you build from it directly, some features will not work. 
You are free to obtain your own keys for Yandex Locator and Yandex Maps; the easiest way to use them is via Gradle files as BuildConfig.yandexLocatorKey and BuildConfig.yandexMapKey ( [instruction](https://stackoverflow.com/questions/35722904/saving-the-api-key-in-gradle-properties) ).
    
