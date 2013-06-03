package com.littleinc.MessageMe.util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;

public class ImageCache extends LruCache<String, Bitmap> {
	public ImageCache(int maxSize) {
		super(maxSize);
	}

	@TargetApi(12)
	@Override
	protected int sizeOf(String key, Bitmap value) {
		// Using format ARGB_888 (default) every pixel is stored in 4bytes.
		// If API 12 or latest is enabled, we can use Bitmap.getByteCount()
		// method
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB){
			return value.getByteCount();
		}
		return value.getRowBytes() * value.getHeight();
	}
}
