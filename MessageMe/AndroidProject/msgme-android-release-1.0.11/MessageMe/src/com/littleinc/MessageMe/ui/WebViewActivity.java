package com.littleinc.MessageMe.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;

/**
 * WebView class used to display
 * the web content from the "Get Help" and "About"
 * buttons on the Settings page
 *
 */
@TargetApi(14)
public class WebViewActivity extends ActionBarActivity {

    private WebView webView;

    private int webCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        webCode = getIntent().getIntExtra(MessageMeConstants.EXTRA_URL, -1);

        webView = (WebView) findViewById(R.id.web_view_content);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true); //Hack needed to open the mobile.twitter page
        webView.setWebChromeClient(new CustomWebClient());
        openUrl();

    }

    /**
     * Opens a URL depending of the action code registered in the
     * EXTRA_URL
     */
    private void openUrl() {
        String url = "";

        switch (webCode) {
        case MessageMeConstants.EXTRA_FAQ_CODE:
            url = getString(R.string.faq_url);
            setTitle(getString(R.string.faq));
            break;
        case MessageMeConstants.EXTRA_PRIVACY_POLICY_CODE:
            url = getString(R.string.privacy_policy_url);
            setTitle(getString(R.string.privacy_policy));
            break;
        case MessageMeConstants.EXTRA_SERVICE_STATUS_CODE:
            url = getString(R.string.service_status_url);
            setTitle(getString(R.string.service_status));
            break;
        case MessageMeConstants.EXTRA_TERMS_OF_SERVICE_CODE:
            url = getString(R.string.terms_of_service_url);
            setTitle(getString(R.string.terms_service));
            break;

        default:
            break;
        }
        LogIt.d(this, "Openning website: ", url);
        webView.loadUrl(url);
        
        // Adding this hack only for the service status page
        // For some reason (might be due javascript) the twitter page opens in a 
        // new browser on Android 4.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
                && webCode == MessageMeConstants.EXTRA_SERVICE_STATUS_CODE) {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Custom web client, used to hide the progressBar
     * when the loading of the page is complete
     *
     */
    private class CustomWebClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int progress) {

            if (progress == 100) {
                setVisible(R.id.web_view_progressbar, false);
            }

            super.onProgressChanged(view, progress);
        }
    }

}
