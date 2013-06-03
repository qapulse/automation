package com.littleinc.MessageMe;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.provider.Settings;

public class ClickListenerShowNetworkSettings implements OnClickListener {

    Context mContext;
    
    public ClickListenerShowNetworkSettings(Context context) {
        mContext = context;
    }
    
    @Override
    public void onClick(DialogInterface dialog, int which) {
        Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        mContext.startActivity(intent);
    }
}
