package com.coredroid.ui;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.coredroid.core.CoreApplication;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.ui.MessagingNotificationHelper;

public class CoreFragmentActivity extends FragmentActivity {
    protected Handler handler = new Handler();

    private boolean initFinished;

    protected MessagingNotificationHelper mNotificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNotificationHelper = new MessagingNotificationHelper(this);
    }

    /**
     * Allow sub classes to determine if the titlebar (or actionbar in
     * honeycomb+) should show. The default is true
     * 
     * @return
     */
    protected boolean hideTitlebar() {
        return true;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        if (!initFinished) {
            setFont(getFont());

            initFinished = true;
        }
    }

    /**
     * Show a short lengthed toast
     */
    protected void toast(int stringId) {
        toast(getString(stringId), Toast.LENGTH_SHORT);
    }

    protected void toast(int stringId, int duration) {
        toast(getString(stringId), duration);
    }

    /**
     * Show a short lengthed toast
     */
    protected void toast(String string) {
        toast(string, Toast.LENGTH_SHORT);
    }

    protected void toast(String string, int duration) {
        Toast.makeText(this, string, duration).show();
    }

    /**
     * Sets the font on all text views for the current activity
     */
    protected void setFont(Typeface font) {
        if (font == null) {
            return;
        }

        UIUtil.setFont(getContentPanel(), font);
    }

    /**
     * Gets the top level panel container for this activity
     */
    protected View getContentPanel() {

        return UIUtil.getContentPanel(this);
    }

    /**
     * The font to recursively set on all text views on the panel, null will use
     * default font. This implementation returns null
     */
    protected Typeface getFont() {
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNotificationHelper.attach(this);
        mNotificationHelper.registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CoreApplication.getState().sync();
        mNotificationHelper.unRegisterReceiver();
    }

    protected String getViewText(int label) {
        TextView view = ((TextView) findViewById(label));
        return view != null ? view.getText().toString() : "";
    }

    protected void setClickListener(int viewId, View.OnClickListener listener) {
        findViewById(viewId).setOnClickListener(listener);
    }

    protected void setClickListener(View root, int viewId,
            View.OnClickListener listener) {
        root.findViewById(viewId).setOnClickListener(listener);
    }

    protected void setViewText(int viewId, String text) {
        ((TextView) findViewById(viewId)).setText(text != null ? text : "");
    }

    protected void setVisible(int viewId, boolean visible) {
        findViewById(viewId).setVisibility(
                visible ? View.VISIBLE : View.INVISIBLE);
    }

    protected void setHidden(int viewId, boolean visible) {
        findViewById(viewId).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /*
     * @Override public boolean onKeyDown(int keyCode, KeyEvent event) { if
     * ((keyCode == KeyEvent.KEYCODE_BACK)) { if (handleBackButton()) { return
     * true; } } return super.onKeyDown(keyCode, event); }
     */

    /**
     * Define what to do when the back button is pushed. Convenience method to
     * override, default behavior is no-op
     */
    protected boolean handleBackButton() {
        return true;
    }

    protected ProgressDialog showProgressDialog(String message) {
        return UIUtil.showProgressDialog(this, message);
    }

    protected ProgressDialog showProgressDialog(int stringId) {
        return showProgressDialog(getString(stringId));
    }

}
