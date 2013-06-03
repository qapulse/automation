/*
 * Copyright 2011 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.littleinc.MessageMe.actionbar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ProgressBar;

import com.littleinc.MessageMe.R;

/**
 * An extension of {@link ActionBarHelper} that provides Android 3.0-specific
 * functionality for
 * Honeycomb tablets. It thus requires API level 11.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ActionBarHelperHoneycomb extends ActionBarHelper {
    private Menu mOptionsMenu;

    private View mRefreshIndeterminateProgressView = null;

    protected ActionBarHelperHoneycomb(Activity activity) {
        super(activity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void setRefreshActionItemState(boolean refreshing) {
        // On Honeycomb, we can set the state of the refresh button by giving it
        // a custom
        // action view.
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                if (mRefreshIndeterminateProgressView == null) {
                    LayoutInflater inflater = (LayoutInflater) getActionBarThemedContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    mRefreshIndeterminateProgressView = inflater.inflate(
                            R.layout.actionbar_indeterminate_progress, null);

                    ProgressBar progressBar = (ProgressBar) mRefreshIndeterminateProgressView
                            .findViewById(R.id.actionbar_progress_bar);

                    if (progressBar != null) {

                        final int indicatorWidth = progressBar
                                .getIndeterminateDrawable() != null ? progressBar
                                .getIndeterminateDrawable().getIntrinsicWidth()
                                : -1;
                        final int indicatorHeight = progressBar
                                .getIndeterminateDrawable() != null ? progressBar
                                .getIndeterminateDrawable()
                                .getIntrinsicHeight() : -1;

                        if (indicatorWidth != -1 && indicatorHeight != -1) {
                            final int indicatorMargin = mActivity
                                    .getResources()
                                    .getDimensionPixelSize(
                                            R.dimen.action_bar_progress_indicator_margin);
                            LayoutParams layoutParams = new LayoutParams(
                                    indicatorWidth, indicatorHeight);
                            layoutParams.setMargins(indicatorMargin, 0,
                                    indicatorMargin, 0);
                            progressBar.setLayoutParams(layoutParams);
                        }
                    }
                }

                refreshItem.setVisible(true);
                refreshItem.setActionView(mRefreshIndeterminateProgressView);
            } else {
                refreshItem.setVisible(false);
                refreshItem.setActionView(null);
            }
        }
    }

    /**
     * Returns a {@link Context} suitable for inflating layouts for the action
     * bar. The implementation for this method in {@link ActionBarHelperICS}
     * asks the action bar for a themed context.
     */
    protected Context getActionBarThemedContext() {
        return mActivity;
    }

    @Override
    public void setMenuItemEnabled(MenuItem item, boolean enabled) {
        int itemId = item.getItemId();

        MenuItem menuItem = mOptionsMenu.findItem(itemId);

        if (menuItem != null) {
            menuItem.setEnabled(enabled);
        }
    }

    @Override
    public void hideMenuItem(MenuItem item) {
        int itemId = item.getItemId();

        MenuItem menuItem = mOptionsMenu.findItem(itemId);

        if (menuItem != null) {
            menuItem.setVisible(false);
        }

    }

    @Override
    public void showMenuitem(MenuItem item) {
        int itemId = item.getItemId();

        MenuItem menuItem = mOptionsMenu.findItem(itemId);

        if (menuItem != null) {
            menuItem.setVisible(true);
        }

    }

    @Override
    public void setHomeIcon(int resID) {
    }

    @Override
    public void disableHomeButton() {
    }
}
