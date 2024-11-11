package truewatcher.signaltrackwriter;

import androidx.collection.SparseArrayCompat;

public class Model {
  private static Model sMe=null;

  private TrackStorage mTrackStorage=new TrackStorage();
  private TrackListener mTrackListener=new TrackListener(mTrackStorage);
  private SparseArrayCompat<String> mEnbCache = new SparseArrayCompat();

  public static Model getInstance() {
    if (sMe == null) sMe=new Model();
    return sMe;
  }

  private Model() {}

  public TrackStorage getTrackStorage() { return mTrackStorage; }
  public TrackListener getTrackListener() { return mTrackListener; }
  public SparseArrayCompat<String> getEnbCache() { return mEnbCache; }
}



