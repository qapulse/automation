package com.littleinc.MessageMe.ui;

import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeExternalAPI;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarMapActivity;
import com.littleinc.MessageMe.net.FacebookData;
import com.littleinc.MessageMe.net.FacebookGraph;
import com.littleinc.MessageMe.net.FacebookPlace;
import com.littleinc.MessageMe.util.PlacesAdapter;

@TargetApi(14)
public class GetLocationActivity extends ActionBarMapActivity {

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private static final float MIN_DISTANCE = 70.0F;

    private static final int SEARCH_LOCATION_RESULT_CODE = 64;

    private static final int MIN_TIME = 20000;

    private static final int LIMIT = 10;

    private MapView mMapView;

    private Criteria fineCriteria;

    private Criteria coarseCriteria;

    private ListView mLocations;

    private List<Overlay> mOverlays;

    private RelativeLayout footerView;

    private Location currentBestLocation;

    private FacebookGraph mFacebookGraph;

    private PlacesAdapter mPlacesAdapter;

    private TextView locationLoadMoreLabel;

    private LocationManager mLocationManager;

    private FacebookData<FacebookPlace> currentPlaces;

    private PlacesItemizedOverlay mCurrentLocationOverlay;

    // private PlacesItemizedOverlay mPlacesOverlays;

    private ProgressBar mLoadProgress;

    public String nextPage;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.select_location);
        setTitle(R.string.select_location_title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        mMapView = (MapView) findViewById(R.id.mapview);
        mLoadProgress = (ProgressBar) findViewById(R.id.load_progress);
        mLocations = (ListView) findViewById(R.id.select_location_list);

        fineCriteria = new Criteria();
        fineCriteria.setPowerRequirement(Criteria.POWER_MEDIUM);
        fineCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        fineCriteria.setCostAllowed(true);

        coarseCriteria = new Criteria();
        coarseCriteria.setPowerRequirement(Criteria.POWER_MEDIUM);
        coarseCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
        coarseCriteria.setCostAllowed(true);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mFacebookGraph = new FacebookGraph(
                MessageMeExternalAPI.FACEBOOK_ACCESS_TOKEN);
        mCurrentLocationOverlay = new PlacesItemizedOverlay(getResources()
                .getDrawable(R.drawable.common_map_maker_self), this);

        mMapView.setBuiltInZoomControls(false);
        mMapView.displayZoomControls(false);

        mOverlays = mMapView.getOverlays();
        mOverlays.add(mCurrentLocationOverlay);

        mPlacesAdapter = new PlacesAdapter(this);

        footerView = (RelativeLayout) LayoutInflater.from(this).inflate(
                R.layout.select_location_load_more, null);
        footerView.setOnClickListener(loadMorePlacesListener);
        locationLoadMoreLabel = (TextView) footerView
                .findViewById(R.id.location_load_more_label);

        mLocations.addHeaderView(LayoutInflater.from(this).inflate(
                R.layout.select_location_current, null));
        mLocations.addFooterView(footerView);
        mLocations.setAdapter(mPlacesAdapter);
        mLocations.setOnItemClickListener(onLocationClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();

        menuInflater.inflate(R.menu.search_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case R.id.menu_search_btn:
            openLocationSearch();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openLocationSearch() {
        LogIt.user(this, "Pressed on the search actionbar button");
        Intent intent = new Intent(this, SearchLocationsActivity.class);
        if (currentBestLocation != null) {
            intent.putExtra(MessageMeConstants.EXTRA_LATITUDE,
                    currentBestLocation.getLatitude());
            intent.putExtra(MessageMeConstants.EXTRA_LONGITUDE,
                    currentBestLocation.getLongitude());
        }
        startActivityForResult(intent, SEARCH_LOCATION_RESULT_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case SEARCH_LOCATION_RESULT_CODE:
            switch (resultCode) {
            case RESULT_OK:
                FacebookPlace location = (FacebookPlace) data
                        .getSerializableExtra(MessageMeConstants.EXTRA_LOCATION);
                LogIt.d(this, "Selected place: " + location.getName());
                Intent intent = new Intent();
                intent.putExtra(MessageMeConstants.EXTRA_LOCATION, location);
                setResult(RESULT_OK, intent);
                finish();
                break;

            }
            break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void registerListener() {
        Location bestLastKnownLocation = null;
        String bestCoarseProvider = mLocationManager.getBestProvider(
                coarseCriteria, true);
        String bestProvider = mLocationManager.getBestProvider(fineCriteria,
                false);
        String bestAvailableProvider = mLocationManager.getBestProvider(
                fineCriteria, true);

        if (isSameProvider(bestProvider, bestAvailableProvider)) {
            mLocationManager.requestLocationUpdates(bestAvailableProvider,
                    MIN_TIME, MIN_DISTANCE,
                    bestAvailableProviderlocationListener);
            bestLastKnownLocation = mLocationManager
                    .getLastKnownLocation(bestAvailableProvider);

            if (bestCoarseProvider != null
                    && !isSameProvider(bestAvailableProvider,
                            bestCoarseProvider)) {
                mLocationManager
                        .requestLocationUpdates(bestCoarseProvider, MIN_TIME,
                                MIN_DISTANCE, networkProviderlocationListener);

                currentBestLocation = isBetterLocation(
                        mLocationManager
                                .getLastKnownLocation(bestCoarseProvider),
                        bestLastKnownLocation) ? mLocationManager
                        .getLastKnownLocation(bestCoarseProvider)
                        : bestLastKnownLocation;
            } else {
                currentBestLocation = bestLastKnownLocation;
            }
        } else {
            bestLastKnownLocation = mLocationManager
                    .getLastKnownLocation(bestProvider);

            if (bestAvailableProvider != null) {
                mLocationManager.requestLocationUpdates(bestProvider, MIN_TIME,
                        MIN_DISTANCE, bestAvailableProviderlocationListener);
                currentBestLocation = isBetterLocation(
                        mLocationManager
                                .getLastKnownLocation(bestAvailableProvider),
                        bestLastKnownLocation) ? mLocationManager
                        .getLastKnownLocation(bestAvailableProvider)
                        : bestLastKnownLocation;

                if (bestCoarseProvider != null) {
                    mLocationManager.requestLocationUpdates(bestCoarseProvider,
                            MIN_TIME, MIN_DISTANCE,
                            networkProviderlocationListener);

                    currentBestLocation = isBetterLocation(
                            mLocationManager
                                    .getLastKnownLocation(bestCoarseProvider),
                            currentBestLocation) ? mLocationManager
                            .getLastKnownLocation(bestCoarseProvider)
                            : currentBestLocation;
                }
            } else {
                if (bestCoarseProvider != null) {
                    mLocationManager.requestLocationUpdates(bestCoarseProvider,
                            MIN_TIME, MIN_DISTANCE,
                            networkProviderlocationListener);

                    currentBestLocation = isBetterLocation(
                            mLocationManager
                                    .getLastKnownLocation(bestCoarseProvider),
                            bestLastKnownLocation) ? mLocationManager
                            .getLastKnownLocation(bestCoarseProvider)
                            : bestLastKnownLocation;
                } else {
                    currentBestLocation = bestLastKnownLocation;
                }
            }
        }

        if (currentBestLocation != null) {
            updateCurrentLocation(currentBestLocation);
        } else if (bestAvailableProvider == null && bestCoarseProvider == null) {
            currentBestLocation = MessageMeApplication.getPreferences()
                    .getLastKnownLocation();

            if (currentBestLocation != null) {
                updateCurrentLocation(currentBestLocation);
            }
        }
        if (currentBestLocation == null) {
            Toast.makeText(this, R.string.location_unavailable,
                    Toast.LENGTH_LONG).show();
        }
    }

    /** 
     * Determines whether one Location reading is better than the current 
     * Location fix
     * 
     * This code is taken almost verbatim from the Android developer website:
     *   http://developer.android.com/guide/topics/location/strategies.html
     * 
     * @param newLocation The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which 
     *                            you want to compare the new one
     */
    protected boolean isBetterLocation(Location newLocation,
            Location currentBestLocation) {

        if (newLocation == null) {
            LogIt.w(this, "isBetterLocation called with null newLocation");
        } else {

            if (currentBestLocation == null) {
                // A new location is always better than no location
                return true;
            }

            // Check whether the new location fix is newer or older
            long timeDelta = newLocation.getTime()
                    - currentBestLocation.getTime();
            LogIt.d(this, "timeDelta", timeDelta);

            boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
            boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
            boolean isNewer = timeDelta > 0;

            // If it's been more than two minutes since the current location, use the new location
            // because the user has likely moved
            if (isSignificantlyNewer) {
                LogIt.d(this, "New location is significantly newer");
                return true;
                // If the new location is more than two minutes older, it must be worse
            } else if (isSignificantlyOlder) {
                LogIt.d(this, "New location is significantly older");
                return false;
            }

            // Check whether the new location fix is more or less accurate
            int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation
                    .getAccuracy());
            boolean isLessAccurate = accuracyDelta > 0;
            boolean isMoreAccurate = accuracyDelta < 0;
            boolean isSignificantlyLessAccurate = accuracyDelta > 200;

            // Check if the old and new location are from the same provider
            boolean isFromSameProvider = isSameProvider(
                    newLocation.getProvider(),
                    currentBestLocation.getProvider());

            // Determine location quality using a combination of timeliness and accuracy
            if (isMoreAccurate) {
                LogIt.d(this, "New location is more accurate than previous one");
                return true;
            } else if (isNewer && !isLessAccurate) {
                LogIt.d(this,
                        "New location is newer and has at least same accuracy");
                return true;
            } else if (isNewer && !isSignificantlyLessAccurate
                    && isFromSameProvider) {
                LogIt.d(this,
                        "New location is newer and is not significantly less accurate, and is from same provider");
                return true;
            }
        }

        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }

        return provider1.equals(provider2);
    }

    private void unRegisterListeners() {
        mLocationManager.removeUpdates(networkProviderlocationListener);
        mLocationManager.removeUpdates(bestAvailableProviderlocationListener);
    }

    public void doAccept(View view) {
        Intent intent = getIntent();

        intent.putExtra("latitude", currentBestLocation.getLatitude());
        intent.putExtra("longitude", currentBestLocation.getLongitude());

        setResult(RESULT_OK, intent);
        finish();
    }

    public void doCancel(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void doSelectCurrentLocation(View view) {
        if (currentBestLocation != null) {
            FacebookPlace currentPlace = new FacebookPlace();

            currentPlace.setLocation(new FacebookPlace.Location(
                    currentBestLocation.getLatitude(), currentBestLocation
                            .getLongitude()));
            currentPlace.setName(getString(R.string.select_location_current));

            Intent intent = new Intent();

            intent.putExtra(MessageMeConstants.EXTRA_LOCATION, currentPlace);

            setResult(RESULT_OK, intent);
            finish();
        } else {
            Toast.makeText(this, R.string.select_location_getting_location,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void doLoadMore(View view) {
        if (currentPlaces != null) {
            new BackgroundTask() {
                FacebookData<FacebookPlace> data;

                @Override
                public void work() {
                    data = mFacebookGraph.getPlaces(currentPlaces.getPaging()
                            .getNext());
                }

                @Override
                public void done() {
                    if (data != null) {
                        updatePlaces(data);
                    }
                }
            };
        }
    }

    @Override
    protected void onPause() {
        unRegisterListeners();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerListener();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (currentBestLocation != null) {
            outState.putDouble("latitude", currentBestLocation.getLatitude());
            outState.putDouble("longitude", currentBestLocation.getLongitude());
        }

        super.onSaveInstanceState(outState);
    }

    private void updateCurrentLocation(Location location) {
        currentBestLocation = location;
        refreshPlaces();

        if (mPlacesAdapter != null) {
            mPlacesAdapter.setLocation(location);
            mPlacesAdapter.clear();
        }

        MapController mc = mMapView.getController();

        GeoPoint currentGeoPoint = new GeoPoint(
                (int) (location.getLatitude() * 1E6),
                (int) (location.getLongitude() * 1E6));

        mc.setZoom(17);
        mc.setCenter(currentGeoPoint);

        OverlayItem overlay = new OverlayItem(currentGeoPoint,
                getString(R.string.select_location_current), "");

        mCurrentLocationOverlay.clearAll();
        mCurrentLocationOverlay.addOverlay(overlay);
    }

    private void refreshPlaces() {
        mLocations.setVisibility(View.INVISIBLE);
        mLoadProgress.setVisibility(View.VISIBLE);

        new BackgroundTask() {
            FacebookData<FacebookPlace> data;

            @Override
            public void work() {
                data = mFacebookGraph.getPlaces(
                        currentBestLocation.getLatitude(),
                        currentBestLocation.getLongitude(), LIMIT);
            }

            @Override
            public void done() {
                mLocations.setVisibility(View.VISIBLE);
                mLoadProgress.setVisibility(View.INVISIBLE);

                if (data != null) {
                    mPlacesAdapter.clear();

                    updatePlaces(data);
                    nextPage = data.getPaging().getNext();
                }

                if (nextPage != null) {
                    footerView.setVisibility(View.VISIBLE);
                    locationLoadMoreLabel.setVisibility(View.VISIBLE);
                } else {
                    footerView.setVisibility(View.GONE);
                }
            }
        };
    }

    private OnClickListener loadMorePlacesListener = new OnClickListener() {

        @Override
        public void onClick(final View v) {
            locationLoadMoreLabel.setVisibility(View.INVISIBLE);

            new BackgroundTask() {

                FacebookData<FacebookPlace> data;

                @Override
                public void work() {
                    data = mFacebookGraph.getPlaces(nextPage);
                }

                @Override
                public void done() {
                    if (data != null) {
                        updatePlaces(data);
                        nextPage = data.getPaging().getNext();
                    }

                    if (nextPage != null) {
                        locationLoadMoreLabel.setVisibility(View.VISIBLE);
                    } else {
                        v.setVisibility(View.GONE);
                        mPlacesAdapter.notifyDataSetChanged();
                    }
                }
            };
        }
    };

    private void updatePlaces(FacebookData<FacebookPlace> data) {
        List<FacebookPlace> places = data.getList();
        mPlacesAdapter.addPlaces(places);
    }

    private LocationListener bestAvailableProviderlocationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            LogIt.v(this, provider + " / " + status + " / " + extras);
        }

        @Override
        public void onProviderEnabled(String provider) {
            LogIt.v(this, provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            LogIt.v(this, provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            LogIt.v(this, location);

            if (isBetterLocation(location, currentBestLocation)) {
                MessageMeApplication.getPreferences().setLastKnownLocation(
                        location);
                updateCurrentLocation(location);

                // Removing location updates after obtain a valid user location
                mLocationManager
                        .removeUpdates(bestAvailableProviderlocationListener);
            }
        }
    };

    private LocationListener networkProviderlocationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            LogIt.v(this, provider + " / " + status + " / " + extras);
        }

        @Override
        public void onProviderEnabled(String provider) {
            LogIt.v(this, provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            LogIt.v(this, provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            LogIt.v(this, location);

            if (isBetterLocation(location, currentBestLocation)) {
                MessageMeApplication.getPreferences().setLastKnownLocation(
                        location);
                updateCurrentLocation(location);

                // Removing location updates after obtain a valid user location
                mLocationManager.removeUpdates(networkProviderlocationListener);
            }
        }
    };

    private OnItemClickListener onLocationClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            if (position != 0) {
                FacebookPlace selectedPlace = mPlacesAdapter
                        .getItem(position - 1);

                Intent intent = new Intent();
                intent.putExtra(MessageMeConstants.EXTRA_LOCATION,
                        selectedPlace);
                setResult(RESULT_OK, intent);

                finish();
            }
        }
    };
}
