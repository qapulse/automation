package com.littleinc.MessageMe.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.ui.DoodleComposerActivity.OnPaintUpdateListener;
import com.littleinc.MessageMe.widget.ColorPicker.DoodleBrushColor;

public class ColorIcon extends FrameLayout implements OnClickListener {

    private View colorView;

    private DoodleBrushColor doodleColor;

    private OnPaintUpdateListener listener;

    public ColorIcon(Context context) {
        super(context);
        init();
    }

    public ColorIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOnClickListener(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        colorView = findViewById(R.id.color_view);
    }

    public DoodleBrushColor getDoodleColor() {
        return doodleColor;
    }

    public void setDoodleColor(DoodleBrushColor doodleColor) {
        this.doodleColor = doodleColor;
        colorView.setBackgroundColor(getResources().getColor(
                doodleColor.getColorResId()));
    }

    @Override
    public void onClick(View v) {
        if (listener != null)
            listener.onPaintUpdate(getDoodleColor());
    }

    public void setListener(OnPaintUpdateListener listener) {
        this.listener = listener;
    }
}