package com.littleinc.MessageMe;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class ClickListenerCloseDialog implements OnClickListener {

    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
}