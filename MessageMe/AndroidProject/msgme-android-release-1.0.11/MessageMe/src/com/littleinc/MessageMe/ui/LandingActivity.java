package com.littleinc.MessageMe.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.coredroid.ui.CoreActivity;
import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.metrics.MMFunnelTracker;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.mobileapptracker.MobileAppTracker;

public class LandingActivity extends CoreActivity {

    private MobileAppTracker mobileAppTracker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MMFunnelTracker.getInstance().abacusOnceFunnel(
                MMFunnelTracker.SPLASH_FUNNEL,
                MessageMeConstants.ONE_WEEK_SECS, "show", null, null, null);

        // tick launches
        MMLocalData.getInstance().tickLaunchCount();

        // tracking installs
        mobileAppTracker = new MobileAppTracker(this, MessageMeConstants.JAMPP_ADVERTISER_ID,
                MessageMeConstants.JAMPP_ADVERTISER_KEY, false, false);

        // track install if we have no user and launch count is 0
        if (MessageMeApplication.getPreferences().getUser() == null
                && MMLocalData.getInstance().getLaunchCount() == 1) {
            LogIt.i(this, "tracking JAMPP install.");
            mobileAppTracker.trackInstall();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (MessageMeApplication.getCurrentUser() != null) {
            finish();
        }
    }

    @Override
    protected int getContentViewId() {
        return R.layout.landing_layout;
    }

    public void signUpClick(View view) {
        startActivity(DeviceUtil.checkSimState(this));
    }

    public void loginClick(View view) {
        Intent intent = new Intent(this, LogInActivity.class);
        startActivity(intent);
    }

}
