package com.littleinc.MessageMe.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.util.SparseArray;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.util.StringUtil;

/**
 * Utilities for finding and replacing emojis.  Android support for iOS emojis
 * is very poor so we need to handle emoji display ourselves.
 */
public class EmojiUtils {
    
    private static final Pattern mNewLinePattern = Pattern.compile(StringUtil.NEW_LINE,
            Pattern.CASE_INSENSITIVE);
    
    private static ImageGetter sEmojiImageGetterBig;

    private static ImageGetter sEmojiImageGetterSmall;

    /**
     * A map of the Unicode emoji character point codes as integers to 
     * HTML for displaying a local image file of the emoji, e.g.
     *   u1f302 (equivalent to \uD83C\uDF02) -> <img src="ulf302.png"> 
     * 
     * This map can also be used to efficiently calculate whether a String
     * contains an emoji or not.
     */
    private static SparseArray<String> sEmojiMap;

    static {
        sEmojiMap = new SparseArray<String>();

        // This website was the source of most of the Unicode values here:
        //   http://punchdrunker.github.com/iOSEmoji/table_html/ios6/index.html
        //
        // Remove the "u" off the front so the values can be parsed as hex
        // values and converted to Integers.
        //
        // These are the Unicode 6.0 standard unified code points for emojis,
        // which matches the standard emojis keyboard on iOS 6.
        sEmojiMap.put(Integer.parseInt("1f302", 16), getEmojiHTML("u1f302"));
        sEmojiMap.put(Integer.parseInt("1f31f", 16), getEmojiHTML("u1f31f"));
        sEmojiMap.put(Integer.parseInt("1f380", 16), getEmojiHTML("u1f380"));
        sEmojiMap.put(Integer.parseInt("1f3a9", 16), getEmojiHTML("u1f3a9"));
        sEmojiMap.put(Integer.parseInt("1f3bd", 16), getEmojiHTML("u1f3bd"));
        sEmojiMap.put(Integer.parseInt("1f3c3", 16), getEmojiHTML("u1f3c3"));
        sEmojiMap.put(Integer.parseInt("1f440", 16), getEmojiHTML("u1f440"));
        sEmojiMap.put(Integer.parseInt("1f442", 16), getEmojiHTML("u1f442"));
        sEmojiMap.put(Integer.parseInt("1f443", 16), getEmojiHTML("u1f443"));
        sEmojiMap.put(Integer.parseInt("1f444", 16), getEmojiHTML("u1f444"));
        sEmojiMap.put(Integer.parseInt("1f445", 16), getEmojiHTML("u1f445"));
        sEmojiMap.put(Integer.parseInt("1f446", 16), getEmojiHTML("u1f446"));
        sEmojiMap.put(Integer.parseInt("1f447", 16), getEmojiHTML("u1f447"));
        sEmojiMap.put(Integer.parseInt("1f448", 16), getEmojiHTML("u1f448"));
        sEmojiMap.put(Integer.parseInt("1f449", 16), getEmojiHTML("u1f449"));
        sEmojiMap.put(Integer.parseInt("1f44a", 16), getEmojiHTML("u1f44a"));
        sEmojiMap.put(Integer.parseInt("1f44b", 16), getEmojiHTML("u1f44b"));
        sEmojiMap.put(Integer.parseInt("1f44c", 16), getEmojiHTML("u1f44c"));
        sEmojiMap.put(Integer.parseInt("1f44d", 16), getEmojiHTML("u1f44d"));
        sEmojiMap.put(Integer.parseInt("1f44e", 16), getEmojiHTML("u1f44e"));
        sEmojiMap.put(Integer.parseInt("1f44f", 16), getEmojiHTML("u1f44f"));
        sEmojiMap.put(Integer.parseInt("1f450", 16), getEmojiHTML("u1f450"));
        sEmojiMap.put(Integer.parseInt("1f451", 16), getEmojiHTML("u1f451"));
        sEmojiMap.put(Integer.parseInt("1f452", 16), getEmojiHTML("u1f452"));
        sEmojiMap.put(Integer.parseInt("1f453", 16), getEmojiHTML("u1f453"));
        sEmojiMap.put(Integer.parseInt("1f454", 16), getEmojiHTML("u1f454"));
        sEmojiMap.put(Integer.parseInt("1f455", 16), getEmojiHTML("u1f455"));
        sEmojiMap.put(Integer.parseInt("1f456", 16), getEmojiHTML("u1f456"));
        sEmojiMap.put(Integer.parseInt("1f457", 16), getEmojiHTML("u1f457"));
        sEmojiMap.put(Integer.parseInt("1f458", 16), getEmojiHTML("u1f458"));
        sEmojiMap.put(Integer.parseInt("1f459", 16), getEmojiHTML("u1f459"));
        sEmojiMap.put(Integer.parseInt("1f45a", 16), getEmojiHTML("u1f45a"));
        sEmojiMap.put(Integer.parseInt("1f45b", 16), getEmojiHTML("u1f45b"));
        sEmojiMap.put(Integer.parseInt("1f45c", 16), getEmojiHTML("u1f45c"));
        sEmojiMap.put(Integer.parseInt("1f45d", 16), getEmojiHTML("u1f45d"));
        sEmojiMap.put(Integer.parseInt("1f45e", 16), getEmojiHTML("u1f45e"));
        sEmojiMap.put(Integer.parseInt("1f45f", 16), getEmojiHTML("u1f45f"));
        sEmojiMap.put(Integer.parseInt("1f460", 16), getEmojiHTML("u1f460"));
        sEmojiMap.put(Integer.parseInt("1f461", 16), getEmojiHTML("u1f461"));
        sEmojiMap.put(Integer.parseInt("1f462", 16), getEmojiHTML("u1f462"));
        sEmojiMap.put(Integer.parseInt("1f463", 16), getEmojiHTML("u1f463"));
        sEmojiMap.put(Integer.parseInt("1f464", 16), getEmojiHTML("u1f464"));
        sEmojiMap.put(Integer.parseInt("1f465", 16), getEmojiHTML("u1f465"));
        sEmojiMap.put(Integer.parseInt("1f466", 16), getEmojiHTML("u1f466"));
        sEmojiMap.put(Integer.parseInt("1f467", 16), getEmojiHTML("u1f467"));
        sEmojiMap.put(Integer.parseInt("1f468", 16), getEmojiHTML("u1f468"));
        sEmojiMap.put(Integer.parseInt("1f469", 16), getEmojiHTML("u1f469"));
        sEmojiMap.put(Integer.parseInt("1f46a", 16), getEmojiHTML("u1f46a"));
        sEmojiMap.put(Integer.parseInt("1f46b", 16), getEmojiHTML("u1f46b"));
        sEmojiMap.put(Integer.parseInt("1f46c", 16), getEmojiHTML("u1f46c"));
        sEmojiMap.put(Integer.parseInt("1f46d", 16), getEmojiHTML("u1f46d"));
        sEmojiMap.put(Integer.parseInt("1f46e", 16), getEmojiHTML("u1f46e"));
        sEmojiMap.put(Integer.parseInt("1f46f", 16), getEmojiHTML("u1f46f"));
        sEmojiMap.put(Integer.parseInt("1f470", 16), getEmojiHTML("u1f470"));
        sEmojiMap.put(Integer.parseInt("1f471", 16), getEmojiHTML("u1f471"));
        sEmojiMap.put(Integer.parseInt("1f472", 16), getEmojiHTML("u1f472"));
        sEmojiMap.put(Integer.parseInt("1f473", 16), getEmojiHTML("u1f473"));
        sEmojiMap.put(Integer.parseInt("1f474", 16), getEmojiHTML("u1f474"));
        sEmojiMap.put(Integer.parseInt("1f475", 16), getEmojiHTML("u1f475"));
        sEmojiMap.put(Integer.parseInt("1f476", 16), getEmojiHTML("u1f476"));
        sEmojiMap.put(Integer.parseInt("1f477", 16), getEmojiHTML("u1f477"));
        sEmojiMap.put(Integer.parseInt("1f478", 16), getEmojiHTML("u1f478"));
        sEmojiMap.put(Integer.parseInt("1f479", 16), getEmojiHTML("u1f479"));
        sEmojiMap.put(Integer.parseInt("1f47a", 16), getEmojiHTML("u1f47a"));
        sEmojiMap.put(Integer.parseInt("1f47c", 16), getEmojiHTML("u1f47c"));
        sEmojiMap.put(Integer.parseInt("1f47d", 16), getEmojiHTML("u1f47d"));
        sEmojiMap.put(Integer.parseInt("1f47f", 16), getEmojiHTML("u1f47f"));
        sEmojiMap.put(Integer.parseInt("1f480", 16), getEmojiHTML("u1f480"));
        sEmojiMap.put(Integer.parseInt("1f481", 16), getEmojiHTML("u1f481"));
        sEmojiMap.put(Integer.parseInt("1f482", 16), getEmojiHTML("u1f482"));
        sEmojiMap.put(Integer.parseInt("1f483", 16), getEmojiHTML("u1f483"));
        sEmojiMap.put(Integer.parseInt("1f484", 16), getEmojiHTML("u1f484"));
        sEmojiMap.put(Integer.parseInt("1f485", 16), getEmojiHTML("u1f485"));
        sEmojiMap.put(Integer.parseInt("1f486", 16), getEmojiHTML("u1f486"));
        sEmojiMap.put(Integer.parseInt("1f487", 16), getEmojiHTML("u1f487"));
        sEmojiMap.put(Integer.parseInt("1f48b", 16), getEmojiHTML("u1f48b"));
        sEmojiMap.put(Integer.parseInt("1f48c", 16), getEmojiHTML("u1f48c"));
        sEmojiMap.put(Integer.parseInt("1f48d", 16), getEmojiHTML("u1f48d"));
        sEmojiMap.put(Integer.parseInt("1f48e", 16), getEmojiHTML("u1f48e"));
        sEmojiMap.put(Integer.parseInt("1f48f", 16), getEmojiHTML("u1f48f"));
        sEmojiMap.put(Integer.parseInt("1f491", 16), getEmojiHTML("u1f491"));
        sEmojiMap.put(Integer.parseInt("1f493", 16), getEmojiHTML("u1f493"));
        sEmojiMap.put(Integer.parseInt("1f494", 16), getEmojiHTML("u1f494"));
        sEmojiMap.put(Integer.parseInt("1f495", 16), getEmojiHTML("u1f495"));
        sEmojiMap.put(Integer.parseInt("1f496", 16), getEmojiHTML("u1f496"));
        sEmojiMap.put(Integer.parseInt("1f497", 16), getEmojiHTML("u1f497"));
        sEmojiMap.put(Integer.parseInt("1f498", 16), getEmojiHTML("u1f498"));
        sEmojiMap.put(Integer.parseInt("1f499", 16), getEmojiHTML("u1f499"));
        sEmojiMap.put(Integer.parseInt("1f49a", 16), getEmojiHTML("u1f49a"));
        sEmojiMap.put(Integer.parseInt("1f49b", 16), getEmojiHTML("u1f49b"));
        sEmojiMap.put(Integer.parseInt("1f49c", 16), getEmojiHTML("u1f49c"));
        sEmojiMap.put(Integer.parseInt("1f49e", 16), getEmojiHTML("u1f49e"));
        sEmojiMap.put(Integer.parseInt("1f4a2", 16), getEmojiHTML("u1f4a2"));
        sEmojiMap.put(Integer.parseInt("1f4a4", 16), getEmojiHTML("u1f4a4"));
        sEmojiMap.put(Integer.parseInt("1f4a5", 16), getEmojiHTML("u1f4a5"));
        sEmojiMap.put(Integer.parseInt("1f4a6", 16), getEmojiHTML("u1f4a6"));
        sEmojiMap.put(Integer.parseInt("1f4a7", 16), getEmojiHTML("u1f4a7"));
        sEmojiMap.put(Integer.parseInt("1f4a8", 16), getEmojiHTML("u1f4a8"));
        sEmojiMap.put(Integer.parseInt("1f4a9", 16), getEmojiHTML("u1f4a9"));
        sEmojiMap.put(Integer.parseInt("1f4aa", 16), getEmojiHTML("u1f4aa"));
        sEmojiMap.put(Integer.parseInt("1f4ab", 16), getEmojiHTML("u1f4ab"));
        sEmojiMap.put(Integer.parseInt("1f4ac", 16), getEmojiHTML("u1f4ac"));
        sEmojiMap.put(Integer.parseInt("1f4ad", 16), getEmojiHTML("u1f4ad"));
        sEmojiMap.put(Integer.parseInt("1f4bc", 16), getEmojiHTML("u1f4bc"));
        sEmojiMap.put(Integer.parseInt("1f525", 16), getEmojiHTML("u1f525"));
        sEmojiMap.put(Integer.parseInt("1f600", 16), getEmojiHTML("u1f600"));
        sEmojiMap.put(Integer.parseInt("1f601", 16), getEmojiHTML("u1f601"));
        sEmojiMap.put(Integer.parseInt("1f602", 16), getEmojiHTML("u1f602"));
        sEmojiMap.put(Integer.parseInt("1f603", 16), getEmojiHTML("u1f603"));
        sEmojiMap.put(Integer.parseInt("1f604", 16), getEmojiHTML("u1f604"));
        sEmojiMap.put(Integer.parseInt("1f605", 16), getEmojiHTML("u1f605"));
        sEmojiMap.put(Integer.parseInt("1f606", 16), getEmojiHTML("u1f606"));
        sEmojiMap.put(Integer.parseInt("1f607", 16), getEmojiHTML("u1f607"));
        sEmojiMap.put(Integer.parseInt("1f608", 16), getEmojiHTML("u1f608"));
        sEmojiMap.put(Integer.parseInt("1f609", 16), getEmojiHTML("u1f609"));
        sEmojiMap.put(Integer.parseInt("1f60a", 16), getEmojiHTML("u1f60a"));
        sEmojiMap.put(Integer.parseInt("1f60c", 16), getEmojiHTML("u1f60c"));
        sEmojiMap.put(Integer.parseInt("1f60d", 16), getEmojiHTML("u1f60d"));
        sEmojiMap.put(Integer.parseInt("1f60b", 16), getEmojiHTML("u1f60b"));
        sEmojiMap.put(Integer.parseInt("1f60e", 16), getEmojiHTML("u1f60e"));
        sEmojiMap.put(Integer.parseInt("1f60f", 16), getEmojiHTML("u1f60f"));
        sEmojiMap.put(Integer.parseInt("1f610", 16), getEmojiHTML("u1f610"));
        sEmojiMap.put(Integer.parseInt("1f611", 16), getEmojiHTML("u1f611"));
        sEmojiMap.put(Integer.parseInt("1f612", 16), getEmojiHTML("u1f612"));
        sEmojiMap.put(Integer.parseInt("1f613", 16), getEmojiHTML("u1f613"));
        sEmojiMap.put(Integer.parseInt("1f614", 16), getEmojiHTML("u1f614"));
        sEmojiMap.put(Integer.parseInt("1f615", 16), getEmojiHTML("u1f615"));
        sEmojiMap.put(Integer.parseInt("1f616", 16), getEmojiHTML("u1f616"));
        sEmojiMap.put(Integer.parseInt("1f617", 16), getEmojiHTML("u1f617"));
        sEmojiMap.put(Integer.parseInt("1f618", 16), getEmojiHTML("u1f618"));
        sEmojiMap.put(Integer.parseInt("1f619", 16), getEmojiHTML("u1f619"));
        sEmojiMap.put(Integer.parseInt("1f61a", 16), getEmojiHTML("u1f61a"));
        sEmojiMap.put(Integer.parseInt("1f61b", 16), getEmojiHTML("u1f61b"));
        sEmojiMap.put(Integer.parseInt("1f61c", 16), getEmojiHTML("u1f61c"));
        sEmojiMap.put(Integer.parseInt("1f61d", 16), getEmojiHTML("u1f61d"));
        sEmojiMap.put(Integer.parseInt("1f61e", 16), getEmojiHTML("u1f61e"));
        sEmojiMap.put(Integer.parseInt("1f61f", 16), getEmojiHTML("u1f61f"));
        sEmojiMap.put(Integer.parseInt("1f620", 16), getEmojiHTML("u1f620"));
        sEmojiMap.put(Integer.parseInt("1f621", 16), getEmojiHTML("u1f621"));
        sEmojiMap.put(Integer.parseInt("1f622", 16), getEmojiHTML("u1f622"));
        sEmojiMap.put(Integer.parseInt("1f623", 16), getEmojiHTML("u1f623"));
        sEmojiMap.put(Integer.parseInt("1f624", 16), getEmojiHTML("u1f624"));
        sEmojiMap.put(Integer.parseInt("1f625", 16), getEmojiHTML("u1f625"));
        sEmojiMap.put(Integer.parseInt("1f626", 16), getEmojiHTML("u1f626"));
        sEmojiMap.put(Integer.parseInt("1f627", 16), getEmojiHTML("u1f627"));
        sEmojiMap.put(Integer.parseInt("1f628", 16), getEmojiHTML("u1f628"));
        sEmojiMap.put(Integer.parseInt("1f629", 16), getEmojiHTML("u1f629"));
        sEmojiMap.put(Integer.parseInt("1f62a", 16), getEmojiHTML("u1f62a"));
        sEmojiMap.put(Integer.parseInt("1f62b", 16), getEmojiHTML("u1f62b"));
        sEmojiMap.put(Integer.parseInt("1f62c", 16), getEmojiHTML("u1f62c"));
        sEmojiMap.put(Integer.parseInt("1f62d", 16), getEmojiHTML("u1f62d"));
        sEmojiMap.put(Integer.parseInt("1f62e", 16), getEmojiHTML("u1f62e"));
        sEmojiMap.put(Integer.parseInt("1f62f", 16), getEmojiHTML("u1f62f"));
        sEmojiMap.put(Integer.parseInt("1f630", 16), getEmojiHTML("u1f630"));
        sEmojiMap.put(Integer.parseInt("1f631", 16), getEmojiHTML("u1f631"));
        sEmojiMap.put(Integer.parseInt("1f632", 16), getEmojiHTML("u1f632"));
        sEmojiMap.put(Integer.parseInt("1f633", 16), getEmojiHTML("u1f633"));
        sEmojiMap.put(Integer.parseInt("1f634", 16), getEmojiHTML("u1f634"));
        sEmojiMap.put(Integer.parseInt("1f635", 16), getEmojiHTML("u1f635"));
        sEmojiMap.put(Integer.parseInt("1f636", 16), getEmojiHTML("u1f636"));
        sEmojiMap.put(Integer.parseInt("1f637", 16), getEmojiHTML("u1f637"));
        sEmojiMap.put(Integer.parseInt("1f638", 16), getEmojiHTML("u1f638"));
        sEmojiMap.put(Integer.parseInt("1f639", 16), getEmojiHTML("u1f639"));
        sEmojiMap.put(Integer.parseInt("1f63a", 16), getEmojiHTML("u1f63a"));
        sEmojiMap.put(Integer.parseInt("1f63b", 16), getEmojiHTML("u1f63b"));
        sEmojiMap.put(Integer.parseInt("1f63c", 16), getEmojiHTML("u1f63c"));
        sEmojiMap.put(Integer.parseInt("1f63d", 16), getEmojiHTML("u1f63d"));
        sEmojiMap.put(Integer.parseInt("1f63e", 16), getEmojiHTML("u1f63e"));
        sEmojiMap.put(Integer.parseInt("1f63f", 16), getEmojiHTML("u1f63f"));
        sEmojiMap.put(Integer.parseInt("1f640", 16), getEmojiHTML("u1f640"));
        sEmojiMap.put(Integer.parseInt("1f645", 16), getEmojiHTML("u1f645"));
        sEmojiMap.put(Integer.parseInt("1f646", 16), getEmojiHTML("u1f646"));
        sEmojiMap.put(Integer.parseInt("1f647", 16), getEmojiHTML("u1f647"));
        sEmojiMap.put(Integer.parseInt("1f648", 16), getEmojiHTML("u1f648"));
        sEmojiMap.put(Integer.parseInt("1f649", 16), getEmojiHTML("u1f649"));
        sEmojiMap.put(Integer.parseInt("1f64a", 16), getEmojiHTML("u1f64a"));
        sEmojiMap.put(Integer.parseInt("1f64b", 16), getEmojiHTML("u1f64b"));
        sEmojiMap.put(Integer.parseInt("1f64c", 16), getEmojiHTML("u1f64c"));
        sEmojiMap.put(Integer.parseInt("1f64d", 16), getEmojiHTML("u1f64d"));
        sEmojiMap.put(Integer.parseInt("1f64e", 16), getEmojiHTML("u1f64e"));
        sEmojiMap.put(Integer.parseInt("1f64f", 16), getEmojiHTML("u1f64f"));
        sEmojiMap.put(Integer.parseInt("1f6b6", 16), getEmojiHTML("u1f6b6"));
        sEmojiMap.put(Integer.parseInt("263a", 16), getEmojiHTML("u263a")); // Duplicate of u1f600
        sEmojiMap.put(Integer.parseInt("270a", 16), getEmojiHTML("u270a"));
        sEmojiMap.put(Integer.parseInt("270b", 16), getEmojiHTML("u270b"));
        sEmojiMap.put(Integer.parseInt("2728", 16), getEmojiHTML("u2728"));

        // Test code for printing out the mappings
        // for (int i = 0; i < sEmojiMap.size(); i++) {
        //     int key = sEmojiMap.keyAt(i);
        //     String html = sEmojiMap.get(key);
        //     LogIt.d(EmojiUtils.class, key + " => " + html);
        // }

        final Context context = MessageMeApplication.getInstance();

        sEmojiImageGetterBig = new ImageGetter() {
            public Drawable getDrawable(String source) {
                int id = context.getResources().getIdentifier(source,
                        "drawable", context.getPackageName());

                Drawable emoji = context.getResources().getDrawable(id);
                int w = (int) (emoji.getIntrinsicWidth());
                int h = (int) (emoji.getIntrinsicHeight());
                emoji.setBounds(0, 0, w, h);
                return emoji;
            }
        };

        // Serve up smaller versions of the emoji images
        sEmojiImageGetterSmall = new ImageGetter() {
            public Drawable getDrawable(String source) {
                int id = context.getResources().getIdentifier(source,
                        "drawable", context.getPackageName());

                Drawable emoji = context.getResources().getDrawable(id);
                int w = (int) (emoji.getIntrinsicWidth() * 0.6);
                int h = (int) (emoji.getIntrinsicHeight() * 0.6);
                emoji.setBounds(0, 0, w, h);
                return emoji;
            }
        };
    }

    /**
     * Get HTML suitable for using to show an image in the place of the 
     * provided emoji.
     */
    private static String getEmojiHTML(String emojiFileName) {

        // Test code for checking if any of the emoji images are missing
        //
        // Context context = MessageMeApplication.getInstance();
        // int imageResource = context.getResources().getIdentifier(
        //         "drawable/" + emojiFileName, null, context.getPackageName());
        //
        // if (imageResource == 0) { 
        //     LogIt.e(EmojiUtils.class, emojiFileName + " file missing");
        // }

        StringBuilder s = new StringBuilder();
        s.append("<img src=\"");
        s.append(emojiFileName);
        s.append("\"/>");
        return s.toString();
    }

    /**
     * A fast, efficient way of checking if a text String contains an emoji.
     */
    public static boolean containsEmoji(String str) {

        if (str == null) {
            return false;
        }
        
        for (int i = 0; i < str.length(); i++) {

            int cp = Character.codePointAt(str, i);

            if (sEmojiMap.get(cp) != null) {
                // LogIt.d(EmojiUtils.class, "Emoji found", str.charAt(i), cp);
                return true;
            }

            // Skip forwards multiple characters instead of 1 if we just 
            // processed a multiple character Unicode value (e.g. emojis
            // typically span two characters).
            int charCount = Character.charCount(cp);

            if (charCount > 1) {
                i += charCount - 1;

                if (i >= str.length()) {
                    LogIt.w(EmojiUtils.class, "Unexpected truncation");
                }
            }
        }

        return false;
    }

    public static SparseArray<String> getEmojisMap() {
        return sEmojiMap;
    }

    /**
     * Convert the input string so that all emoji Unicode values in it are
     * replaced with HTML to show a local image of the emoji.
     * 
     * Generally {@link #convertToEmojisIfRequired(String)} should be used
     * instead of this method.  However, there are some special cases were
     * external classes need access to this method, e.g. anywhere that a
     * String needs additional HTML formatting applied.
     */
    public static String convertEmojisRaw(String input) {

        StringBuilder output = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {

            int cp = Character.codePointAt(input, i);
            String html = sEmojiMap.get(cp);

            if (html == null) {
                // No emoji, use the existing value
                output.appendCodePoint(cp);
            } else {
                // We have an emoji for this character code point 
                output.append(html);
            }

            // Skip forwards multiple characters instead of 1 if we just 
            // processed a multiple character Unicode value (e.g. emojis
            // typically span two characters).
            int charCount = Character.charCount(cp);

            if (charCount > 1) {
                i += charCount - 1;

                if (i >= input.length()) {
                    LogIt.w(EmojiUtils.class, "Unexpected truncation");
                }
            }
        }

        return output.toString();
    }

    /**
     * @return an ImageGetter that can serve up normal size local emoji images.
     */
    public static ImageGetter getEmojiImageGetterNormal() {
        return sEmojiImageGetterBig;
    }

    /**
     * @return an ImageGetter that can serve up smaller versions of the local 
     * emoji images.
     */
    public static ImageGetter getEmojiImageGetterSmall() {
        return sEmojiImageGetterSmall;
    }

    public enum EmojiSize {
        SMALL, NORMAL;
    }

    /**
     * Convert emoji Unicode chars in the provided 'text' String into HTML
     * for displaying them as images.
     */
    public static CharSequence convertToEmojisIfRequired(String text,
            EmojiSize emojiSize) {

        if (EmojiUtils.containsEmoji(text)) {

            // LogIt.d(EmojiUtils.class, "convertToEmojisIfRequired - start", text);
            
            StringBuffer output = new StringBuffer();
            
            String emojiText = EmojiUtils.convertEmojisRaw(text);

            // Converting to HTML loses the newlines in the original text, so
            // we manually convert them to <br> tags to preserve them.
            Matcher newLineMatcher = mNewLinePattern.matcher(emojiText);

            while (newLineMatcher.find()) {
                newLineMatcher.appendReplacement(output, "<br>");
            }
            newLineMatcher.appendTail(output);
            
            // LogIt.d(EmojiUtils.class, "convertToEmojisIfRequired - done", output);
            
            try {
                CharSequence spanned;

                if (emojiSize == EmojiSize.NORMAL) {
                    spanned = Html.fromHtml(output.toString(),
                            EmojiUtils.getEmojiImageGetterNormal(), null);
                } else {
                    spanned = Html.fromHtml(output.toString(),
                            EmojiUtils.getEmojiImageGetterSmall(), null);
                }

                return spanned;
            } catch (Exception e) {
                // Show the normal text instead
                LogIt.e(EmojiUtils.class, "Error showing text with emojis",
                        emojiText);
                return text;
            }
        } else {
            return text;
        }
    }
    
    /**
     * Same as {@link #convertToEmojisIfRequired(String, EmojiSize)}, but this
     * method returns a Spanned object instead of a CharSequence.
     */
    public static Spanned convertToEmojisIfRequiredSpanned(String text,
            EmojiSize emojiSize) {

        if (EmojiUtils.containsEmoji(text)) {

            String emojiText = EmojiUtils.convertEmojisRaw(text);

            try {
                Spanned spanned;

                if (emojiSize == EmojiSize.NORMAL) {
                    spanned = Html.fromHtml(emojiText.toString(),
                            EmojiUtils.getEmojiImageGetterNormal(), null);
                } else {
                    spanned = Html.fromHtml(emojiText.toString(),
                            EmojiUtils.getEmojiImageGetterSmall(), null);
                }

                return spanned;
            } catch (Exception e) {
                // Show the normal text instead
                LogIt.e(EmojiUtils.class, "Error showing text with emojis",
                        emojiText);
                return Html.fromHtml(text);
            }
        } else {
            return Html.fromHtml(text);
        }
    }
    
    /**
     * Utility method only for use when debugging
     */
    public static String printCharacterCodes(String str) {

        StringBuilder output = new StringBuilder();
        
        for (int i = 0; i < str.length(); i++) {

            int cp = Character.codePointAt(str, i);

            output.append(cp);
            output.append(" ");
            
            // Skip forwards multiple characters instead of 1 if we just 
            // processed a multiple character Unicode value (e.g. emojis
            // typically span two characters).
            int charCount = Character.charCount(cp);

            if (charCount > 1) {
                i += charCount - 1;

                if (i >= str.length()) {
                    LogIt.w(EmojiUtils.class, "Unexpected truncation");
                }
            }
        }

        return output.toString();
    }
}