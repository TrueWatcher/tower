package truewatcher.signaltrackwriter;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

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

public class CellInformer {
  
  private JSONObject cellData=new JSONObject();
  private String mStatus = "fresh";
  private FragmentActivity mActivity;

  public void bindAxtivity(FragmentActivity activity) { mActivity = activity; }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  public JSONObject getInfo() {
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
    return cellData; //.toString();
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
