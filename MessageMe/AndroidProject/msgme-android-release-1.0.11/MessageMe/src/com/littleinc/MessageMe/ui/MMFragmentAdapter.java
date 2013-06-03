package com.littleinc.MessageMe.ui;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import com.coredroid.util.LogIt;

/**
 * A custom PagerAdapter is required to ensure the ViewPager does not display
 * old fragments after an orientation change.  Users of this class should call
 * {@link MMFragmentAdapter#removePages()} when the old fragments should be
 * removed. 
 */
public class MMFragmentAdapter extends FragmentStatePagerAdapter {

    private List<Fragment> mFragments;

    public MMFragmentAdapter(FragmentManager fm, List<Fragment> fragments)
    {
        super(fm);
        mFragments = fragments;
    }

    @Override
    public Fragment getItem(int position)
    {
        return mFragments.get(position);
    }

    @Override
    public int getCount()
    {
        return mFragments.size();
    }

    /**
     * Workaround to ensure old fragments get removed from the view when the
     * adapter is changed.  Source:
     *   http://stackoverflow.com/a/10399127/112705
     */
    @Override
    public int getItemPosition(Object object){
        return PagerAdapter.POSITION_NONE;
    }
    
    public void removePages() {
        LogIt.d(this, "Clear out old fragments");
        
        if (mFragments != null) {            
            mFragments.clear();
        }
        notifyDataSetChanged();
    }
}