package com.coredroid.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.littleinc.MessageMe.R;

public class UIUtil {

    private static Map<String, Typeface> typefaceMap = new HashMap<String, Typeface>();

    /**
     * Get the rough physical diagonal size of the device
     */
    public static float getScreenInches(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        double dimX = metrics.widthPixels / metrics.xdpi;
        double dimY = metrics.heightPixels / metrics.ydpi;
        return (float) Math.sqrt(dimX * dimX + dimY * dimY);
    }

    /**
     * Show the soft keyboard if a physical keyboard is not already
     * displayed.
     */
    public static void showKeyboard(View view) {
        LogIt.d(UIUtil.class, "showKeyboard");
        InputMethodManager mgr = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    /**
     * Close the soft keyboard associated with this view
     */
    public static void hideKeyboard(View view) {
        if (view == null) {
            LogIt.w(UIUtil.class, "hideKeyboard called with null view");
        } else {
            LogIt.d(UIUtil.class, "hideKeyboard");
            InputMethodManager mgr = (InputMethodManager) view.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static Typeface getTypeface(Context context, String typeface) {
        Typeface font = typefaceMap.get(typeface);
        if (font == null) {
            font = Typeface.createFromAsset(context.getAssets(), typeface);
            typefaceMap.put(typeface, font);
        }
        return font;
    }

    /**
     * Changes the size dimension to fit within the width/height ratio
     */
    public static void contain(Dimension size, int width, int height) {

        if (width == 0 || height == 0) {
            size.setWidth(0);
            size.setHeight(0);
            return;
        }

        double ratio = size.getWidth() / (double) size.getHeight();

        if (size.getWidth() < size.getHeight()) {
            size.setHeight(height);
            size.setWidth((int) (height * ratio));

            if (size.getWidth() > width) {
                size.setWidth(width);
                size.setHeight((int) (width / ratio));
            }
        } else {
            size.setWidth(width);
            size.setHeight((int) (width / ratio));

            if (size.getHeight() > height) {
                size.setHeight(height);
                size.setWidth((int) (height * ratio));
            }
        }
    }

    /**
     * Recursively set the font on all text views on this root
     */
    public static void setFont(View view, Typeface font) {

        if (view == null) {
            return;
        }

        if (view instanceof ViewGroup) {
            for (int i = 0, lim = ((ViewGroup) view).getChildCount(); i < lim; i++) {
                setFont(((ViewGroup) view).getChildAt(i), font);
            }
            return;
        }

        if (view instanceof TextView) {
            ((TextView) view).setTypeface(font);
        }
    }

    /**
     * Convenience method to show a loading dialog with message
     */
    public static ProgressDialog showProgressDialog(Context context,
            String message) {
        return ProgressDialog.show(context, null, message, true);
    }

    /**
     * Set the font on this specific view
     */
    public void setFont(Context context, TextView view, String font) {
        Typeface typeface = UIUtil.getTypeface(context, font);
        view.setTypeface(typeface);
    }

    /**
     * Set the font on the specific textview with the given ID
     */
    public void setFont(Activity activity, int viewId, String font) {
        TextView view = (TextView) activity.findViewById(viewId);
        setFont(activity, view, font);
    }

    /**
     * Get the top level content panel for the activity
     */
    public static View getContentPanel(Activity activity) {

        Window window = activity.getWindow();
        if (window == null) {
            return null;
        }

        View view = window.getDecorView();
        return view instanceof ViewGroup ? ((ViewGroup) view).getChildAt(0)
                : view;
    }

    public static void confirm(Context context, String title, String message,
            DialogInterface.OnClickListener onOK) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }
        if (!TextUtils.isEmpty(message)) {
            builder.setMessage(message);
        }
        builder.setPositiveButton(android.R.string.ok, onOK);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    public static Dialog confirmYesNo(Context context, int titleResId,
            int messageResId, DialogInterface.OnClickListener onYes,
            DialogInterface.OnClickListener onNo) {
        Dialog alert = confirmYesNo(context, context.getString(titleResId),
                context.getString(messageResId), onYes, onNo);

        return alert;
    }

    public static Dialog confirmYesNo(Context context, String title,
            String message, DialogInterface.OnClickListener onYes,
            DialogInterface.OnClickListener onNo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }
        if (!TextUtils.isEmpty(message)) {
            builder.setMessage(message);
        }
        builder.setPositiveButton(android.R.string.yes, onYes);
        builder.setNegativeButton(android.R.string.no, onNo);
        Dialog alert = builder.create();
        alert.show();
        return alert;
    }

    public static void alert(Context context, String title, String message) {
        new AlertDialog.Builder(new ContextThemeWrapper(context,
                R.style.Theme_Dialog_NoBackground))
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
    }

    public static void alert(Context context, int titleResId, String message) {
        new AlertDialog.Builder(new ContextThemeWrapper(context,
                R.style.Theme_Dialog_NoBackground))
                .setTitle(context.getString(titleResId))
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
    }

    public static void alert(Context context, int titleResId, int messageResId) {
        new AlertDialog.Builder(new ContextThemeWrapper(context,
                R.style.Theme_Dialog_NoBackground))
                .setTitle(context.getString(titleResId))
                .setMessage(context.getString(messageResId))
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
    }

    /**
     * Creates an Alert Dialog with centered text and title
     */
    public static void centeredAlert(Context context, String title,
            String message) {
        new AlertDialog.Builder(new ContextThemeWrapper(context,
                R.style.CenteredAlertDialog))
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
    }

    /**
     * Show a message dialog, clicking OK executes the supplied handler
     */
    public static void alert(Context context, String message,
            DialogInterface.OnClickListener onDismiss) {
        new AlertDialog.Builder(new ContextThemeWrapper(context,
                R.style.Theme_Dialog_NoBackground)).setMessage(message)
                .setPositiveButton(android.R.string.ok, onDismiss).create()
                .show();
    }

    /**
     * Show a message dialog, clicking OK executes the supplied handler
     */
    public static void alert(Context context, String title, String message,
            DialogInterface.OnClickListener onDismiss) {
        new AlertDialog.Builder(new ContextThemeWrapper(context,
                R.style.Theme_Dialog_NoBackground)).setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onDismiss).create()
                .show();
    }
    
    /**
     * Show a message dialog, clicking OK executes the supplied handler
     */
    public static void alert(Context context, String title, String message,
            String buttonMessage, DialogInterface.OnClickListener onDismiss) {
        new AlertDialog.Builder(new ContextThemeWrapper(context,
                R.style.Theme_Dialog_NoBackground)).setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(buttonMessage, onDismiss).create().show();
    }
    
    /**
     * Show a message dialog, clicking OK executes the supplied handler
     */
    public static void alert(Context context, String title, String message,
            String acceptButtonMessage, String cancelButtonMessage,
            DialogInterface.OnClickListener onAccept,
            DialogInterface.OnClickListener onCancel) {
        new AlertDialog.Builder(new ContextThemeWrapper(context,
                R.style.Theme_Dialog_NoBackground)).setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(cancelButtonMessage, onCancel)
                .setPositiveButton(acceptButtonMessage, onAccept).create()
                .show();
    }

    /**
     * Show a message dialog with an OK button, clicking OK kills the application
     */
    public static void fail(Context context, String message) {
        new AlertDialog.Builder(new ContextThemeWrapper(context,
                R.style.Theme_Dialog_NoBackground))
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                System.exit(0);
                            }
                        }).create().show();
    }

    /**
     * Prevent keyboard from popping up when field gets focus
     */
    public static void suppressKeyboard(EditText text) {
        text.setInputType(InputType.TYPE_NULL);
    }

    public static int getPixeslFromDip(float dips, DisplayMetrics metrics) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dips, metrics);
    }

    /**
     * Get the full package name of the activity currently in view.
     * 
     * This utility requires the GET_TASKS permission to use it.
     */
    public static String getTopActivity(final Context context) {
        String topActivityClass = "";

        if (context != null) {
            ActivityManager activityMgr = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);

            if (activityMgr != null) {
                List<RunningTaskInfo> tasks = activityMgr.getRunningTasks(1);

                if ((tasks != null) && (tasks.size() >= 1)) {
                    RunningTaskInfo topmostTask = tasks.get(0);

                    topActivityClass = topmostTask.topActivity.getClassName();
                    LogIt.d(UIUtil.class, "Top activity is", topActivityClass);
                }
            }
        }

        return topActivityClass;
    }
}
