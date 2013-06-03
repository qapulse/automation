package com.littleinc.MessageMe.ui;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class PlacesItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context mContext;

	public PlacesItemizedOverlay(Drawable defaultMarker, Context contex) {
		super(boundCenter(defaultMarker));
		mContext = contex;
		populate(); //http://code.google.com/p/android/issues/detail?id=2035
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	public void clearAll() {
		mOverlays.clear();
		populate();
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

}
