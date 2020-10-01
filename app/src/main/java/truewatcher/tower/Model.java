package truewatcher.tower;

import android.content.Context;
import android.util.Log;

public class Model {
  private static Model sModel;
  public Point lastCell;
  public Point lastGps;
  public Point lastPosition;
  private CellInformer mCellInformer;
  private GpsInformer mGpsInformer;
  private PointList mPointList;
  private StorageHelper mStorageHelper;
  private JSbridge mJSbridge;
  private TrackStorage mTrackStorage;
  private TrackListener mTrackListener;
  public CellInformer getCellInformer() { return mCellInformer; }
  public GpsInformer getGpsInformer() { return mGpsInformer; }
  public PointList getPointList() { return mPointList; }
  public StorageHelper getStorageHelper() { return mStorageHelper; }
  public JSbridge getJSbridge() { return mJSbridge; }
  public TrackStorage getTrackStorage() { return mTrackStorage; }
  public TrackListener getTrackListener() { return mTrackListener; }
  private boolean mIsFresh=true;

  public static Model getInstance() {
    if (sModel == null) { sModel=new Model(); }
    return sModel;
  }
    
  private Model() {
    mCellInformer=new CellInformer();
    mGpsInformer=new GpsInformer();
    mStorageHelper=new StorageHelper();
    mPointList=new PointList( 0 , mStorageHelper);// must be resized after reading StoredPreferences in MainActivity    
    mJSbridge=new JSbridge();
    mJSbridge.setPointList(mPointList);
    mTrackStorage=new TrackStorage();
    mTrackListener=new TrackListener(mTrackStorage);
  }

  public U.Summary[] loadData(Context context, MyRegistry mrg) {
    U.Summary[] res=new U.Summary[2];
    res[0]=res[1]=null;
    if ( ! mIsFresh) return null;
    try {
      mPointList.adoptMax(mrg.getInt("maxPoints"));
      mStorageHelper.init(context, mrg.get("myFile"));
      res[0]=mPointList.load();
      if (U.DEBUG) Log.d(U.TAG,"MainPageFragment:"+ "Loaded "+res[0].adopted+" points");

      mTrackStorage.initTargetDir(context);
      if ( mrg.getBool("enableTrack")) {
        TrackStorage.Track2LatLonJSON converter = mTrackStorage.getTrack2LatLonJSON();
        String buf = converter.file2LatLonJSON();
        res[1] = converter.getResults();
        mJSbridge.replaceCurrentTrackLatLonJson(buf);
      }
    }
    catch (Exception e) {
      Log.e(U.TAG,"MainPageFragment:"+e.getMessage());
      return null;
    }
    mIsFresh=false;
    return res;
  }
  
}