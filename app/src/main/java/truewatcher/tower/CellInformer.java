package truewatcher.tower;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import android.Manifest;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CellInformer extends PointFetcher implements PermissionReceiver,HttpReceiver {
  
  private JSONObject cellData=new JSONObject();
  private CellResolver mCellResolver;
  private String mResponse="";
  
  @Override
  protected String getPermissionType() { return Manifest.permission.ACCESS_COARSE_LOCATION; }
  
  @Override
  protected int getPermissionCode() { return 1; }
  
  @RequiresApi(api = Build.VERSION_CODES.Q)
  @Override
  public void afterLocationPermissionOk() {
    String cd=getInfo();
    if (! mStatus.equals("available") && ! mStatus.equals("mocking")) {
      mStatus="error";
      mPi.addProgress("failed to get cell info");
      return;
    }
    mPoint=new Point("cell");
    mPoint.cellData=cd;
    mPi.showData(JsonHelper.filterQuotes(cd));
    if ( "none".equals( MyRegistry.getInstance().get("cellResolver") ) ) {
      mPi.addProgress("cell location is off");
      onPointavailable(mPoint);
      return;
    }
    mPi.addProgress("trying to resolve...");
    startResolveCell();
  }
  
  public void onlyResolve(PointIndicator pi, PointReceiver pr, Point p) {
  // a shortcut entrypoint for resolving a ready cell without permission checks
    mPi=pi;
    mPointReceiver=pr;
    mPoint=p;
    mStatus="asked to resolve";
    mToUpdateLocation=false;
    if ( ! p.getType().equals("cell") || p.cellData == null || p.cellData.length() < 10) {
      mPi.addProgress("Not a valid cell");
      return;
    }
    if ( "none".equals( MyRegistry.getInstance().get("cellResolver") ) ) {
      mPi.addProgress("Select cell location service");
      return;
    }
    startResolveCell();
  }
  
  private void startResolveCell() {
    String resolverUri = "";
    String reqData = "";

    try {
      cellData=new JSONObject(mPoint.cellData);
    }
    catch (JSONException e) { 
      Log.e(U.TAG,"Wrong point.cellData");
      Log.e(U.TAG,e.getMessage());
      mStatus="Wrong point.cellData";
      onPointavailable(mPoint);
    }
    try {
      mCellResolver = CellResolverFactory.getResolver(MyRegistry.getInstance().get("cellResolver"));
      resolverUri = mCellResolver.makeResolverUri(cellData);
      reqData = mCellResolver.makeResolverData(cellData);
    }
    catch (U.DataException e) {
      Log.e(U.TAG,e.getMessage());
      mStatus="Failure:"+e.getMessage();
      onPointavailable(mPoint);
      return;
    }
    if (U.DEBUG) Log.i(U.TAG,"startResolveCell:"+"About to query "+resolverUri+"\n data="+reqData);
    AsyncTask<String, Void, String> req = mCellResolver.getRequestTask(this);
    req.execute(resolverUri,reqData);
  }

  @Override
  public void onHttpReceived(String result) {
    mResponse=result;
    if (U.DEBUG) Log.i(U.TAG,"onHttpReceived:"+"Response:"+mResponse);
    try {
      JSONObject resolvedData = mCellResolver.getResolvedData(mResponse);
      mStatus = "resolved";
      mPoint.lat=resolvedData.optString("lat");
      mPoint.lon=resolvedData.optString("lon");
      String r=resolvedData.optString("range");
      if ( r != null && ! r.isEmpty()) mPoint.range=r;
    }
    catch (U.DataException e) {
      Log.e(U.TAG,e.getMessage());
      mStatus="Failure:"+e.getMessage();
    }

    mPi.addProgress(mStatus);
    //if (mStatus != "resolved") return;
    if (mPoint.range != null && ! mPoint.range.isEmpty()) {
      String rt=PointIndicator.floor(mPoint.range);
      mPi.addData(" Accuracy:"+rt);
    }
    onPointavailable(mPoint);
  }
  
  @Override
  public void onHttpError(String error) {
    mStatus="Http Error:"+error;
    mPi.addProgress(mStatus);
    onPointavailable(mPoint);
  }
  
  @RequiresApi(api = Build.VERSION_CODES.Q)
  private String getInfo() {
    if (U.DEBUG) Log.d(U.TAG,"CellInformer:"+"getInfo here");
    TelephonyManager tm = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
    List<CellInfo> cellInfos = new ArrayList<>();
    try {
    // the permission is checked in PointFetcher; just to make Lint happy
      cellInfos = tm.getAllCellInfo();
    }
    catch (SecurityException e) { throw new U.RunException(e.getMessage()); }
    if (null != cellInfos && cellInfos.size() > 0) {
      if (U.DEBUG) Log.d(U.TAG,"got "+cellInfos.size()+" cell infos");
      cellData = getMyCellParams(cellInfos.get(0));
    }
    else {
      if (U.DEBUG) Log.d(U.TAG,"got null cell info");
      cellData = getMockParams();
      mStatus="mocking";
    }
    return cellData.toString();    
  }
  
  @RequiresApi(api = Build.VERSION_CODES.Q)
  private JSONObject getMyCellParams(CellInfo cellInfo) {
    JSONObject data=new JSONObject();
    JSONObject err=new JSONObject();
    try {
      if (cellInfo instanceof CellInfoGsm) {
        CellInfoGsm cellInfoGsm = (CellInfoGsm) cellInfo;
        CellIdentityGsm cellIdentityGsm = cellInfoGsm.getCellIdentity();
        data.accumulate("type", "GSM");
        data.accumulate("MCC", cellIdentityGsm.getMcc());
        data.accumulate("MNC", cellIdentityGsm.getMnc());
        data.accumulate("LAC", cellIdentityGsm.getLac());
        data.accumulate("CID",  cellIdentityGsm.getCid());
        int dbm = cellInfoGsm.getCellSignalStrength().getDbm();
        data.accumulate("dBm", dbm);
      }
      else if (cellInfo instanceof CellInfoCdma){
        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
        CellIdentityCdma cellIdentityCdma = cellInfoCdma.getCellIdentity();
        data.accumulate("type", "CDMA");
        data.accumulate("MCC", cellIdentityCdma.getSystemId());
        data.accumulate("MNC", cellIdentityCdma.getNetworkId());
        data.accumulate("LAC", 0); // cellIdentityCdma.getLac());
        data.accumulate("CID", cellIdentityCdma. getBasestationId());
        int dbm = cellInfoCdma.getCellSignalStrength().getDbm();
        data.accumulate("dBm", dbm);
      }
      else if (cellInfo instanceof CellInfoWcdma){
        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo;
        CellIdentityWcdma cellIdentityWcdma = cellInfoWcdma.getCellIdentity();
        data.accumulate("type", "WCDMA");
        data.accumulate("MCC", cellIdentityWcdma.getMcc());
        data.accumulate("MNC", cellIdentityWcdma.getMnc());
        data.accumulate("LAC", cellIdentityWcdma.getLac());
        data.accumulate("CID", cellIdentityWcdma.getCid());
        int dbm = cellInfoWcdma.getCellSignalStrength().getDbm();
        data.accumulate("dBm", dbm);
      }
      else if (cellInfo instanceof CellInfoLte) {
        CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
        CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();
        data.accumulate("type", "LTE");
        data.accumulate("MCC", cellIdentityLte.getMcc());
        data.accumulate("MNC", cellIdentityLte.getMnc());
        data.accumulate("TAC", cellIdentityLte.getTac());
        data.accumulate("CID", cellIdentityLte.getCi());
        data.accumulate("PCI", cellIdentityLte.getPci());
        int dbm = cellInfoLte.getCellSignalStrength().getDbm();
        data.accumulate("dBm", dbm);
      }
      else if (cellInfo instanceof CellInfoNr) {
        CellInfoNr cellInfoNr = (CellInfoNr) cellInfo;
        CellIdentityNr cellIdentityNr = (CellIdentityNr) cellInfoNr.getCellIdentity();
        data.accumulate("type", "NR");
        data.accumulate("MCC", cellIdentityNr.getMccString());
        data.accumulate("MNC", cellIdentityNr.getMncString());
        data.accumulate("TAC", cellIdentityNr.getTac());
        data.accumulate("CID", cellIdentityNr.getNci());
        data.accumulate("PCI", cellIdentityNr.getPci());
        int dbm = cellInfoNr.getCellSignalStrength().getDbm();
        data.accumulate("dBm", dbm);
      }
      else {
        Log.e(U.TAG,"Wrong cellInfo");
        mStatus="error";
        return err;
      }
      cellData=data;
      mStatus="available";
      return data;
    } 
    catch (JSONException e) { 
      Log.e(U.TAG,e.getMessage());
      mStatus="error";
      return err;
    }
  }
  
  private JSONObject getMockParams() {
    JSONObject data=new JSONObject();
    try {
      data.accumulate("type", "mock GSM");
      data.accumulate("MCC", 250);
      data.accumulate("MNC", 99);
      data.accumulate("LAC", 11002);
      data.accumulate("CID", 26953);
    }
    catch (JSONException e) {
      Log.e(U.TAG, e.getStackTrace().toString());
      return new JSONObject();
    }
    cellData=data;
    return data;
  }

}
