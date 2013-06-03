package com.littleinc.MessageMe.ui;

import android.content.Intent;
import android.os.Bundle;

import com.coredroid.core.AppLauncher;
import com.coredroid.ui.CloseActivityClickListener;
import com.coredroid.ui.CoreActivity;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;

public class ShowAlertAndCloseAppActivity extends CoreActivity {

    public static final String EXTRA_ALERT_TITLE = "extra_alert_title";
    
    public static final String EXTRA_ALERT_MSG = "extra_alert_message";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String title = intent.getStringExtra(EXTRA_ALERT_TITLE);
        String msg = intent.getStringExtra(EXTRA_ALERT_MSG);
        
        LogIt.d(this, "onCreate", title, msg);
        setContentView(R.layout.show_alert_and_close_layout);
        
        UIUtil.alert(this, title, msg, new CloseActivityClickListener(this));
        
        // Once we've shown the message above, mark that a downgrade is no 
        // longer needed.  This is to ensure that the DatabaseHelper 
        // upgrade/downgrade gets run again if the user reinstalls a newer
        // version.
        //
        // This screen is just a warning, it does not try to lock the user
        // out of the app.  A user can exit this screen and launch the app 
        // again, which means new code will run with the old DB (which could
        // result in crashes or indeterminate behaviour).  A downgrade should
        // never happen in Production, so this is fine.
        MessageMeApplication.getPreferences().setDbNeedsDowngrade(false);
        MessageMeApplication.getState().sync();
    }
}
