package truewatcher.tower;

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
  
}