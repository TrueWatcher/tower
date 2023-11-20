package truewatcher.signaltrackwriter;

import android.location.Location;

public class LatLon {
  public String lat="", lon="";

  public LatLon() {}

  public LatLon(String aLat, String aLon) {
    this.lat=aLat;
    this.lon=aLon;
  }

  public LatLon(Location loc) {
    this.lat=String.valueOf(loc.getLatitude());
    this.lon=String.valueOf(loc.getLongitude());
  }

  public boolean hasCoords() {
    boolean no=( lat == null || lon == null || lat.isEmpty() || lon.isEmpty() );
    return ! no;
  }
}
