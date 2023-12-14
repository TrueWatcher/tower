package truewatcher.signaltrackwriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import android.telephony.CellInfoTdscdma;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthTdscdma;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CellInformer___ {

  private JSONObject cellData = new JSONObject();
  private String mStatus = "fresh";
  private FragmentActivity mActivity;
  public static final List<String> CELL_PARAMS = Collections.unmodifiableList(Arrays.asList(
      new String[] {"type","MCC","MNC","LAC","TAC","CID","PCI"}
  ));

  public void bindAxtivity(FragmentActivity activity) { mActivity = activity; }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  public JSONObject getInfo() {
    if (U.DEBUG) Log.d(U.TAG,"CellInformer:"+"getInfo here");
    TelephonyManager tm = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
    List<CellInfo> cellInfos = new ArrayList<>();
    try {
      // the permission is checked in PointFetcher
      cellInfos = tm.getAllCellInfo();
      // also https://stackoverflow.com/questions/61075598/what-is-proper-usage-of-requestcellinfoupdate
    }
    catch (SecurityException e) {
      //throw new U.RunException(e.getMessage());
      mStatus="forbidden";
      return cellData;//.toString();
    }

    if (null == cellInfos || cellInfos.size() == 0) {
      if (U.DEBUG) Log.d(U.TAG,"got null cell info");
      //cellData=getMockParams();
      //mStatus="mocking";
      cellData=getNoService();
      mStatus="noService";
    }
    else {
      int cellCount = cellInfos.size();
      if (U.DEBUG) Log.d(U.TAG,"got "+cellCount+" cell infos");
      if (U.DEBUG) Log.d(U.TAG,"0th cellInfo is registered:"+cellInfos.get(0).isRegistered()+
          ", cellInfos registered:"+countRegisteres(cellInfos));
      cellData = getMyCellParams(cellInfos.get(0));
    }
    return cellData;//.toString();
  }

  private int countRegisteres(List<CellInfo> cellInfos) {
    int found = 0;
    for (CellInfo c : cellInfos) {
      if (c.isRegistered()) found +=1;
    }
    return found;
  }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  private JSONObject getMyCellParams(CellInfo cellInfo) {
    JSONObject data=new JSONObject();
    JSONObject err=new JSONObject();
    try {
      if (U.classHasMethod(CellInfo.class, "getTimeStamp")) {
        int age = (int) (cellInfo.getTimeStamp() - System.currentTimeMillis() / 1000);
        if (age >= 0) data.accumulate("age", age);
      }
      if (U.classHasMethod(CellInfo.class, "getCellConnectionStatus")) {
        int s = cellInfo.getCellConnectionStatus();
        int isPrimary = s == CellInfo.CONNECTION_PRIMARY_SERVING ? 1 : 0;
        data.accumulate("primary", isPrimary);
      }
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
      else if (cellInfo instanceof CellInfoWcdma) {
        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo;
        CellIdentityWcdma cellIdentityWcdma = cellInfoWcdma.getCellIdentity();
        data.accumulate("type", "WCDMA");
        data.accumulate("MCC", cellIdentityWcdma.getMcc());
        data.accumulate("MNC", cellIdentityWcdma.getMnc());
        data.accumulate("LAC", cellIdentityWcdma.getLac());
        data.accumulate("CID", cellIdentityWcdma.getCid());
        int dbm = cellInfoWcdma.getCellSignalStrength().getDbm();
        // Get the RSCP as dBm value -120..-24dBm or UNAVAILABLE
        data.accumulate("dBm", dbm);
      }
      else if (U.classExists("android.telephony.CellInfoLte") && cellInfo instanceof CellInfoLte) {
        CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
        CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();
        data.accumulate("type", "LTE");
        data.accumulate("MCC", cellIdentityLte.getMcc());
        data.accumulate("MNC", cellIdentityLte.getMnc());
        data.accumulate("TAC", cellIdentityLte.getTac());
        data.accumulate("CID", cellIdentityLte.getCi());
        data.accumulate("PCI", cellIdentityLte.getPci());
        CellSignalStrengthLte ss = (CellSignalStrengthLte) cellInfoLte.getCellSignalStrength();
        if (U.classHasMethod(CellSignalStrengthLte.class, "getDbm")) {
          data.accumulate("dBm", ss.getDbm());
        }
        if (U.classHasMethod(CellSignalStrengthLte.class, "getRsrp")) {
          data.accumulate("RSRP", ss.getRsrp());
        }
        if (U.classHasMethod(CellSignalStrengthLte.class, "getRssi")) {
          data.accumulate("RSSI", ss.getRssi());
        }
      }
      else if (U.classExists("android.telephony.CellInfoNr") && cellInfo instanceof CellInfoNr) {
        CellInfoNr cellInfoNr = (CellInfoNr) cellInfo;
        CellIdentityNr cellIdentityNr = (CellIdentityNr) cellInfoNr.getCellIdentity();
        data.accumulate("type", "NR");
        data.accumulate("MCC", cellIdentityNr.getMccString());
        data.accumulate("MNC", cellIdentityNr.getMncString());
        data.accumulate("TAC", cellIdentityNr.getTac());
        data.accumulate("CID", cellIdentityNr.getNci());
        data.accumulate("PCI", cellIdentityNr.getPci());
        CellSignalStrengthNr ss = (CellSignalStrengthNr)  cellInfoNr.getCellSignalStrength();
        data.accumulate("dBm", ss.getDbm());
        // Get the SS-RSRP as dBm value -140..-44dBm or UNAVAILABLE
        if (U.classHasMethod(CellSignalStrengthNr.class, "getCsiRsrp")) {
          data.accumulate("CsiRSRP", ss.getCsiRsrp());
        }
        if (U.classHasMethod(CellSignalStrengthNr.class, "getSsRsrp")) {
          data.accumulate("SsRSRP", ss.getSsRsrp());
        }
        if (U.classHasMethod(CellSignalStrengthNr.class, "getCsiRsrq")) {
          data.accumulate("CsiRSRQ", ss.getCsiRsrq ());
        }
        if (U.classHasMethod(CellSignalStrengthNr.class, "getSsRsrq")) {
          data.accumulate("SsRSRQ", ss.getSsRsrq());
        }
        if (U.classHasMethod(CellSignalStrengthNr.class, "getCsiRsrq")) {
          data.accumulate("CsiRSRQ", ss.getCsiRsrq ());
        }
        if (U.classHasMethod(CellSignalStrengthNr.class, "getSsSinr")) {
          data.accumulate("SsSINR", ss.getSsSinr());
        }
        if (U.classHasMethod(CellSignalStrengthNr.class, "getCsiSinr")) {
          data.accumulate("CsiSINR", ss.getCsiSinr());
        }
      }
      else if (U.classExists("android.telephony.CellInfoTdscdma") && cellInfo instanceof CellInfoTdscdma) {
        data.accumulate("type", "TDSCDMA");
        CellInfoTdscdma cellInfoTdscdma = (CellInfoTdscdma) cellInfo;
        CellSignalStrengthTdscdma ss = (CellSignalStrengthTdscdma)  cellInfoTdscdma.getCellSignalStrength();
        data.accumulate("dBm", ss.getDbm());
      }
      else if (U.classExists("android.telephony.CellInfoCdma") && cellInfo instanceof CellInfoCdma) {
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
  private JSONObject getNoService() {
    JSONObject data=new JSONObject();
    try {
      data.accumulate("type", "noService");
      data.accumulate("dBm", -999);
    }
    catch (JSONException e) {
      Log.e(U.TAG, e.getStackTrace().toString());
      return new JSONObject();
    }
    cellData=data;
    return data;
  }

  private JSONObject getMockParams() {
    JSONObject data=new JSONObject();
    try {
      data.accumulate("type", "mock GSM");
      data.accumulate("dBm", -60);
    }
    catch (JSONException e) {
      Log.e(U.TAG, e.getStackTrace().toString());
      return new JSONObject();
    }
    cellData=data;
    return data;
  }
}
