package truewatcher.tower;

import org.json.JSONException;
import org.json.JSONObject;
import android.Manifest;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

public class CellPointFetcher extends PointFetcher implements PermissionReceiver,CellDataReceiver,HttpReceiver {

  private CellResolver mCellResolver;
  private CellInformer mCellInformer = new CellInformer();

  @Override
  protected String getPermissionType() { return Manifest.permission.ACCESS_FINE_LOCATION; }

  @Override
  protected int getPermissionCode() { return 1; }

  @RequiresApi(api = Build.VERSION_CODES.Q)
  @Override
  public void afterLocationPermissionOk() {
    mCellInformer.bindActivity(mActivity);
    mCellInformer.requestCellInfos(this);
  }

  public void onCellDataObtained(JSONObject cellData) {
    mStatus = mCellInformer.getStatus();
    if (mStatus.equals("forbidden")) {
      mPi.addProgress(mActivity.getResources().getString(R.string.permissionfailure));
      return;
    }
    if (mStatus.indexOf("unsupported") == 0) {
      mPi.addProgress(mStatus);
      return;
    }
    if (! mStatus.equals("available") && ! mStatus.equals("mocking") && ! mStatus.equals("noService")) {
      mStatus="error";
      mPi.addProgress("failed to get cell info");
      return;
    }

    mPoint=new Point("cell");
    mPoint.cellData=cellData.toString();
    mPi.showData( JsonHelper.filterQuotes(mPoint.cellData) );
    if (shouldNotResolve()) {
      onPointavailable(mPoint);
      return;
    }
    mPi.addProgress("trying to get location...");
    startResolveCell();
  }

  private boolean shouldNotResolve() {
    if ("none".equals( MyRegistry.getInstance().get("cellResolver") ) ) {
      mPi.addProgress("location service is off (see Settings)");
      return true;
    }
    //if (U.DEBUG) Log.i(U.TAG,"network on:"+U.isNetworkOn(mActivity));
    if (! U.isNetworkOn(mActivity)) {
      mPi.addProgress("no internet");
      return true;
    }
    return false;
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
    JSONObject cellData = new JSONObject();

    try {
      cellData = new JSONObject(mPoint.cellData);
    }
    catch (JSONException e) {
      Log.e(U.TAG,"Wrong point.cellData");
      Log.e(U.TAG,e.getMessage());
      mPi.addProgress("Wrong point.cellData");
      onPointavailable(mPoint);
      return;
    }
    try {
      if ( ! cellData.has("CID") || "".equals(cellData.optString("CID"))) {
        throw new U.DataException("No cell id");
      }
      mCellResolver = CellResolverFactory.getResolver(MyRegistry.getInstance().get("cellResolver"));
      resolverUri = mCellResolver.makeResolverUri(cellData);
      reqData = mCellResolver.makeResolverData(cellData);
    }
    catch (U.DataException e) {
      Log.e(U.TAG,e.getMessage());
      mStatus="Failure:"+e.getMessage();
      mPi.addProgress(mStatus);
      onPointavailable(mPoint);
      return;
    }
    if (U.DEBUG) Log.i(U.TAG,"startResolveCell:"+"About to query "+resolverUri+"\n data="+reqData);
    AsyncTask<String, Void, String> req = mCellResolver.getRequestTask(this);
    req.execute(resolverUri,reqData);
  }

  @Override
  public void onHttpReceived(String response) {
    if (U.DEBUG) Log.i(U.TAG,"onHttpReceived:"+"Response:"+response);
    try {
      JSONObject resolvedData = mCellResolver.getResolvedData(response);
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

}
