package truewatcher.tower;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import android.util.Log;
import android.util.SparseArray;

public class PointList {

  public static final String OK="OK";
  private SparseArray<Point> mArr = new SparseArray<Point>();
  private int mNext;
  private int mMax;
  private int mFirst;
  private boolean mDirty=false;
  private int ii=0;
  private StorageHelper mStorageHelper;
  private List<Point> mRemoved=new ArrayList<Point>();
  private Point mProximityOrigin;
  private boolean mUseMockRegistry=false;
  private boolean mShouldUseTrash;
  
  public PointList(int max) {
    fastClear();
    mMax=max;
  }
  
  public PointList(int max, StorageHelper sh) {
    mStorageHelper=sh;
    fastClear();
    mMax=max;
  }
  
  public void clear() {
    // puts removed points to trash
    int l=mArr.size();
    int i=0;
    for (; i < l; i+=1) {
      mRemoved.add(mArr.valueAt(0));      
      mArr.removeAt(0);
      //onPointdeleted();
    }
    mFirst=-1;
    mNext=1;
    mDirty=true;
  }
  
  public void fastClear() {
    // does not use trash
    mArr.clear();
    mFirst=-1;
    mNext=1;
    mDirty=true;
  }
  
  public int adoptMax(int m) {
    if (m < 1) return mMax;
    // cannot be set to less than actual count
    if (m < mArr.size()) { mMax=mArr.size(); } //if m < mMax   
    else { mMax=m; }
    return mMax;
  }
  
  public int getMax() { return mMax; }
  
  public int getSize() { return mArr.size(); }
  
  public String addAsNext(Point p) throws U.DataException {
    String s="";
    if (mNext != p.getId()) throw new U.RunException("mNext="+mNext+", id="+p.getId());
    if (mArr.size() >= mMax) {
      String r=trimFirst();
      if (r != null) s="Removed "+r;
    } 
    mArr.append(mNext, p);
    mDirty=true;
    mNext+=1;    
    return s;
  }
  
  public String addAndShiftNext(Point p) throws U.DataException  {
    String s="";
    if (mArr.size() >= mMax) {
      String r=trimFirst();
      if (r != null) s="Removed "+r;
    } 
    int newId = Math.max(p.getId(), mNext);
    p.setId(newId);
    mArr.append(newId, p);
    mDirty=true;
    mNext=newId+1;   
    return s;
  }
  
  private String trimFirst() throws U.DataException {
    Point toRemove=getEdge();
    if (toRemove == null) return "";
    mFirst=toRemove.getId();
    Log.d(U.TAG,"PointList:"+"About to delete:"+String.valueOf(mFirst));
    mRemoved.add(toRemove);
    mArr.delete(mFirst);
    //onPointdeleted();
    String deletedId=String.valueOf(mFirst);
    mDirty=true;
    return deletedId;
  }
  
  public int getNext() { return mNext; }
  public String getNextS() { return String.valueOf(mNext); }
  
  public Point getEdge() throws U.DataException {
    if (U.DEBUG) Log.d(U.TAG,"PointList:"+"Array size "+mArr.size()+"/"+mMax);
    if (mArr.size() < mMax) {
      if (U.DEBUG) Log.d(U.TAG,"PointList:"+"No need to remove anything:"+mArr.size()+"/"+mMax);
      return null;
    }
    int i=0;
    Point p;
    while ( (p=mArr.valueAt(i)).isProtected() ) {
      i+=1;
      if (i >= mArr.size()) {
        if (U.DEBUG) Log.d(U.TAG,"PointList:"+"No removable points");
        throw new U.DataException ("No room");      
      }
    }
    if (U.DEBUG) Log.d(U.TAG,"PointList:"+"Found edge point:"+String.valueOf(p.getId()));
    return p;
  }
  
  public boolean isEmpty() { return mArr.size() == 0; }
  
  public boolean isDirty() { return mDirty; }
  public void clearDirty() { mDirty=false; }
  public void setDirty() { mDirty=true; }
  
  public U.Summary load() throws IOException,U.DataException {
    U.Summary loaded=mStorageHelper.readPoints(this);
    mDirty=false;
    return loaded;
  }
  
  public U.Summary clearAndLoad() throws IOException,U.DataException {
    fastClear();
    U.Summary loaded=mStorageHelper.readPoints(this);
    mDirty=true;
    return loaded;
  }
  
  public String save() {
    try {
      mStorageHelper.savePoints(this);
      if ( ! mRemoved.isEmpty()) tryUseTrash();
      mDirty=false;
      return PointList.OK;
    }
    catch (IOException e) {     
      Log.e(U.TAG,"PointList_save:"+e.getMessage());
      return "File error:"+e.getMessage();
    }
    catch (Exception e) {
      Log.e(U.TAG, "PointList_save:"+e.getMessage());
      return "Save error:"+e.getMessage();
    }  
  }
  
  public void forceUseTrash() {// for tests
    mUseMockRegistry=true;
    mShouldUseTrash=true;
  }
  
  public void forceNotUseTrash() {// for tests
    mUseMockRegistry=true;
    mShouldUseTrash=false;
  }
  
  private boolean shouldUseTrash() {
    if (mUseMockRegistry) return mShouldUseTrash;
    return MyRegistry.getInstance().getBool("useTrash");
  }

  private void tryUseTrash() throws IOException {
    if (shouldUseTrash()) {
      for ( Point p : mRemoved ) { mStorageHelper.trashPoint(p); }// normally it is only one point
    }
    mRemoved=new ArrayList<Point>();
  }
  
  public String makeJsonPresentation() {
    JSONArray ja=new JSONArray();
    int size=mArr.size();
    if (size == 0) return ja.toString();
    int i=0;
    for( ; i < size; i+=1) {
      Point p=mArr.valueAt(i);
      if (p.hasCoords()) { ja.put(p.makeJsonPresentation(mArr.keyAt(i))); }
    }
    return ja.toString();
  }
  
  public int getIdByIndex(int i) {
    return mArr.keyAt(i);
  }
  
  public List<String> getIndices() {
    List<String> r = new ArrayList<String>();
    Point p;
    int id;
    while ((p=iterate()) != null) {  
      id=p.getId();
      r.add(String.valueOf(id));
    }
    return r;
  }
  
  public Point iterate() {
    int size=mArr.size();
    if (size == 0 || ii >= size) {
      ii=0;
      return null;
    }
    Point p=mArr.valueAt(ii);
    if (p.getId() <= 0) p.setId(mArr.keyAt(ii));
    ii+=1;
    return p;
  }
  
  public Point getById(int id) {
    return mArr.get(id);    
  }
  
  public void update(Point p) {
    int id=p.getId();
    if (mArr.indexOfKey(id) < 0) {
      Log.e(U.TAG,"PointList:"+"Unknown id="+id);
      return;
    }
    mArr.put(id, p);
    mDirty=true;
  }
  
  public void moveUnprotectedToTrash(int id) {
    if (mArr.indexOfKey(id) < 0) {
      Log.e(U.TAG,"PointList:"+"Unknown id="+id);
      return;
    }
    Point p=mArr.get(id);
    if (p.isProtected()) return; 
    mRemoved.add(p);
    mArr.delete(id);
    mDirty=true;
  }
  
  public int fastDeleteGroup(int from, int until) {
    int count=0, id;
    Point p;
    //Iterator ip=mArr.iterator(); does not work for SparseArray
    List<Integer> toRemove= new ArrayList<Integer>();
    while ((p=iterate()) != null) {  
      id=p.getId();
      if (id >= from && (until < 0 || id <= until)) toRemove.add(id);
    }
    count=toRemove.size();
    if (count > 0) {
      for (int i=0; i < count; i+=1) { mArr.delete(toRemove.get(i)); }
      mDirty=true;
    }
    return count;
  }
  
  public void renumber() {
    SparseArray<Point> a=new SparseArray<Point>();
    int l=mArr.size();
    Point p;
    for (int i=0; i < l; i+=1) {
      p=mArr.valueAt(i);
      p.setId(i+1);
      a.append(i+1,p);      
    }
    mArr=a;
    mDirty=true;
  }
  
  public StorageHelper getStorageHelper() { return mStorageHelper; }
  
  public void setProximityOrigin(Point loc) {
    if (loc !=null && loc.hasCoords()) mProximityOrigin=(Point) loc.clone();
  }
  
  public boolean hasProximityOrigin() {  return mProximityOrigin != null; }
  public Point getProximityOrigin() {  return mProximityOrigin; }

  public int findNearest(Point cursor) {
    double deg2rad = 0.0174532925199433;
    double cosLat=Math.cos(Double.parseDouble(cursor.lat)*deg2rad);
    double minSqDistance=1e99, sqDistance;
    int foundId=-1;
    Point p;

    while ((p=iterate()) != null) {
      if ( ! p.hasCoords()) continue;
      sqDistance=U.sqDistance(p, cursor, cosLat);
      if (sqDistance < minSqDistance) {
        minSqDistance=sqDistance;
        foundId=p.getId();
      }
    }
    return foundId;
  }
  
}
