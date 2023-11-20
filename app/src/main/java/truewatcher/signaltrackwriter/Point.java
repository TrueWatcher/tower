package truewatcher.signaltrackwriter;

public class Point extends Trackpoint {
  //public Point() { super(); }

  @Override
  public Point fromCsv(String line) throws U.DataException { return (Point) super.fromCsv(line);  }
}
