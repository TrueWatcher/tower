package truewatcher.tower;

public class LatLon {
  public String lat="", lon="";

  public LatLon() {}

  public LatLon(String aLat, String aLon) {
    this.lat=aLat;
    this.lon=aLon;
  }

  public boolean hasCoords() {
    boolean no=( lat == null || lon == null || lat.isEmpty() || lon.isEmpty() );
    return ! no;
  }
}
