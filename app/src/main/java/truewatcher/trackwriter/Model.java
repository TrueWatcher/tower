package truewatcher.trackwriter;

public class Model {
  private static Model sMe=null;

  private TrackStorage mTrackStorage=new TrackStorage();
  private TrackListener mTrackListener=new TrackListener(mTrackStorage);

  public static Model getInstance() {
    if (sMe == null) sMe=new Model();
    return sMe;
  }

  private Model() {}

  public TrackStorage getTrackStorage() { return mTrackStorage; }

  public TrackListener getTrackListener() { return mTrackListener; }

}



