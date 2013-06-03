package com.littleinc.MessageMe.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.EditText;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;
import com.littleinc.MessageMe.util.StringUtil;

/**
 * A custom EditText class that allows emoji characters to be inserted by
 * providing the character cope point of the emoji.
 */
public class EmojiEditText extends EditText {

    private final Pattern mImgHTMLPattern = Pattern.compile(
            "<img src=\"u([a-zA-Z0-9]*)\">", Pattern.CASE_INSENSITIVE);

    /**
     * Keep track of whether this EditText contains any emojis. Only run the
     * new conversion code if an emoji was actually inserted.
     */
    private boolean mContainsEmojis = false;

    public EmojiEditText(Context context) {
        super(context);
    }

    public EmojiEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Insert the provided emoji character code at the current cursor position.
     */
    public void insertEmoji(Integer characterCode) {

        mContainsEmojis = true;

        Editable text = getText();

        // Remove any text currently selected
        int start = getSelectionStart();
        int end = getSelectionEnd();

        LogIt.d(EmojiEditText.class, "Insert emoji", characterCode, start, end);

        if (start != end) {
            text.delete(start, end);
        }

        char[] chars = Character.toChars(characterCode);
        CharSequence charSeq = String.valueOf(chars);

        Spanned sp = EmojiUtils.convertToEmojisIfRequiredSpanned(
                charSeq.toString(), EmojiSize.NORMAL);

        if (start >= text.length()) {
            text.append(sp);
        } else {
            text.insert(start, sp);
        }
    }

    public boolean containsEmojis() {
        return mContainsEmojis;
    }

    /**
     * @return a String of the text suitable for sending in a text message. Any
     * emoji HTML will be converted to its Unicode character
     * representation.
     */
    public String getEmojiText() {

        // This EditText gets cleared afterwards, so reset this flag
        mContainsEmojis = false;

        String rawHTML = Html.toHtml(getText());

        return convertEmojiHTMLToUnicodeString(rawHTML);
    }

    private String convertEmojiHTMLToUnicodeString(String rawHTML) {

        StringBuffer output = new StringBuffer();

        LogIt.d(EmojiUtils.class, "EmojiEditText convert - start", rawHTML);

        // Match our emoji HTML images, e.g.
        // <img src="u1f6b6">
        Matcher imgMatcher = mImgHTMLPattern.matcher(rawHTML);

        while (imgMatcher.find()) {

            // Extract the emoji character code from the image filename
            // e.g. 1f6b6
            String fileName = imgMatcher.group(1);

            if (fileName.length() > 2) {
                try {
                    int charCode = Integer.parseInt(fileName, 16);

                    char[] chars = Character.toChars(charCode);

                    // LogIt.d(EmojiUtils.class, "Replacing", "<img src=\"u"
                    // + fileName + "\">", charCode, new String(chars));

                    imgMatcher.appendReplacement(output, new String(chars));

                } catch (NumberFormatException e) {
                    LogIt.w(EmojiUtils.class,
                            "NumberFormatException when parsing img HTML",
                            fileName, e);
                }
            } else {
                LogIt.d(EmojiUtils.class,
                        "Ignore image source that is too small to be one of ours",
                        fileName);
            }
        }
        imgMatcher.appendTail(output);

        // LogIt.d(EmojiUtils.class, "EmojiEditText convert - middle", output);

        // Remove all HTML from the string. Different devices insert all sorts
        // of HTML, e.g. <p>text</p>, <font color="#000000"> etc, and we need to
        // remove it all to turn the message into plain text again.
        String result = Html.fromHtml(output.toString()).toString();
        
        // Using HTML to display emojis results in either one or two unwanted 
        // newlines at the end of the message. Remove them.  Unfortunately this
        // means we sometimes remove one newline too many, but iOS does too.
        LogIt.d(EmojiUtils.class, "EmojiEditText convert - remove newline", result);
        result = StringUtil.removeTrailingNewlines(result, 2); 
        
        LogIt.d(EmojiUtils.class, "EmojiEditText convert - complete", result);

        return result;
    }

    /**
     * Custom handling of cut/copy/paste events.
     * 
     * This is based on the Android source code here:
     * http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.
     * android/android/
     * 2.3_r1/android/widget/TextView.java#TextView.onTextContextMenuItem%28int%2
     * 9
     */
    @Override
    public boolean onTextContextMenuItem(int id) {

        int min = 0;
        int max = getText().length();

        if (isFocused()) {
            final int selStart = getSelectionStart();
            final int selEnd = getSelectionEnd();

            min = Math.max(0, Math.min(selStart, selEnd));
            max = Math.max(0, Math.max(selStart, selEnd));
        }

        ClipboardManager clip = (ClipboardManager) getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);

        switch (id) {
            case android.R.id.cut:
            case android.R.id.copy:
                // Copy and cut use the same code
                if (mContainsEmojis) {
                    SpannableStringBuilder selectedText = (SpannableStringBuilder) getText()
                            .subSequence(min, max);

                    // Turn the selected Spanned text into raw HTML
                    String rawHTML = Html.toHtml(selectedText);

                    String emojiUnicode = convertEmojiHTMLToUnicodeString(rawHTML);
                    
                    LogIt.d(this, "Put text in clipboard", emojiUnicode.toString());
                    clip.setText(emojiUnicode);

                    if (id == android.R.id.cut) {
                        LogIt.d(this, "Cut original text", min, max);
                        getText().delete(min, max);
                    }

                    return true;
                } else {
                    LogIt.d(this, "Copy/cut - no emojis so use default handling");
                    return super.onTextContextMenuItem(id);
                }
            case android.R.id.paste:

                if ((clip == null) || (clip.getText() == null)) {
                    LogIt.w(this, "Ignore paste with null clip or null text");
                    return true;
                }
                
                String paste = clip.getText().toString();

                if ((paste == null) || (paste.length() == 0)) {
                    LogIt.d(this, "Paste - nothing in clipboard");
                    return true;
                }

                if (EmojiUtils.containsEmoji(paste)) {
                    LogIt.d(this, "Paste - convert emojis");
                    CharSequence emojiText = EmojiUtils
                            .convertToEmojisIfRequired(paste, EmojiSize.NORMAL);
                    getText().replace(min, max, emojiText);
                    mContainsEmojis = true;

                    return true;
                } else {
                    LogIt.d(this, "Paste - no emojis so use default handling");
                    return super.onTextContextMenuItem(id);
                }

            default:
                return super.onTextContextMenuItem(id);
        }
    }
}
