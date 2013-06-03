package com.littleinc.MessageMe.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.ImageLoader.ProfilePhotoSize;

public class LocationDetailOverlay {

    private View root;

    protected final View anchor;

    private final PopupWindow window;

    public LocationDetailOverlay(View anchor, String title, String description,
            User sender) {
        this.anchor = anchor;
        this.window = new PopupWindow(anchor.getContext());

        // when a touch even happens outside of the window
        // make the window go away
        this.window.setTouchInterceptor(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    LocationDetailOverlay.this.window.dismiss();
                    return true;
                }
                return false;
            }
        });

        LayoutInflater inflater = (LayoutInflater) this.anchor.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        root = inflater.inflate(R.layout.location_detail, null);

        TextView titleLabel = (TextView) root.findViewById(R.id.title);
        TextView descriptionLabel = (TextView) root
                .findViewById(R.id.description);

        titleLabel.setText(title);
        descriptionLabel.setText(description);

        this.window.setBackgroundDrawable(new BitmapDrawable());

        this.window.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        this.window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        this.window.setTouchable(true);
        this.window.setFocusable(true);
        this.window.setOutsideTouchable(true);

        this.window.setContentView(this.root);

        ImageView userPicture = (ImageView) root.findViewById(R.id.user_thumb);

        if (sender != null) {
            ImageLoader.getInstance().displayProfilePicture(sender,
                    userPicture, ProfilePhotoSize.MEDIUM);
        } else {
            LogIt.w(LocationDetailOverlay.class,
                    "sender doesn't exist in the DB");
        }
    }

    /**
     * If you want to do anything when {@link dismiss} is called
     * 
     * @param listener
     */
    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        this.window.setOnDismissListener(listener);
    }

    /**
     * Displays like a popup from the anchor view.
     */
    public void showLikeQuickAction() {
        this.showLikeQuickAction(0, 0);
    }

    /**
     * Displays like a QuickAction from the anchor view.
     * 
     * @param xOffset
     *            offset in the X direction
     * @param yOffset
     *            offset in the Y direction
     */
    public void showLikeQuickAction(int xOffset, int yOffset) {

        this.window.setAnimationStyle(R.style.Animations_GrowFromBottom);

        int[] location = new int[2];
        this.anchor.getLocationOnScreen(location);

        Rect anchorRect = new Rect(location[0], location[1], location[0]
                + this.anchor.getWidth(), location[1] + this.anchor.getHeight());

        this.root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        int rootWidth = this.root.getMeasuredWidth();
        int rootHeight = this.root.getMeasuredHeight();
        int xPos = anchorRect.left + (anchor.getWidth() / 2 - (rootWidth / 2))
                + xOffset;
        int yPos = anchorRect.top + (anchor.getHeight() / 2 - (rootHeight))
                + yOffset;

        this.window.showAtLocation(this.anchor, Gravity.NO_GRAVITY, xPos, yPos);
    }

    public void showAtPosition(int x, int y) {
        this.window.setAnimationStyle(R.style.Animations_GrowFromBottom);

        int[] location = new int[2];

        this.root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        int rootWidth = this.root.getMeasuredWidth();
        int rootHeight = this.root.getMeasuredHeight();

        int xPos = x - (rootWidth / 2);
        int yPos = y - (rootHeight / 2);

        // display on bottom
        // if (rootHeight > anchorRect.top) {
        // yPos = anchorRect.bottom + yOffset;
        // this.window.setAnimationStyle(R.style.Animations_GrowFromTop);
        // }

        this.window.showAtLocation(this.anchor, Gravity.NO_GRAVITY, xPos, yPos);
    }

    public void dismiss() {
        this.window.dismiss();
    }
}
