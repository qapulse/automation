package com.littleinc.MessageMe.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.coredroid.util.Dimension;
import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;

/**
 * Don't extend MessageMeFragment as there will always be another fragment
 * or activity on screen
 */
public class EmojiKeyboardFragment extends Fragment {

    /**
     * How long a key must be held down before it starts to repeat.
     */
    private static final int KEY_PRESSED_INITIAL_INTERVAL_MILLIS = 500;

    /**
     * Once a key has been held down long enough to start to repeat, this
     * interval defines how frequently those repeats happen.
     */
    private static final int KEY_PRESSED_REPEAT_INTERVAL_MILLIS = 100;

    private static final String EXTRA_NUMBER_OF_KEYS_PER_ROW = "extra_key_count_per_row";

    private static final String EXTRA_START_INDEX = "extra_start_index";

    private static final String EXTRA_END_INDEX = "extra_end_index";

    private static final int DELETE_KEY = -1;

    /**
     * Used to handle individual key presses.
     */
    private OnClickListener mClickListener;

    /**
     * Used to repeat the delete key if it is held down.
     */
    private OnTouchListener mTouchListener;

    private EmojiEditText mEditText;

    /**
     * An ordered list of the Unicode emoji character point codes as integers.
     * We use a separate data structure as it is much quicker to traverse a
     * List in order than it is to iterate through the keys in a Map.
     */
    private static List<Integer> sEmojiKeys;

    private static SparseIntArray sEmojiMap;

    private static int sEmojiKeySize;

    private static int sEmojiKeyboardPadding;

    static {
        Resources res = MessageMeApplication.getInstance().getResources();

        sEmojiKeySize = res
                .getDimensionPixelSize(R.dimen.emoji_keyboard_key_size);

        sEmojiKeyboardPadding = res
                .getDimensionPixelSize(R.dimen.emoji_keyboard_padding);

        sEmojiKeys = new ArrayList<Integer>();

        // The order here matches the order on the first page of smilies on
        // the default iOS 6 emoji keyboard
        sEmojiKeys.add(Integer.parseInt("1f604", 16));
        sEmojiKeys.add(Integer.parseInt("1f603", 16));
        sEmojiKeys.add(Integer.parseInt("1f60a", 16));
        sEmojiKeys.add(Integer.parseInt("263a", 16));
        sEmojiKeys.add(Integer.parseInt("1f609", 16));
        sEmojiKeys.add(Integer.parseInt("1f60d", 16));
        sEmojiKeys.add(Integer.parseInt("1f618", 16));
        sEmojiKeys.add(Integer.parseInt("1f61a", 16));
        sEmojiKeys.add(Integer.parseInt("1f61c", 16));
        sEmojiKeys.add(Integer.parseInt("1f61d", 16));
        sEmojiKeys.add(Integer.parseInt("1f633", 16));
        sEmojiKeys.add(Integer.parseInt("1f601", 16));
        sEmojiKeys.add(Integer.parseInt("1f614", 16));
        sEmojiKeys.add(Integer.parseInt("1f60c", 16));
        sEmojiKeys.add(Integer.parseInt("1f612", 16));
        sEmojiKeys.add(Integer.parseInt("1f61e", 16));
        sEmojiKeys.add(Integer.parseInt("1f623", 16));
        sEmojiKeys.add(Integer.parseInt("1f622", 16));
        sEmojiKeys.add(Integer.parseInt("1f602", 16));
        sEmojiKeys.add(Integer.parseInt("1f62d", 16));
        sEmojiKeys.add(Integer.parseInt("1f62a", 16));
        sEmojiKeys.add(Integer.parseInt("1f625", 16));
        sEmojiKeys.add(Integer.parseInt("1f630", 16));
        sEmojiKeys.add(Integer.parseInt("1f605", 16));
        sEmojiKeys.add(Integer.parseInt("1f613", 16));
        sEmojiKeys.add(Integer.parseInt("1f629", 16));
        sEmojiKeys.add(Integer.parseInt("1f62b", 16));
        sEmojiKeys.add(Integer.parseInt("1f631", 16));
        sEmojiKeys.add(Integer.parseInt("1f620", 16));
        sEmojiKeys.add(Integer.parseInt("1f621", 16));
        sEmojiKeys.add(Integer.parseInt("1f624", 16));
        sEmojiKeys.add(Integer.parseInt("1f616", 16));
        sEmojiKeys.add(Integer.parseInt("1f606", 16));
        sEmojiKeys.add(Integer.parseInt("1f60b", 16));
        sEmojiKeys.add(Integer.parseInt("1f637", 16));
        sEmojiKeys.add(Integer.parseInt("1f60e", 16));
        sEmojiKeys.add(Integer.parseInt("1f635", 16));
        sEmojiKeys.add(Integer.parseInt("1f632", 16));
        sEmojiKeys.add(Integer.parseInt("1f608", 16));
        sEmojiKeys.add(Integer.parseInt("1f47f", 16));
        sEmojiKeys.add(Integer.parseInt("1f610", 16));
        sEmojiKeys.add(Integer.parseInt("1f636", 16));
        sEmojiKeys.add(Integer.parseInt("1f607", 16));
        sEmojiKeys.add(Integer.parseInt("1f60f", 16));
        sEmojiKeys.add(Integer.parseInt("1f472", 16));
        sEmojiKeys.add(Integer.parseInt("1f473", 16));
        sEmojiKeys.add(Integer.parseInt("1f46e", 16));
        sEmojiKeys.add(Integer.parseInt("1f477", 16));
        sEmojiKeys.add(Integer.parseInt("1f482", 16));
        sEmojiKeys.add(Integer.parseInt("1f476", 16));
        sEmojiKeys.add(Integer.parseInt("1f466", 16));
        sEmojiKeys.add(Integer.parseInt("1f467", 16));
        sEmojiKeys.add(Integer.parseInt("1f468", 16));
        sEmojiKeys.add(Integer.parseInt("1f469", 16));
        sEmojiKeys.add(Integer.parseInt("1f474", 16));
        sEmojiKeys.add(Integer.parseInt("1f475", 16));
        sEmojiKeys.add(Integer.parseInt("1f471", 16));
        sEmojiKeys.add(Integer.parseInt("1f47c", 16));
        sEmojiKeys.add(Integer.parseInt("1f478", 16));
        sEmojiKeys.add(Integer.parseInt("1f63a", 16));
        sEmojiKeys.add(Integer.parseInt("1f638", 16));
        sEmojiKeys.add(Integer.parseInt("1f63b", 16));
        sEmojiKeys.add(Integer.parseInt("1f63d", 16));
        sEmojiKeys.add(Integer.parseInt("1f63c", 16));
        sEmojiKeys.add(Integer.parseInt("1f640", 16));
        sEmojiKeys.add(Integer.parseInt("1f63f", 16));
        sEmojiKeys.add(Integer.parseInt("1f639", 16));
        sEmojiKeys.add(Integer.parseInt("1f63e", 16));
        sEmojiKeys.add(Integer.parseInt("1f479", 16));
        sEmojiKeys.add(Integer.parseInt("1f47a", 16));
        sEmojiKeys.add(Integer.parseInt("1f648", 16));
        sEmojiKeys.add(Integer.parseInt("1f649", 16));
        sEmojiKeys.add(Integer.parseInt("1f64a", 16));
        sEmojiKeys.add(Integer.parseInt("1f480", 16));
        sEmojiKeys.add(Integer.parseInt("1f47d", 16));
        sEmojiKeys.add(Integer.parseInt("1f4a9", 16));
        sEmojiKeys.add(Integer.parseInt("1f525", 16));
        sEmojiKeys.add(Integer.parseInt("2728", 16));
        sEmojiKeys.add(Integer.parseInt("1f31f", 16));
        sEmojiKeys.add(Integer.parseInt("1f4ab", 16));
        sEmojiKeys.add(Integer.parseInt("1f4a5", 16));
        sEmojiKeys.add(Integer.parseInt("1f4a2", 16));
        sEmojiKeys.add(Integer.parseInt("1f4a6", 16));
        sEmojiKeys.add(Integer.parseInt("1f4a7", 16));
        sEmojiKeys.add(Integer.parseInt("1f4a4", 16));
        sEmojiKeys.add(Integer.parseInt("1f4a8", 16));
        sEmojiKeys.add(Integer.parseInt("1f442", 16));
        sEmojiKeys.add(Integer.parseInt("1f440", 16));
        sEmojiKeys.add(Integer.parseInt("1f443", 16));
        sEmojiKeys.add(Integer.parseInt("1f445", 16));
        sEmojiKeys.add(Integer.parseInt("1f444", 16));
        sEmojiKeys.add(Integer.parseInt("1f44d", 16));
        sEmojiKeys.add(Integer.parseInt("1f44e", 16));
        sEmojiKeys.add(Integer.parseInt("1f44c", 16));
        sEmojiKeys.add(Integer.parseInt("1f44a", 16));
        sEmojiKeys.add(Integer.parseInt("270a", 16));
        sEmojiKeys.add(Integer.parseInt("1f44b", 16));
        sEmojiKeys.add(Integer.parseInt("270b", 16));
        sEmojiKeys.add(Integer.parseInt("1f450", 16));
        sEmojiKeys.add(Integer.parseInt("1f446", 16));
        sEmojiKeys.add(Integer.parseInt("1f447", 16));
        sEmojiKeys.add(Integer.parseInt("1f449", 16));
        sEmojiKeys.add(Integer.parseInt("1f448", 16));
        sEmojiKeys.add(Integer.parseInt("1f64c", 16));
        sEmojiKeys.add(Integer.parseInt("1f64f", 16));
        sEmojiKeys.add(Integer.parseInt("1f4aa", 16));
        sEmojiKeys.add(Integer.parseInt("1f6b6", 16));
        sEmojiKeys.add(Integer.parseInt("1f3c3", 16));
        sEmojiKeys.add(Integer.parseInt("1f483", 16));
        sEmojiKeys.add(Integer.parseInt("1f46b", 16));
        sEmojiKeys.add(Integer.parseInt("1f46a", 16));
        sEmojiKeys.add(Integer.parseInt("1f46c", 16));
        sEmojiKeys.add(Integer.parseInt("1f46d", 16));
        sEmojiKeys.add(Integer.parseInt("1f46f", 16));
        sEmojiKeys.add(Integer.parseInt("1f491", 16));
        sEmojiKeys.add(Integer.parseInt("1f46f", 16));
        sEmojiKeys.add(Integer.parseInt("1f646", 16));
        sEmojiKeys.add(Integer.parseInt("1f645", 16));
        sEmojiKeys.add(Integer.parseInt("1f481", 16));
        sEmojiKeys.add(Integer.parseInt("1f64b", 16));
        sEmojiKeys.add(Integer.parseInt("1f486", 16));
        sEmojiKeys.add(Integer.parseInt("1f487", 16));
        sEmojiKeys.add(Integer.parseInt("1f485", 16));
        sEmojiKeys.add(Integer.parseInt("1f470", 16));
        sEmojiKeys.add(Integer.parseInt("1f64e", 16));
        sEmojiKeys.add(Integer.parseInt("1f64d", 16));
        sEmojiKeys.add(Integer.parseInt("1f647", 16));
        sEmojiKeys.add(Integer.parseInt("1f3a9", 16));
        sEmojiKeys.add(Integer.parseInt("1f451", 16));
        sEmojiKeys.add(Integer.parseInt("1f452", 16));
        sEmojiKeys.add(Integer.parseInt("1f45f", 16));
        sEmojiKeys.add(Integer.parseInt("1f45e", 16));
        sEmojiKeys.add(Integer.parseInt("1f461", 16));
        sEmojiKeys.add(Integer.parseInt("1f460", 16));
        sEmojiKeys.add(Integer.parseInt("1f462", 16));
        sEmojiKeys.add(Integer.parseInt("1f455", 16));
        sEmojiKeys.add(Integer.parseInt("1f454", 16));
        sEmojiKeys.add(Integer.parseInt("1f45a", 16));
        sEmojiKeys.add(Integer.parseInt("1f457", 16));
        sEmojiKeys.add(Integer.parseInt("1f3bd", 16));
        sEmojiKeys.add(Integer.parseInt("1f456", 16));
        sEmojiKeys.add(Integer.parseInt("1f458", 16));
        sEmojiKeys.add(Integer.parseInt("1f459", 16));
        sEmojiKeys.add(Integer.parseInt("1f4bc", 16));
        sEmojiKeys.add(Integer.parseInt("1f45c", 16));
        sEmojiKeys.add(Integer.parseInt("1f45d", 16));
        sEmojiKeys.add(Integer.parseInt("1f45b", 16));
        sEmojiKeys.add(Integer.parseInt("1f453", 16));
        sEmojiKeys.add(Integer.parseInt("1f380", 16));
        sEmojiKeys.add(Integer.parseInt("1f302", 16));
        sEmojiKeys.add(Integer.parseInt("1f484", 16));
        sEmojiKeys.add(Integer.parseInt("1f49b", 16));
        sEmojiKeys.add(Integer.parseInt("1f499", 16));
        sEmojiKeys.add(Integer.parseInt("1f49c", 16));
        sEmojiKeys.add(Integer.parseInt("1f49a", 16));
        sEmojiKeys.add(Integer.parseInt("1f494", 16));
        sEmojiKeys.add(Integer.parseInt("1f497", 16));
        sEmojiKeys.add(Integer.parseInt("1f493", 16));
        sEmojiKeys.add(Integer.parseInt("1f495", 16));
        sEmojiKeys.add(Integer.parseInt("1f496", 16));
        sEmojiKeys.add(Integer.parseInt("1f49e", 16));
        sEmojiKeys.add(Integer.parseInt("1f498", 16));
        sEmojiKeys.add(Integer.parseInt("1f48c", 16));
        sEmojiKeys.add(Integer.parseInt("1f48b", 16));
        sEmojiKeys.add(Integer.parseInt("1f48d", 16));
        sEmojiKeys.add(Integer.parseInt("1f48e", 16));
        sEmojiKeys.add(Integer.parseInt("1f464", 16));
        sEmojiKeys.add(Integer.parseInt("1f465", 16));
        sEmojiKeys.add(Integer.parseInt("1f4ac", 16));
        sEmojiKeys.add(Integer.parseInt("1f463", 16));
        sEmojiKeys.add(Integer.parseInt("1f4ad", 16));

        sEmojiMap = new SparseIntArray();

        sEmojiMap.put(Integer.parseInt("1f604", 16), R.drawable.u1f604);
        sEmojiMap.put(Integer.parseInt("1f603", 16), R.drawable.u1f603);
        sEmojiMap.put(Integer.parseInt("1f60a", 16), R.drawable.u1f60a);
        sEmojiMap.put(Integer.parseInt("263a", 16), R.drawable.u263a);
        sEmojiMap.put(Integer.parseInt("1f609", 16), R.drawable.u1f609);
        sEmojiMap.put(Integer.parseInt("1f60d", 16), R.drawable.u1f60d);
        sEmojiMap.put(Integer.parseInt("1f618", 16), R.drawable.u1f618);
        sEmojiMap.put(Integer.parseInt("1f61a", 16), R.drawable.u1f61a);
        sEmojiMap.put(Integer.parseInt("1f61c", 16), R.drawable.u1f61c);
        sEmojiMap.put(Integer.parseInt("1f61d", 16), R.drawable.u1f61d);
        sEmojiMap.put(Integer.parseInt("1f633", 16), R.drawable.u1f633);
        sEmojiMap.put(Integer.parseInt("1f601", 16), R.drawable.u1f601);
        sEmojiMap.put(Integer.parseInt("1f614", 16), R.drawable.u1f614);
        sEmojiMap.put(Integer.parseInt("1f60c", 16), R.drawable.u1f60c);
        sEmojiMap.put(Integer.parseInt("1f612", 16), R.drawable.u1f612);
        sEmojiMap.put(Integer.parseInt("1f61e", 16), R.drawable.u1f61e);
        sEmojiMap.put(Integer.parseInt("1f623", 16), R.drawable.u1f623);
        sEmojiMap.put(Integer.parseInt("1f622", 16), R.drawable.u1f622);
        sEmojiMap.put(Integer.parseInt("1f602", 16), R.drawable.u1f602);
        sEmojiMap.put(Integer.parseInt("1f62d", 16), R.drawable.u1f62d);
        sEmojiMap.put(Integer.parseInt("1f62a", 16), R.drawable.u1f62a);
        sEmojiMap.put(Integer.parseInt("1f625", 16), R.drawable.u1f625);
        sEmojiMap.put(Integer.parseInt("1f630", 16), R.drawable.u1f630);
        sEmojiMap.put(Integer.parseInt("1f605", 16), R.drawable.u1f605);
        sEmojiMap.put(Integer.parseInt("1f613", 16), R.drawable.u1f613);
        sEmojiMap.put(Integer.parseInt("1f629", 16), R.drawable.u1f629);
        sEmojiMap.put(Integer.parseInt("1f62b", 16), R.drawable.u1f62b);
        sEmojiMap.put(Integer.parseInt("1f631", 16), R.drawable.u1f631);
        sEmojiMap.put(Integer.parseInt("1f620", 16), R.drawable.u1f620);
        sEmojiMap.put(Integer.parseInt("1f621", 16), R.drawable.u1f621);
        sEmojiMap.put(Integer.parseInt("1f624", 16), R.drawable.u1f624);
        sEmojiMap.put(Integer.parseInt("1f616", 16), R.drawable.u1f616);
        sEmojiMap.put(Integer.parseInt("1f606", 16), R.drawable.u1f606);
        sEmojiMap.put(Integer.parseInt("1f60b", 16), R.drawable.u1f60b);
        sEmojiMap.put(Integer.parseInt("1f637", 16), R.drawable.u1f637);
        sEmojiMap.put(Integer.parseInt("1f60e", 16), R.drawable.u1f60e);
        sEmojiMap.put(Integer.parseInt("1f635", 16), R.drawable.u1f635);
        sEmojiMap.put(Integer.parseInt("1f632", 16), R.drawable.u1f632);
        sEmojiMap.put(Integer.parseInt("1f608", 16), R.drawable.u1f608);
        sEmojiMap.put(Integer.parseInt("1f47f", 16), R.drawable.u1f47f);
        sEmojiMap.put(Integer.parseInt("1f610", 16), R.drawable.u1f610);
        sEmojiMap.put(Integer.parseInt("1f636", 16), R.drawable.u1f636);
        sEmojiMap.put(Integer.parseInt("1f607", 16), R.drawable.u1f607);
        sEmojiMap.put(Integer.parseInt("1f60f", 16), R.drawable.u1f60f);
        sEmojiMap.put(Integer.parseInt("1f472", 16), R.drawable.u1f472);
        sEmojiMap.put(Integer.parseInt("1f473", 16), R.drawable.u1f473);
        sEmojiMap.put(Integer.parseInt("1f46e", 16), R.drawable.u1f46e);
        sEmojiMap.put(Integer.parseInt("1f477", 16), R.drawable.u1f477);
        sEmojiMap.put(Integer.parseInt("1f482", 16), R.drawable.u1f482);
        sEmojiMap.put(Integer.parseInt("1f476", 16), R.drawable.u1f476);
        sEmojiMap.put(Integer.parseInt("1f466", 16), R.drawable.u1f466);
        sEmojiMap.put(Integer.parseInt("1f467", 16), R.drawable.u1f467);
        sEmojiMap.put(Integer.parseInt("1f468", 16), R.drawable.u1f468);
        sEmojiMap.put(Integer.parseInt("1f469", 16), R.drawable.u1f469);
        sEmojiMap.put(Integer.parseInt("1f474", 16), R.drawable.u1f474);
        sEmojiMap.put(Integer.parseInt("1f475", 16), R.drawable.u1f475);
        sEmojiMap.put(Integer.parseInt("1f471", 16), R.drawable.u1f471);
        sEmojiMap.put(Integer.parseInt("1f47c", 16), R.drawable.u1f47c);
        sEmojiMap.put(Integer.parseInt("1f478", 16), R.drawable.u1f478);
        sEmojiMap.put(Integer.parseInt("1f63a", 16), R.drawable.u1f63a);
        sEmojiMap.put(Integer.parseInt("1f638", 16), R.drawable.u1f638);
        sEmojiMap.put(Integer.parseInt("1f63b", 16), R.drawable.u1f63b);
        sEmojiMap.put(Integer.parseInt("1f63d", 16), R.drawable.u1f63d);
        sEmojiMap.put(Integer.parseInt("1f63c", 16), R.drawable.u1f63c);
        sEmojiMap.put(Integer.parseInt("1f640", 16), R.drawable.u1f640);
        sEmojiMap.put(Integer.parseInt("1f63f", 16), R.drawable.u1f63f);
        sEmojiMap.put(Integer.parseInt("1f639", 16), R.drawable.u1f639);
        sEmojiMap.put(Integer.parseInt("1f63e", 16), R.drawable.u1f63e);
        sEmojiMap.put(Integer.parseInt("1f479", 16), R.drawable.u1f479);
        sEmojiMap.put(Integer.parseInt("1f47a", 16), R.drawable.u1f47a);
        sEmojiMap.put(Integer.parseInt("1f648", 16), R.drawable.u1f648);
        sEmojiMap.put(Integer.parseInt("1f649", 16), R.drawable.u1f649);
        sEmojiMap.put(Integer.parseInt("1f64a", 16), R.drawable.u1f64a);
        sEmojiMap.put(Integer.parseInt("1f480", 16), R.drawable.u1f480);
        sEmojiMap.put(Integer.parseInt("1f47d", 16), R.drawable.u1f47d);
        sEmojiMap.put(Integer.parseInt("1f4a9", 16), R.drawable.u1f4a9);
        sEmojiMap.put(Integer.parseInt("1f525", 16), R.drawable.u1f525);
        sEmojiMap.put(Integer.parseInt("2728", 16), R.drawable.u2728);
        sEmojiMap.put(Integer.parseInt("1f31f", 16), R.drawable.u1f31f);
        sEmojiMap.put(Integer.parseInt("1f4ab", 16), R.drawable.u1f4ab);
        sEmojiMap.put(Integer.parseInt("1f4a5", 16), R.drawable.u1f4a5);
        sEmojiMap.put(Integer.parseInt("1f4a2", 16), R.drawable.u1f4a2);
        sEmojiMap.put(Integer.parseInt("1f4a6", 16), R.drawable.u1f4a6);
        sEmojiMap.put(Integer.parseInt("1f4a7", 16), R.drawable.u1f4a7);
        sEmojiMap.put(Integer.parseInt("1f4a4", 16), R.drawable.u1f4a4);
        sEmojiMap.put(Integer.parseInt("1f4a8", 16), R.drawable.u1f4a8);
        sEmojiMap.put(Integer.parseInt("1f442", 16), R.drawable.u1f442);
        sEmojiMap.put(Integer.parseInt("1f440", 16), R.drawable.u1f440);
        sEmojiMap.put(Integer.parseInt("1f443", 16), R.drawable.u1f443);
        sEmojiMap.put(Integer.parseInt("1f445", 16), R.drawable.u1f445);
        sEmojiMap.put(Integer.parseInt("1f444", 16), R.drawable.u1f444);
        sEmojiMap.put(Integer.parseInt("1f44d", 16), R.drawable.u1f44d);
        sEmojiMap.put(Integer.parseInt("1f44e", 16), R.drawable.u1f44e);
        sEmojiMap.put(Integer.parseInt("1f44c", 16), R.drawable.u1f44c);
        sEmojiMap.put(Integer.parseInt("1f44a", 16), R.drawable.u1f44a);
        sEmojiMap.put(Integer.parseInt("270a", 16), R.drawable.u270a);
        sEmojiMap.put(Integer.parseInt("1f44b", 16), R.drawable.u1f44b);
        sEmojiMap.put(Integer.parseInt("270b", 16), R.drawable.u270b);
        sEmojiMap.put(Integer.parseInt("1f450", 16), R.drawable.u1f450);
        sEmojiMap.put(Integer.parseInt("1f446", 16), R.drawable.u1f446);
        sEmojiMap.put(Integer.parseInt("1f447", 16), R.drawable.u1f447);
        sEmojiMap.put(Integer.parseInt("1f449", 16), R.drawable.u1f449);
        sEmojiMap.put(Integer.parseInt("1f448", 16), R.drawable.u1f448);
        sEmojiMap.put(Integer.parseInt("1f64c", 16), R.drawable.u1f64c);
        sEmojiMap.put(Integer.parseInt("1f64f", 16), R.drawable.u1f64f);
        sEmojiMap.put(Integer.parseInt("1f4aa", 16), R.drawable.u1f4aa);
        sEmojiMap.put(Integer.parseInt("1f6b6", 16), R.drawable.u1f6b6);
        sEmojiMap.put(Integer.parseInt("1f3c3", 16), R.drawable.u1f3c3);
        sEmojiMap.put(Integer.parseInt("1f483", 16), R.drawable.u1f483);
        sEmojiMap.put(Integer.parseInt("1f46b", 16), R.drawable.u1f46b);
        sEmojiMap.put(Integer.parseInt("1f46a", 16), R.drawable.u1f46a);
        sEmojiMap.put(Integer.parseInt("1f46c", 16), R.drawable.u1f46c);
        sEmojiMap.put(Integer.parseInt("1f46d", 16), R.drawable.u1f46d);
        sEmojiMap.put(Integer.parseInt("1f46f", 16), R.drawable.u1f46f);
        sEmojiMap.put(Integer.parseInt("1f491", 16), R.drawable.u1f491);
        sEmojiMap.put(Integer.parseInt("1f46f", 16), R.drawable.u1f46f);
        sEmojiMap.put(Integer.parseInt("1f646", 16), R.drawable.u1f646);
        sEmojiMap.put(Integer.parseInt("1f645", 16), R.drawable.u1f645);
        sEmojiMap.put(Integer.parseInt("1f481", 16), R.drawable.u1f481);
        sEmojiMap.put(Integer.parseInt("1f64b", 16), R.drawable.u1f64b);
        sEmojiMap.put(Integer.parseInt("1f486", 16), R.drawable.u1f486);
        sEmojiMap.put(Integer.parseInt("1f487", 16), R.drawable.u1f487);
        sEmojiMap.put(Integer.parseInt("1f485", 16), R.drawable.u1f485);
        sEmojiMap.put(Integer.parseInt("1f470", 16), R.drawable.u1f470);
        sEmojiMap.put(Integer.parseInt("1f64e", 16), R.drawable.u1f64e);
        sEmojiMap.put(Integer.parseInt("1f64d", 16), R.drawable.u1f64d);
        sEmojiMap.put(Integer.parseInt("1f647", 16), R.drawable.u1f647);
        sEmojiMap.put(Integer.parseInt("1f3a9", 16), R.drawable.u1f3a9);
        sEmojiMap.put(Integer.parseInt("1f451", 16), R.drawable.u1f451);
        sEmojiMap.put(Integer.parseInt("1f452", 16), R.drawable.u1f452);
        sEmojiMap.put(Integer.parseInt("1f45f", 16), R.drawable.u1f45f);
        sEmojiMap.put(Integer.parseInt("1f45e", 16), R.drawable.u1f45e);
        sEmojiMap.put(Integer.parseInt("1f461", 16), R.drawable.u1f461);
        sEmojiMap.put(Integer.parseInt("1f460", 16), R.drawable.u1f460);
        sEmojiMap.put(Integer.parseInt("1f462", 16), R.drawable.u1f462);
        sEmojiMap.put(Integer.parseInt("1f455", 16), R.drawable.u1f455);
        sEmojiMap.put(Integer.parseInt("1f454", 16), R.drawable.u1f454);
        sEmojiMap.put(Integer.parseInt("1f45a", 16), R.drawable.u1f45a);
        sEmojiMap.put(Integer.parseInt("1f457", 16), R.drawable.u1f457);
        sEmojiMap.put(Integer.parseInt("1f3bd", 16), R.drawable.u1f3bd);
        sEmojiMap.put(Integer.parseInt("1f456", 16), R.drawable.u1f456);
        sEmojiMap.put(Integer.parseInt("1f458", 16), R.drawable.u1f458);
        sEmojiMap.put(Integer.parseInt("1f459", 16), R.drawable.u1f459);
        sEmojiMap.put(Integer.parseInt("1f4bc", 16), R.drawable.u1f4bc);
        sEmojiMap.put(Integer.parseInt("1f45c", 16), R.drawable.u1f45c);
        sEmojiMap.put(Integer.parseInt("1f45d", 16), R.drawable.u1f45d);
        sEmojiMap.put(Integer.parseInt("1f45b", 16), R.drawable.u1f45b);
        sEmojiMap.put(Integer.parseInt("1f453", 16), R.drawable.u1f453);
        sEmojiMap.put(Integer.parseInt("1f380", 16), R.drawable.u1f380);
        sEmojiMap.put(Integer.parseInt("1f302", 16), R.drawable.u1f302);
        sEmojiMap.put(Integer.parseInt("1f484", 16), R.drawable.u1f484);
        sEmojiMap.put(Integer.parseInt("1f49b", 16), R.drawable.u1f49b);
        sEmojiMap.put(Integer.parseInt("1f499", 16), R.drawable.u1f499);
        sEmojiMap.put(Integer.parseInt("1f49c", 16), R.drawable.u1f49c);
        sEmojiMap.put(Integer.parseInt("1f49a", 16), R.drawable.u1f49a);
        sEmojiMap.put(Integer.parseInt("1f494", 16), R.drawable.u1f494);
        sEmojiMap.put(Integer.parseInt("1f497", 16), R.drawable.u1f497);
        sEmojiMap.put(Integer.parseInt("1f493", 16), R.drawable.u1f493);
        sEmojiMap.put(Integer.parseInt("1f495", 16), R.drawable.u1f495);
        sEmojiMap.put(Integer.parseInt("1f496", 16), R.drawable.u1f496);
        sEmojiMap.put(Integer.parseInt("1f49e", 16), R.drawable.u1f49e);
        sEmojiMap.put(Integer.parseInt("1f498", 16), R.drawable.u1f498);
        sEmojiMap.put(Integer.parseInt("1f48c", 16), R.drawable.u1f48c);
        sEmojiMap.put(Integer.parseInt("1f48b", 16), R.drawable.u1f48b);
        sEmojiMap.put(Integer.parseInt("1f48d", 16), R.drawable.u1f48d);
        sEmojiMap.put(Integer.parseInt("1f48e", 16), R.drawable.u1f48e);
        sEmojiMap.put(Integer.parseInt("1f464", 16), R.drawable.u1f464);
        sEmojiMap.put(Integer.parseInt("1f465", 16), R.drawable.u1f465);
        sEmojiMap.put(Integer.parseInt("1f4ac", 16), R.drawable.u1f4ac);
        sEmojiMap.put(Integer.parseInt("1f463", 16), R.drawable.u1f463);
        sEmojiMap.put(Integer.parseInt("1f4ad", 16), R.drawable.u1f4ad);
    }

    public static int getKeyCount() {

        return sEmojiKeys.size();
    }

    public EmojiKeyboardFragment() {

        mClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                
                if (mEditText == null) {
                    // Ignore any events received after the fragment has been 
                    // destroyed
                    LogIt.w(EmojiKeyboardFragment.class,
                            "Ignore key click as mEditText is null");
                    return;
                }
                
                Object keyId = v.getTag();

                if (keyId instanceof Integer) {
                    int keyIdInt = (Integer) keyId;

                    if (keyIdInt == DELETE_KEY) {
                        LogIt.user(EmojiKeyboardFragment.class,
                                "onClick - Delete pressed");
                        mEditText.dispatchKeyEvent(new KeyEvent(
                                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                    } else {
                        LogIt.user(EmojiKeyboardFragment.class,
                                "onClick - emoji pressed", keyIdInt);
                        mEditText.insertEmoji(keyIdInt);
                    }
                } else {
                    LogIt.w(this, "Unexpected tag on view", keyId);
                }
            }
        };

        mTouchListener = new OnTouchListener() {

            private Handler mHandler;

            private Rect mViewBounds;

            // Runnable used when the delete key is held down
            Runnable mAction = new Runnable() {
                @Override
                public void run() {
                    LogIt.d(EmojiKeyboardFragment.class,
                            "OnTouchListener callback - send delete event");

                    mEditText.dispatchKeyEvent(new KeyEvent(
                            KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));

                    mHandler.postDelayed(this,
                            KEY_PRESSED_REPEAT_INTERVAL_MILLIS);
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Object keyId = v.getTag();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        mViewBounds = new Rect(v.getLeft(), v.getTop(),
                                v.getRight(), v.getBottom());

                        if (keyId instanceof Integer) {
                            int keyIdInt = (Integer) keyId;

                            if (keyIdInt == DELETE_KEY) {
                                LogIt.user(EmojiKeyboardFragment.class,
                                        "onTouch - delete pressed");

                                if (mHandler == null) {
                                    LogIt.d(EmojiKeyboardFragment.class,
                                            "onTouch - start callbacks", keyId);
                                    mHandler = new Handler();
                                    mHandler.postDelayed(mAction,
                                            KEY_PRESSED_INITIAL_INTERVAL_MILLIS);
                                }
                            } else {
                                LogIt.user(EmojiKeyboardFragment.class,
                                        "onTouch - emoji pressed", keyIdInt);
                                mEditText.insertEmoji(keyIdInt);
                            }
                        } else {
                            LogIt.w(this, "Unexpected tag on view", keyId);
                        }
                        break;
                    default:
                        // We only want to cancel the key repeat if the user
                        // moves outside of the button
                        if ((event.getAction() == MotionEvent.ACTION_MOVE)
                                && mViewBounds.contains(v.getLeft()
                                        + (int) event.getX(), v.getTop()
                                        + (int) event.getY())) {
                            LogIt.d(EmojiKeyboardFragment.class,
                                    "onTouch - ignore movement inside button");
                            return false;
                        }

                        // Always cancel the key repeat on any other event.
                        // These include MotionEvent.ACTION_UP and
                        // MotionEvent.ACTION_MOVE.
                        if (mHandler != null) {
                            LogIt.d(EmojiKeyboardFragment.class,
                                    "onTouch - cancel pending callback",
                                    event.getAction());
                            mHandler.removeCallbacks(mAction);
                            mHandler = null;
                        }
                        break;
                }

                // Indicate that we did not consume the event so the onClick
                // listener can get a chance to handle it.
                return false;
            }
        };
    }

    private void setEmojiEditText(EmojiEditText editText) {
        mEditText = editText;
    }

    /**
     * @return the character code for the emoji at the provided index.
     */
    private static int getKeyCodeAtIndex(int index) {
        return sEmojiKeys.get(index);
    }

    /**
     * @return the resource ID for the emoji at the provided index.
     */
    private static int getResourceAtIndex(int index) {
        Integer characterCode = sEmojiKeys.get(index);

        return sEmojiMap.get(characterCode);
    }

    /**
     * @return the drawable resource ID for the provided emoji character code.
     */
    public static int getResourceForEmojiCharCode(int characterCode) {
        return sEmojiMap.get(characterCode);
    }

    /**
     * @param startIndex the starting index to pass to
     * {@link EmojiKeyboardFragment#getResourceAtIndex(int)}
     */
    public static EmojiKeyboardFragment newInstance(int startIndex,
            int endIndex, int numberOfKeysPerRow, EmojiEditText editText) {
        EmojiKeyboardFragment fragment = new EmojiKeyboardFragment();
        fragment.setEmojiEditText(editText);

        LogIt.d(EmojiKeyboardFragment.class, "Created emoji keyboard",
                startIndex, endIndex);

        Bundle args = new Bundle();
        args.putInt(EXTRA_START_INDEX, startIndex);
        args.putInt(EXTRA_END_INDEX, endIndex);
        args.putInt(EXTRA_NUMBER_OF_KEYS_PER_ROW, numberOfKeysPerRow);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup mainView = (ViewGroup) inflater.inflate(
                R.layout.emoji_keyboard, container, false);

        int rowSize = getKeyCountPerRow();
        int keyCount = (getEndIndex() - getStartIndex());

        Dimension screenSize = MessageMeApplication.getScreenSize();

        // Annoyingly we can't get the size of our container here, so we
        // have to use the screen size
        int freeWidth = screenSize.getWidth() - (rowSize * sEmojiKeySize);

        LogIt.d(this, "onCreateView", getStartIndex(), getEndIndex(),
                getKeyCountPerRow(), freeWidth);

        RelativeLayout layout = (RelativeLayout) mainView
                .findViewById(R.id.main_container);

        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        // We use the setId() method for each button so we can align keys
        // relative to each other. We can't use "i" below as IDs must be a
        // positive integer (using zero for the first item breaks the layout).
        int id = 1;

        for (int i = 0; i <= keyCount; ++i, ++id) {

            ImageButton btn = new ImageButton(getActivity());
            btn.setId(id);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    sEmojiKeySize, sEmojiKeySize);

            if (i / rowSize == 0) {
                // The top row should be top aligned
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                        RelativeLayout.TRUE);
            } else {
                params.addRule(RelativeLayout.BELOW, id - rowSize);
            }

            if (i % rowSize == 0) {
                // The left column should be left aligned
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
                        RelativeLayout.TRUE);

                // Add the left padding required. We apply the padding here
                // so the ViewPager fills the entire screen width, which means
                // the fragments are visible all the way to the edge of the
                // screen when changing between tabs.
                params.leftMargin = (freeWidth / 2);
            } else {
                params.addRule(RelativeLayout.RIGHT_OF, id - 1);
            }

            btn.setLayoutParams(params);

            btn.setBackgroundResource(R.drawable.stickerpicker_sticker_base_selector);
            btn.setImageResource(getResourceAtIndex(i + getStartIndex()));
            btn.setOnClickListener(mClickListener);

            btn.setTag(getKeyCodeAtIndex(i + getStartIndex()));

            layout.addView(btn);
        }

        ImageButton deleteBtn = new ImageButton(getActivity());
        deleteBtn.setTag(DELETE_KEY);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                sEmojiKeySize, sEmojiKeySize);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

        // Add the right padding required
        params.rightMargin = (freeWidth / 2);

        deleteBtn.setLayoutParams(params);

        deleteBtn
                .setBackgroundResource(R.drawable.stickerpicker_sticker_base_selector);
        deleteBtn
                .setImageResource(R.drawable.stickerpicker_button_delete_selector);
        deleteBtn.setOnClickListener(mClickListener);

        // The delete button can be held down to repeat
        deleteBtn.setOnTouchListener(mTouchListener);

        layout.addView(deleteBtn);

        return mainView;
    }

    public static int getEmojiKeySize() {
        return sEmojiKeySize;
    }

    public static int getEmojiKeyboardPadding() {
        return sEmojiKeyboardPadding;
    }

    private int getStartIndex() {
        return getArguments().getInt(EXTRA_START_INDEX, 0);
    }

    private int getEndIndex() {
        return getArguments().getInt(EXTRA_END_INDEX, 0);
    }

    private int getKeyCountPerRow() {
        return getArguments().getInt(EXTRA_NUMBER_OF_KEYS_PER_ROW, 1);
    }

    public static int calculateNumberOfKeysPerRow() {
        Dimension screenSize = MessageMeApplication.getScreenSize();

        // Allow for our minimum left and right padding
        int width = screenSize.getWidth() - (2 * sEmojiKeyboardPadding);

        int keysPerRow = width / sEmojiKeySize;

        LogIt.d(EmojiKeyboardFragment.class, "Number of keys per row",
                keysPerRow);

        return keysPerRow;
    }
}
