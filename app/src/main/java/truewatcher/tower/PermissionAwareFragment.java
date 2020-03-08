package truewatcher.tower;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.SparseArray;

interface PermissionReceiver {
  public void receivePermission(int reqCode, boolean isGranted);
}

public abstract class PermissionAwareFragment extends android.support.v4.app.Fragment {
  private SparseArray<PermissionReceiver> mPermissionReceivers = new SparseArray<PermissionReceiver>();

  @TargetApi(23)
  public void genericRequestPermission(String permCode, int reqCode, PermissionReceiver receiver) {
    mPermissionReceivers.put(reqCode, receiver);
    if (U.DEBUG) Log.d(U.TAG, "Requesting user...");
    requestPermissions(new String[]{permCode}, reqCode);
  }

  @Override
  public void onRequestPermissionsResult(int reqCode, String[] permissions, int[] grantResults) {
    boolean isGranted = ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED );
    if (U.DEBUG) Log.d(U.TAG,"grantResults length="+grantResults.length);
    mPermissionReceivers.get(reqCode).receivePermission(reqCode,isGranted);
  }
}
