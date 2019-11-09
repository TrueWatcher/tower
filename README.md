
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

Currently, the following map services are available (choose it on the SETTINGS page):

* OpenStreetMap map  
* Google hybrid  
* Yandex hybrid  

The map position is set in three ways:

* location by current phone cell (CELL command)  
* location by GPS receiver (GPS command)  
* by any stored waypoint ( _LIST - choose point - MAP_ )  

Map data is downloaded from the network connection (4G, 3G, GPRS-EDGE). A waypoint layer is visible even when network is unavailable.

Resolving of cell data to coordinates is done with free web-services, currently available are (choose it on the SETTINGS page):

* Yandex Locator
* mylnikov.org

Sometimes the free services show only very general location, fail altogether or are slow, especialy with the 3G connection type or on unstable network. On the light side, urban 4G connections are usually resolved very rapidly with 100-300m precision. This program displays cell data ( _MCC_ - coutry code, _MNC_ - operator code, _LAC_ - region number, _CID_ - cell number) and allows to store them even if coords are missing. GPS may also be slow or fail, depending on your device and environmental conditions.

A new waypoint is created on the ADD page by one of the four ways:

* by the current map center  
* by the current cell coords  
* by the current GPS location  
* by any lattitude and longitude, entered by user  

The waypoint is always given a number by the program, and the user may enter a short name (also called comment). The number and comment are shown on a map next to the waypoint mark. The cell and gps points usually also have a range value, displyed on a POINT page (the marker size is unrelated to that). A longer description may be entered/edited on the POINT page, it is stored with the point data but not shown on a map.

The program assesses a location only by a user's explicit command (CELL, GPS, ADD-Get). There's no automatic location update, so this implementation cannot write tracks. No background activity is very good for the battery :)

A waypoint list is a ring buffer, max number of points is set on the SETTINGS page. From that limit, creating a new point causes removal of the oldest stored point; more precisely, the oldest unprotected point, as any waypoint may be marked as protected. If all remaining points are protected, addition is blocked. The user has to unprotect or/and manually delete some points ( _LIST - choose point - Toggle protect, Delete_ ) or increase the limit. There's a command _LIST - Delete all_ to clear the list and reset numbering.

There's a clever option "_save removed points to trash.csv_" (in SETTINGS). If it's on, the trash file will be created in the program's data folder on a vitrual card, that contains the default list file current.csv (somewhere like _/sdcard/Android/data/truewatcher.tower/files/current.csv_), both have the same trivial format and may be retrieved or edited manually.

Any point may be chosen as a base for measuring distances, which are presented in the point list together with numbers and names. A stored waypoint may be chosen via _LIST - choose point - As center_, a new point - by checking _As center_ on the ADD page. When a location is determined by cell/GPS, the location point is automatically set as center, even if it's not a stored waypoint. The point list may be sorted by those distances: _LIST - By proximity_.

There's also a page LIST-FILE with a few point list commands:

* _Open_ ( _Open - choose a file - OK_ ) sets that file as the current point list and loads its content  
* _Load_ adds points from the chosen file to the currently open list  
* _Export_ saves points to a new file; there're options for number range and removal of exported points; if the file exists, it will be silently overwritten. To create a new empty list, use: _Export - set a name - from 0 until 0 - OK_  
* _Delete_ removes the selected file; unlike _LIST - Delete all_ it doesn't ask confirmation, nor saves content to trash.csv  . 

The _LIST-FILE-Open_ works only with csv files, created by this app. The format is trivial and those files may be easily edited manually. Other commands work, beside these, with the GPX format, that works with Garmin navigators and OziExplorer (no warranty, though); tracks and routes are not supported. Open and Load both check the expected point count and give an error if it exceeds the current limit.


Summary of screen navigation commands:

page &nbsp; &nbsp; : screen navigation commands  
MAP <img style="vertical-align: middle" src="app/src/main/assets/icons_readme/map_black_24.svg" /> : ADD, LIST, SETTINGS  
ADD <img style="vertical-align: middle" src="app/src/main/assets/icons_readme/add-24px.svg" /> : OK(MAP), BACK(MAP), SETTINGS  
LIST <img style="vertical-align: middle" src="app/src/main/assets/icons_readme/list-24px.svg" /> : click on a line(POINT), MAP, FILE, SETTINGS  
POINT &nbsp; : MAP, LIST, SETTINGS  
FILE <img style="vertical-align: middle" src="app/src/main/assets/icons_readme/folder_open-24px.svg" /> : OK(LIST), BACK(LIST), SETTINGS  
SETTINGS <img style="vertical-align: middle" src="app/src/main/assets/icons_readme/build-24px.svg" /> : BACK(MAP/ADD/LIST/POINT/FILE)  

## External materials (see LICENSES):

This distribution includes the _LeafletJS_ javacsript library code ver. 1.3.4 (https://leafletjs.com, https://github.com/Leaflet/Leaflet),
which has BSD 2-Clause "Simplified" License

This program uses several web APIs and loads data and javascript code, details and references are included in the LICENSES file.
    
