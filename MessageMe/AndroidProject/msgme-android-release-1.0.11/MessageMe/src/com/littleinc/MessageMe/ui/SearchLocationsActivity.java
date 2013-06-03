package com.littleinc.MessageMe.ui;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeExternalAPI;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.SearchActivity;
import com.littleinc.MessageMe.net.FacebookData;
import com.littleinc.MessageMe.net.FacebookGraph;
import com.littleinc.MessageMe.net.FacebookPlace;
import com.littleinc.MessageMe.util.PlacesAdapter;
import com.littleinc.MessageMe.util.SearchManager;
import com.littleinc.MessageMe.util.SearchTextWatcher;
import com.littleinc.MessageMe.util.StringUtil;

/**
 * Class designed to handle the search 
 * of a specific nearby location given the 
 * current user location
 *
 */
public class SearchLocationsActivity extends SearchActivity implements
        SearchManager {

    private PlacesAdapter mPlacesAdapter;
    
    private double latitude;

    private double longitude;

    private FacebookGraph mFacebookGraph;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            
            if (msg == null) {
                LogIt.w(SearchLocationsActivity.class, 
                        "Ignore null message received by handler");
                return;
            }
            
            switch (msg.what) {
            case MessageMeConstants.UPDATE_SEARCH_MESSAGE:
                String terms = (String) msg.obj;
                LogIt.d(SearchLocationsActivity.class, "UPDATE_SEARCH_MESSAGE",
                        terms);
                
                if (StringUtil.isEmpty(terms)) {
                    mPlacesAdapter.clear();
                    isShowingDarkOverlay = true;
                    masterLayout.setBackgroundResource(0);
                    setVisible(R.id.emptyElement, false);
                } else {
                    doSearch(terms);
                }
                break;
            default:
                LogIt.w(SearchLocationsActivity.class, 
                        "Unexpected message received by handler",
                        msg.what);
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listView = (ListView) findViewById(R.id.list_view);

        searchBox.addTextChangedListener(new SearchTextWatcher(mHandler));

        mFacebookGraph = new FacebookGraph(
                MessageMeExternalAPI.FACEBOOK_ACCESS_TOKEN);

        latitude = getIntent().getDoubleExtra(
                MessageMeConstants.EXTRA_LATITUDE, -1);
        longitude = getIntent().getDoubleExtra(
                MessageMeConstants.EXTRA_LONGITUDE, -1);
        
        if(latitude == -1 || longitude == -1){
            Toast.makeText(this, R.string.location_unavailable,
                    Toast.LENGTH_LONG).show();
        }

        updateUI();

    }

    private void searchLocations(final String terms) {

        new BackgroundTask() {

            FacebookData<FacebookPlace> data;

            @Override
            public void work() {
                data = mFacebookGraph.getPlaces(terms, latitude, longitude);
            }

            @Override
            public void done() {
                isShowingDarkOverlay = false;
                masterLayout.setBackgroundResource(R.color.list_view_background);
                if (data != null && data.getList().size() > 0) {
                    mPlacesAdapter.clear();
                    updatePlaces(data);
                    setVisible(R.id.emptyElement, false);
                } else {
                    setVisible(R.id.emptyElement, true);
                }

            }
        };

    }

    /**
     * Method defined in the layout XML
     */
    @Override
    public void onSearch(View view) {
        LogIt.user(this, "User pressed on the search icon button");
        String terms = searchBox.getText().toString();
        if (!StringUtil.isEmpty(terms)) {
            UIUtil.hideKeyboard(searchBox);
            doSearch(terms);
        }
    }

    private void updatePlaces(FacebookData<FacebookPlace> data) {
        List<FacebookPlace> places = data.getList();
        mPlacesAdapter.addPlaces(places);
        updateUI();
    }

    private class onLocationClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            FacebookPlace selectedPlace = mPlacesAdapter.getItem(position);

            Intent intent = new Intent();
            intent.putExtra(MessageMeConstants.EXTRA_LOCATION, selectedPlace);
            setResult(RESULT_OK, intent);

            finish();

        }
    }

    @Override
    public void doSearch(String terms) {
        searchLocations(terms);
    }

    @Override
    public void updateUI() {
        if (listView.getAdapter() == null) {
            LogIt.i(this, "Image search GridView adapter null, set it");
            mPlacesAdapter = new PlacesAdapter(this);
            listView.setAdapter(mPlacesAdapter);
            listView.setOnItemClickListener(new onLocationClickListener());
        } else {
            mPlacesAdapter.notifyDataSetChanged();
        }
    };

}
