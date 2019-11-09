package truewatcher.tower;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class ConfirmationDialogFragment extends DialogFragment {
  // https://stackoverflow.com/questions/5393197/show-dialog-from-fragment  -- answer by EpicPandaForce
  public interface ConfirmationDialogReceiver {
    public void onConfirmationPositive(int id);//DialogFragment dialog
    public void onConfirmationNegative(int id);
  }

  private ConfirmationDialogReceiver mListener;
  private DialogFragment mMe=this;
  private int mId=0;
  private int mStringId=0;
  
  public ConfirmationDialogFragment() { super(); }

  @TargetApi(23)
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    try {
      //mListener = (ConfirmationDialogReceiver) context;
      mListener = (ConfirmationDialogReceiver) getTargetFragment();
    }
    catch (ClassCastException e) {
      throw new ClassCastException("Host activity must implement ConfirmationDialogReceiver");
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    mId=Integer.valueOf(this.getArguments().getString("actionId"));
    mStringId=Integer.valueOf(this.getArguments().getString("actionStringId"));
    //Log.i(U.TAG, "ConfirmationDialogFragment:"+"got args="+mId+"/"+ mStringId);
    if (mId == 0 || mStringId == 0) throw new U.RunException("ConfirmationDialogFragment:Missing arguments");

    android.support.v7.app.AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder
    .setMessage(R.string.are_you_sure)
    .setPositiveButton(mStringId, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        mListener.onConfirmationPositive(mId);
      }
    })
    .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        mListener.onConfirmationNegative(mId);
      }
    });
    return builder.create();
  }

}
