package com.littleinc.MessageMe.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.ui.DoodleComposerActivity.OnPaintUpdateListener;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.ImageUtil;
import com.littleinc.MessageMe.widget.ColorPicker.DoodleBrushColor;
import com.littleinc.MessageMe.widget.ColorPicker.DoodleBrushSize;

public class DrawerView extends View implements OnPaintUpdateListener {

    public static final String DOODLE_KEY = FileSystemUtil.md5(String
            .valueOf(R.id.drawer_view));

    private DoodleBrushColor selectedBrushColor = DoodleBrushColor.COLOR_000;

    private DoodleBrushSize selectedBrushSize = DoodleBrushSize.SIZE_00;

    private Bitmap mBitmap;

    private Canvas mCanvas;

    private Path mPath;

    private Paint mBitmapPaint;

    private Paint mPaint;

    private List<Path> mPaths;

    private List<Paint> mPaints;

    private String backgroundPath;

    private Bitmap backgroundBitmap;

    private ImageLoader imageLoader = ImageLoader.getInstance();

    public DrawerView(Context context) {
        super(context);
        init();
    }

    public DrawerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mPath = new Path();
        mPaint = createNewPaint();
        mPaths = new ArrayList<Path>();
        mPaints = new ArrayList<Paint>();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        backgroundPath = null;
        backgroundBitmap = null;

        if (imageLoader.containsImage(DOODLE_KEY)) {
            imageLoader.getImage(DOODLE_KEY).recycle();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        try {
            if (imageLoader.containsImage(DOODLE_KEY)) {
                mBitmap = imageLoader.getImage(DOODLE_KEY);

                if (mBitmap.isRecycled() || mBitmap.getWidth() != w
                        || mBitmap.getHeight() != h) {
                    mBitmap = createBitmap(w, h);
                    imageLoader.addImage(DOODLE_KEY, mBitmap);
                }
            } else {
                mBitmap = createBitmap(w, h);
                imageLoader.addImage(DOODLE_KEY, mBitmap);
            }

            mCanvas = new Canvas(mBitmap);

            if (backgroundBitmap != null) {
                drawBitmap(backgroundBitmap);
            } else if (backgroundPath != null) {
                drawNewBackground(backgroundPath);
            }
        } catch (OutOfMemoryError e) {
            LogIt.e(this, e);
        } catch (Exception e) {
            LogIt.e(this, e);
        }
    }

    private Bitmap createBitmap(int w, int h) {
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null && !mBitmap.isRecycled() && mBitmapPaint != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        } else {
            LogIt.w(DrawerView.class, "Trying to draw a recycled bitmap");
        }
    }

    private float mX, mY;

    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mPath.lineTo(x + 1, y + 1);
        mX = x;
        mY = y;

        mCanvas.drawPath(mPath, mPaint);
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }

        mCanvas.drawPath(mPath, mPaint);
    }

    private void touch_up() {
        mPath.lineTo(mX, mY);
        mCanvas.drawPath(mPath, mPaint);
        Path pathCopy = new Path(mPath);
        mPath.reset();

        mPaths.add(pathCopy);
        mPaints.add(mPaint);

        mPaint = createNewPaint();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            touch_start(x, y);
            invalidate();
            break;
        case MotionEvent.ACTION_MOVE:
            touch_move(x, y);
            invalidate();
            break;
        case MotionEvent.ACTION_UP:
            touch_up();
            invalidate();
            break;
        }
        return true;
    }

    public Bitmap getDoodle() {
        return mBitmap;
    }

    public void clear() {
        mPaths.clear();
        mPaints.clear();
        backgroundPath = null;

        if (backgroundBitmap != null) {
            backgroundBitmap.recycle();
        }
        backgroundBitmap = null;

        mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

        invalidate();
    }

    public void undo() {
        if (mPaths.size() > 0 && mPaints.size() > 0) {
            int index = mPaths.size() - 1;

            mPaths.remove(index);
            mPaints.remove(index);

            mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            if (backgroundBitmap != null) {
                drawBitmap(backgroundBitmap);
            } else if (backgroundPath != null) {
                try {
                    drawNewBackground(backgroundPath);
                } catch (Exception e) {
                    LogIt.d(this, e, e.getMessage());
                }
            }

            for (int i = 0; i < mPaths.size(); i++) {
                mCanvas.drawPath(mPaths.get(i), mPaints.get(i));
            }

            invalidate();
        }
    }

    /**
     * Draws a new background into the doodle composer.
     * This background is added to the memory cache to avoid OutOfMemoryExceptions
     */
    public void drawNewBackground(String path) throws Exception {
        backgroundPath = path;

        if (mBitmap != null) {
            Bitmap background = imageLoader.getImage(backgroundPath);

            // Only reuse the cached image if it have the same dimensions
            if (backgroundPath != null && background != null
                    && background.getWidth() == mBitmap.getWidth()
                    && background.getHeight() == mBitmap.getHeight()) {
                background = imageLoader.getImage(backgroundPath);
            } else if (backgroundPath != null) {
                background = imageLoader.getBitmap(backgroundPath,
                        mBitmap.getWidth(), mBitmap.getHeight());

                // Doodle background should fill the entire canvas, so we need to
                // resize the background bitmap if is needed
                Options options = new Options();
                options.outWidth = background.getWidth();
                options.outHeight = background.getHeight();

                background = ImageUtil
                        .resizeBitmapIfNeeded(background, options,
                                mBitmap.getWidth(), mBitmap.getHeight(), false);

                imageLoader.addImage(backgroundPath, background);
            } else {
                throw new Exception("Selected image is not available");
            }

            if (mCanvas != null) {
                mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                drawBitmap(background);
            }
        }
    }

    public void drawNewBackground(Bitmap bitmap) {
        bitmap = ImageUtil.resizeImage(bitmap, mBitmap.getHeight(), false);

        backgroundBitmap = bitmap;
        drawBitmap(bitmap);
    }

    private void drawBitmap(Bitmap bitmap) {
        if (mBitmap != null && mCanvas != null) {
            mCanvas.drawBitmap(bitmap, (getWidth() - bitmap.getWidth()) / 2,
                    (getHeight() - bitmap.getHeight()) / 2, new Paint());

            for (int i = 0; i < mPaths.size(); i++) {
                mCanvas.drawPath(mPaths.get(i), mPaints.get(i));
            }

            invalidate();
        }
    }

    private Paint createNewPaint() {
        Paint paint = new Paint();

        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        if (DoodleBrushColor.TRANSPARENT == selectedBrushColor) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            paint.setXfermode(null);
        }

        paint.setStrokeWidth(getResources().getDimension(
                selectedBrushSize.getDimenResId()));
        paint.setColor(getResources().getColor(
                selectedBrushColor.getColorResId()));

        return paint;
    }

    @Override
    public void onPaintUpdate(DoodleBrushColor doodleBrushColor) {
        selectedBrushColor = doodleBrushColor;
        int brushColor = getResources().getColor(
                selectedBrushColor.getColorResId());

        if (DoodleBrushColor.TRANSPARENT == doodleBrushColor) {
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            mPaint.setXfermode(null);
        }
        mPaint.setColor(brushColor);
    }

    @Override
    public void onPaintUpdate(DoodleBrushSize doodleBrushSize) {
        selectedBrushSize = doodleBrushSize;
        float brushSize = getResources().getDimension(
                selectedBrushSize.getDimenResId());
        mPaint.setStrokeWidth(brushSize);
    }

    public boolean isDoodleCreated() {
        return mPaints.size() > 0 && mPaths.size() > 0;
    }
}