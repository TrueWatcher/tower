package truewatcher.signaltrackwriter;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/*
    public void setDirty();
    public void addAndShiftNext(Point p);
    public void addAsNext(Point p);
    public int getNext();
    public int getMax();
    public void clearDirty();
    public Point iterate();
* */

public class TrackList implements StorageHelper.PointList {

  public class TrackSegment {
    private List<Trackpoint> mPArr = new ArrayList<Trackpoint>();

    public void add(Trackpoint tp) {
      mPArr.add(tp);
    }

    public JSONArray makeJsonPresentation() {
      JSONArray ja=new JSONArray();
      if (mPArr.isEmpty()) return ja;
      for(Trackpoint p : mPArr) {
        if (p.hasCoords()) { ja.put(p.makeJsonPresentation()); }
      }
      return ja;
    }

    public int getCount() { return mPArr.size(); }

    public List<Trackpoint> getArr() { return mPArr; }
  }

  private List<TrackSegment> mSArr  = new ArrayList<TrackSegment>();
  //private Iterator<TrackSegment> mIter = mSArr.iterator();
  private boolean mDirty=false;

  public int getSegCount() { return mSArr.size(); }

  public int getTotalCount() {
    int s=0;
    for (TrackSegment ts : mSArr) { s += ts.getCount(); }
    return s;
  }

  public void add(Trackpoint p) {
    if (p.isNewSegment()) {
      // keep the segment mark if it's given
      mSArr.add(new TrackSegment());
    }
    else if (mSArr.isEmpty()) {
      p.setNewSegment();
      mSArr.add(new TrackSegment());
    }
    mSArr.get(mSArr.size()-1).add(p);
  }

  public void clear() { mSArr.clear(); }

  public void setDirty() { mDirty=true; }
  public void clearDirty() { mDirty=false; }
  public boolean isDirty() { return mDirty; }

  int mSeg=0, mTp=0, mIterCount=0;

  public Trackpoint doIterate() {
    TrackSegment ts;
    int segCount=mSArr.size();
    if (segCount == 0) {
      mSeg=0;
      mTp=0;
      mIterCount=0;
      return null;
    }
    ts=mSArr.get(mSeg);
    int ptCount=ts.getCount();
    if (ptCount == 0 || mTp >= ptCount) {
      mSeg += 1;
      mTp=0;
      if (mSeg >= segCount) {
        mSeg=0;
        mIterCount=0;
        return null;
      }
    }
    Trackpoint tp=mSArr.get(mSeg).getArr().get(mTp);
    mTp += 1;
    mIterCount += 1;
    tp.setIdInt(mIterCount);
    return tp;
  }

  //-------------------------------------
  // for compatibility with StorageHelper.PointList
  @Override
  public void addAndShiftNext(Point p){ add((Trackpoint) p); }

  @Override
  public void addAsNext(Point p) { add((Trackpoint) p);}

  @Override
  public int getNext() { return 0; }

  @Override
  public int getMax() { return 10000; }

  @Override
  public Point iterate() { return (Point) doIterate(); }

}
