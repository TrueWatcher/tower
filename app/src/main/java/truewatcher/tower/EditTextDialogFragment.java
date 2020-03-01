package truewatcher.tower;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

public class EditTextDialogFragment extends DialogFragment {
  // https://stackoverflow.com/questions/5393197/show-dialog-from-fragment  -- answer by EpicPandaForce
  public interface EditTextDialogReceiver {
    public void onEditTextPositive(int id, String text);
  }

  private EditTextDialogReceiver mListener;
  private DialogFragment mMe=this;
  private int mId=0;
  private int mStringId=0;
  private String mText="";
  private EditText mInput; 
  
  public EditTextDialogFragment() { super(); }

  @TargetApi(23)
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    try {
      //mListener = (ConfirmationDialogReceiver) context;
      mListener = (EditTextDialogReceiver) getTargetFragment();
    }
    catch (ClassCastException e) {
      throw new ClassCastException("Host activity must implement EditTextDialogReceiver");
    }
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v=super.onCreateView(inflater,container, savedInstanceState);
    return v;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    mId=Integer.valueOf(this.getArguments().getString("actionId"));
    mStringId=Integer.valueOf(this.getArguments().getString("actionStringId"));
    mText=this.getArguments().getString("text");
    if (mId == 0 || mStringId == 0 || mText == null) throw new U.RunException("EditTextDialogFragment:Missing arguments");

    AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
    builder.setMessage(mStringId);
    // https://stackoverflow.com/questions/18799216/how-to-make-a-edittext-box-in-a-dialog
    mInput = new EditText(getActivity());  
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                          LinearLayout.LayoutParams.MATCH_PARENT,
                          LinearLayout.LayoutParams.MATCH_PARENT);
    mInput.setLayoutParams(lp);
    if (mText.isEmpty()) mInput.setHint("<enter text>");
    mInput.setText(mText);
    builder.setView(mInput);
    
    builder
    .setPositiveButton(R.string.action_done, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        mText=mInput.getText().toString();
        if (U.DEBUG) Log.d(U.TAG,"EditTextDialogFragment:"+"Action confirmed by user, text="+mText);
        mListener.onEditTextPositive(mId, mText);
      }
    })
    .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        //mListener.onConfirmationNegative(mId);
        if (U.DEBUG) Log.d(U.TAG,"EditTextDialogFragment:"+"Action canceled by user");
      }
    });
    return builder.create();
  }

}
