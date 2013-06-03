package com.littleinc.MessageMe.ui;

import java.util.List;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeExternalAPI;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.LocationMessage;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.MessageUtil;
import com.littleinc.MessageMe.widget.LocationDetailOverlay;

public class MapDetailScreenActivity extends MapActivity {

    private TextView actionBtn;

    private ImageButton closeBtn;

    private ImageButton exportsBtn;

    private MapView mMapView;

    private List<Overlay> mOverlays;

    private LocationMessage currentMessage;

    private Handler mHandler = new Handler();

    private LocationDetailOverlay locationOverlay;

    private PlacesItemizedOverlay mCurrentLocationOverlay;

    private float latitude;

    private float longitude;    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_detail_screen);

        latitude = getIntent().getFloatExtra(MessageMeConstants.EXTRA_LATITUDE,
                0.0f);
        longitude = getIntent().getFloatExtra(
                MessageMeConstants.EXTRA_LONGITUDE, 0.0f);

        actionBtn = (TextView) findViewById(R.id.detail_screen_action);
        closeBtn = (ImageButton) findViewById(R.id.detail_screen_close);
        exportsBtn = (ImageButton) findViewById(R.id.detail_screen_exports);

        new DatabaseTask() {

            @Override
            public void work() {
                long selectedMessageId = getIntent().getLongExtra(
                        Message.ID_COLUMN, -1);
                int selectedMessageType = getIntent().getIntExtra(
                        Message.TYPE_COLUMN, -1);

                currentMessage = (LocationMessage) MessageUtil
                        .newMessageInstanceByType(IMessageType
                                .valueOf(selectedMessageType));
                currentMessage.setId(selectedMessageId);
                currentMessage.load();
            }

            @Override
            public void done() {
                if (currentMessage.getSender() != null) {
                    setTitle(currentMessage.getSender().getDisplayName());
                } else {
                    LogIt.w(MapDetailScreenActivity.class, "sender",
                            currentMessage.getSenderId(),
                            "doesn't exist in the DB");
                }

                registerForContextMenu(exportsBtn);

                actionBtn.setText(R.string.detail_screen_open_map);
                actionBtn.setOnClickListener(new OpenInMapsClickListener());
                actionBtn
                        .setCompoundDrawablesWithIntrinsicBounds(
                                getResources()
                                        .getDrawable(
                                                R.drawable.detailview_actionbar_button_icon_location),
                                null, null, null);

                mMapView = (MapView) findViewById(R.id.mapview);
                mMapView.setBuiltInZoomControls(false);
                mMapView.displayZoomControls(false);
                mOverlays = mMapView.getOverlays();
                mCurrentLocationOverlay = new PlacesItemizedOverlay(
                        getResources().getDrawable(
                                R.drawable.common_map_maker_self),
                        MapDetailScreenActivity.this);
                mOverlays.add(mCurrentLocationOverlay);
                MapController mc = mMapView.getController();
                mc.setZoom(17);
                GeoPoint currentGeoPoint = new GeoPoint((int) (latitude * 1E6),
                        (int) (longitude * 1E6));
                mc.setCenter(currentGeoPoint);
                OverlayItem overlay = new OverlayItem(currentGeoPoint,
                        getString(R.string.select_location_current), "");

                mCurrentLocationOverlay.clearAll();
                mCurrentLocationOverlay.addOverlay(overlay);

                locationOverlay = new LocationDetailOverlay(mMapView,
                        currentMessage.getName(), currentMessage.getAddress(),
                        currentMessage.getSender());

                closeBtn.setOnClickListener(new CloseBtnClickListener());
                exportsBtn.setOnClickListener(new ExportsBtnClickListener());

                // In slow devices as the emulator a bad token error still appearing
                // A solution is use a delayed post to ensure that the window have been created before show the overlay
                // http://stackoverflow.com/questions/8782250/popupwindow-badtokenexception-unable-to-add-window-token-null-is-not-valid?answertab=active#tab-top
                mHandler.postDelayed(showOverlayRunnable, 500);
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(showOverlayRunnable);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();

        switch (currentMessage.getType()) {
        case LOCATION:
            inflater.inflate(R.menu.detail_screen_menu_location, menu);
            break;
        default:
            break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.forward_pm:
            Intent intent = new Intent(this, MessageContactActivity.class);

            intent.putExtra(Message.ID_COLUMN, currentMessage.getId());
            intent.putExtra(Message.TYPE_COLUMN, currentMessage.getType()
                    .getProtobufNumber());

            startActivity(intent);
            finish();
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    private class ExportsBtnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            openContextMenu(v);
        }
    }

    private class OpenInMapsClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            LogIt.user(MapDetailScreenActivity.class, "Pressed open in maps",
                    currentMessage.getName());

            String senderName = currentMessage.getSender() != null ? currentMessage
                    .getSender().getDisplayName() : "";

            Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=" + latitude + "," + longitude + " ("
                            + senderName + ")"));

            List<ResolveInfo> matchedApps = getPackageManager()
                    .queryIntentActivities(intent,
                            PackageManager.MATCH_DEFAULT_ONLY);

            if (matchedApps.size() == 0) {
                String data = String.format(MessageMeExternalAPI.G_MAPS_URL, latitude, longitude);

                intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse(data));

                LogIt.w(this,
                        "User doesn't have an application to open maps, opening maps in browser");

            }

            startActivity(intent);

        }
    }

    private class CloseBtnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            finish();
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    /**
     * Shows the location overlay
     * 
     * @param view parameter needed because this method can be called from a
     * layout listener
     */
    public void showOverlay(View view) {
        int marging = getResources().getDimensionPixelSize(
                R.dimen.location_detail_overlay_marging);

        if (locationOverlay != null) {
            locationOverlay.showLikeQuickAction(0, -1 * marging);
        } else {
            LogIt.w(this, "Can't display map overlay, locationOverlay is null");
        }
    }

    private Runnable showOverlayRunnable = new Runnable() {

        @Override
        public void run() {
            showOverlay(null);
        }
    };
}