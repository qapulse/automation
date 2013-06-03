package com.littleinc.MessageMe.util;

import android.view.View;

/**
 * Public interface defined to commonize
 * methods in all the search activities
 *
 */
public interface SearchManager {

    public abstract void doSearch(String terms);

    public abstract void updateUI();
    
    public void onSearch(View view);

}
