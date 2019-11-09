package truewatcher.tower;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ListHelper {
  private boolean mToShowProximity=true;
  private Map<Integer,Double> mProximityMap=new HashMap<Integer,Double>();
  private Map<Integer,Double> mSortedProximityMap;
  private PointList mPointList;
  private String mSort="id";// id, rid (id, reverse), pr (proximity)
  
  public ListHelper(PointList pl) {
    mPointList=pl;
  }
  
  public void setSort(String s) {
    if (s.equals("pr")) { 
      mSort=s;
      mToShowProximity=true;
    }
    else if (s.equals("rid")) { mSort=s; }
    else mSort="id";    
  }
  
  public ArrayList<String> getList() {
    ArrayList<String> nl=new ArrayList<String>();
    Point p;
    int l=mPointList.getSize(), i=0;
    
    if ( ! mPointList.hasProximityOrigin()) { mToShowProximity=false; }
    if (mToShowProximity) makeProximityMap();
    if (mSort.equals("pr") && mToShowProximity) {
      int key;
      sortProximityMap();
      for (Entry<Integer,Double> entry : mSortedProximityMap.entrySet()) {
        key=entry.getKey();
        p=mPointList.getById(key);
        nl.add(getPointPresentation(p));
        if (U.DEBUG) Log.d(U.TAG, "ListHelper:"+ "key:"+key+", proximity:"+entry.getValue());
      }
    }
    else if (mSort.equals("rid")) {
      for (i=l-1; i >= 0; i-=1) { nl.add(getPresentation(i)); }
    }
    else {// sort by id
      for (; i < l; i+=1) { nl.add(getPresentation(i)); }
    }
    return nl;
  }
  
  public int getIdByPosition(int position) {
    if (mSort.equals("pr") && mToShowProximity) {
      return (new ArrayList<Integer>(mSortedProximityMap.keySet())).get(position);
    }
    else if (mSort.equals("rid")) {
      return mPointList.getIdByIndex(mPointList.getSize()-position-1);
    }
    return mPointList.getIdByIndex(position);
  }
  
  private String getPresentation(int position) {
    int k=mPointList.getIdByIndex(position);
    Point p=mPointList.getById(k);
    return getPointPresentation(p);
  }
  
  private void makeProximityMap() {
    mProximityMap=new HashMap<Integer,Double>();
    Point p;
    while ((p=mPointList.iterate()) != null) {
      mProximityMap.put(p.getId(), U.proximityM(p, mPointList.getProximityOrigin()));
    }
    mSortedProximityMap=null;
  }
  
  private void sortProximityMap() {
    mSortedProximityMap=U.sortByComparator(mProximityMap,true);
  }

  public static void printMap(Map<Integer,Double> map)  {
    for (Entry<Integer,Double> entry : map.entrySet()) {
      System.out.println("Key : " + entry.getKey() + " Value : "+ entry.getValue());
    }
  }

  private String getPointPresentation(Point p) {
    String pro="";
    String s;
    if (p.isProtected()) pro="🔒 ";// R.string.lock gives an int
    s=pro + String.valueOf(p.getId()) + "." + p.getType() + "."+p.getComment();
    if (mToShowProximity) {
      s+=" "+proximityToKm(mProximityMap.get(p.getId()));
    }
    return s;
  }
  
  public String getLocationPresentation() {
    if ( ! mPointList.hasProximityOrigin()) { return null; }
    Point location=mPointList.getProximityOrigin();
    if ( location.getId() > 0 ) {
      return location.getId()+"."+location.getType()+"."+location.getComment();
    }
    return location.getType()+"."+location.time;
  }
  
  public static String proximityToKm(double pr) {
    if (pr == U.FAR) return "-";
    int km=(int) Math.floor(pr/1000);
    if (km == 0) return String.valueOf(Math.round(pr))+"m";
    if (km < 100) return String.valueOf( Math.round(pr/10) / 100d )+"km";// 100d , not 100 !!!
    return String.valueOf(km)+"km";
  }
  
  public boolean getShowProximity() { return mToShowProximity; }
  public void setShowProximity() { mToShowProximity=true; }
  
  public static class MyArrayAdapter extends ArrayAdapter<String> {
    private Context mContext;
    private List<String> mObjects;
    private int mLayoutId;

    public MyArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
      super(context, -1, objects);
      mContext=context;
      mObjects=objects;
      mLayoutId=textViewResourceId;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View rowView = inflater.inflate(mLayoutId, parent, false);
      TextView textView = (TextView) rowView.findViewById(R.id.tvItem);
      textView.setText(mObjects.get(position));
      return rowView;
    }
    
    public void changeObjects(List<String> objects) {
      if (U.DEBUG) Log.d(U.TAG,"MyArrayAdapter:"+"Changing the data list");
      //mObjects=objects; // crashes on point deletion, mObjects must be kept !
      U.refillList(mObjects, objects);
      notifyDataSetChanged();
    }
  }
}
