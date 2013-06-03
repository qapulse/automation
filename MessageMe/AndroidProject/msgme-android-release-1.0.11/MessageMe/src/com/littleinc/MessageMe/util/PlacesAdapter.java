package com.littleinc.MessageMe.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.net.FacebookPlace;
import com.littleinc.MessageMe.net.FacebookPlaceComparator;
import com.littleinc.MessageMe.util.ConversionUtil.UnitLocale;

/**
 * Adapter class to be used to display locations from the Facebook places API
 */
public class PlacesAdapter extends BaseAdapter {

    List<FacebookPlace> mPlaces = new ArrayList<FacebookPlace>();

    LayoutInflater inflater;

    Location latestLocation;

    public PlacesAdapter(Context context) {
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addPlaces(List<FacebookPlace> places) {
        mPlaces.addAll(places);

        FacebookPlaceComparator comparator = new FacebookPlaceComparator(
                latestLocation);
        Collections.sort(mPlaces, comparator);

        notifyDataSetChanged();
    }

    public void setPlaces(List<FacebookPlace> places) {
        mPlaces = places;

        notifyDataSetChanged();
    }

    public void setLocation(Location location) {
        latestLocation = location;
    }

    @Override
    public int getCount() {
        return mPlaces.size();
    }

    /**
     * Return the FacebookPlace at the specified location.
     * 
     * This method needs to cope if the mPlaces list is null as the user could
     * have forced the activity to recreate (e.g. by opening/closing a physical
     * keyboard) while a getItem call is pending on the old adapter.
     */
    @Override
    public FacebookPlace getItem(int position) {
        if ((mPlaces != null) && (position < mPlaces.size())) {
            return mPlaces.get(position);
        } else if (mPlaces == null) {
            LogIt.i(this, "Ignore getItem call when mPlaces is null");
            return null;
        } else {
            LogIt.i(this, "Ignore getItem call outside size of mPlaces",
                    position + "/" + mPlaces.size());
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        PlaceViewHolder holder;
        FacebookPlace place = (FacebookPlace) getItem(position);
        if (convertView == null) {
            holder = new PlaceViewHolder();
            convertView = inflater.inflate(R.layout.list_item_place, parent,
                    false);
            holder.distance = (TextView) convertView
                    .findViewById(R.id.list_item_place_distance);
            holder.name = (TextView) convertView
                    .findViewById(R.id.list_item_place_name);
            holder.unit = (TextView) convertView
                    .findViewById(R.id.list_item_place_unit);

            convertView.setTag(holder);
        } else {
            holder = (PlaceViewHolder) convertView.getTag();
        }
        if (latestLocation != null) {
            String distance = "";
            String unit = "";
            double kilometers = LocationUtil
                    .distance(latestLocation.getLatitude(),
                            latestLocation.getLongitude(),
                            place.getLocation().latitude,
                            place.getLocation().longitude);

            if (ConversionUtil.getCurrent() == UnitLocale.Imperial) {
                double miles = ConversionUtil.kilometerToMiles(kilometers);
                if (miles <= 0.1) {
                    distance = context.getString(R.string.distance_format,
                            ConversionUtil.kilometerToFeets(kilometers));
                    unit = context.getString(R.string.feet_unit);
                } else {
                    distance = context.getString(R.string.distance_format,
                            miles);
                    unit = context.getString(R.string.miles_unit);
                }
            } else {
                if (kilometers < 0.5) { // show meters
                    distance = context.getString(R.string.distance_format,
                            kilometers * 1000);
                    unit = context.getString(R.string.meters_unit);
                } else { // show kilometers
                    distance = context.getString(R.string.distance_format,
                            kilometers);
                    unit = context.getString(R.string.kilometers_unit);
                }
            }
            holder.distance.setText(distance);
            holder.unit.setText(unit);
        } else {
            holder.distance.setText("");
            holder.unit.setText("");
        }
        holder.name.setText(place.getName());
        // holder.position.setText(String.valueOf(position + 1));

        return convertView;
    }

    public void clear() {
        mPlaces.clear();
        notifyDataSetChanged();
    }

    private static class PlaceViewHolder {
        TextView unit;

        TextView name;

        TextView distance;
    }
}
