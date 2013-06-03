package com.littleinc.MessageMe;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.coredroid.util.UIUtil;

/**
 * Base class for all the search activities
 * will contain shared variables/methods/functionalities.
 *
 */
public class SearchActivity extends MessageMeActivity {

    protected ListView listView;

    protected EditText searchBox;

    protected RelativeLayout masterLayout;

    protected boolean isShowingDarkOverlay = true;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_list_layout);

        masterLayout = (RelativeLayout) findViewById(R.id.master_layout);
        listView = (ListView) findViewById(R.id.list_view);
        searchBox = (EditText) findViewById(R.id.search_box);
        gestureDetector = new GestureDetector(this, new TapListener());
        
        // Always show the keyboard when launching search
        UIUtil.showKeyboard(searchBox);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UIUtil.hideKeyboard(searchBox);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private class TapListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (isShowingDarkOverlay) {
                finish();
            }
            return super.onSingleTapConfirmed(e);
        }
    }

}
