package com.littleinc.MessageMe.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import com.coredroid.util.LogIt;

public class StringUtil {

    public static final String NEW_LINE = System.getProperty("line.separator");

    public static final char NEW_LINE_CHAR = NEW_LINE.charAt(0);

    private static final Random random = new Random();

    private static final char[] FILENAME_CHARS = new char[62];

    private static final int IMAGE_FILENAME_LENGTH = 6;

    static {
        for (int idx = 0; idx < 10; ++idx)
            FILENAME_CHARS[idx] = (char) ('0' + idx);
        for (int idx = 10; idx < 36; ++idx)
            FILENAME_CHARS[idx] = (char) ('a' + idx - 10);
        for (int idx = 36; idx < 62; ++idx)
            FILENAME_CHARS[idx] = (char) ('A' + idx - 36);
    }

    /**
     * Get a random filename to use for storing an image file, e.g. "2pKfEz".
     */
    public static String getRandomFilename() {
        return getRandomString(IMAGE_FILENAME_LENGTH);
    }

    /**
     * Get a random String with the provided length, containing only characters
     * in {@link #FILENAME_CHARS}, e.g. "2pKfEz".
     */
    public static String getRandomString(int length) {
        char[] buf = new char[length];

        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = FILENAME_CHARS[random.nextInt(FILENAME_CHARS.length)];
        }

        return new String(buf);
    }

    public static boolean isValid(String value) {
        if (value != null & value.length() > 0) {
            return true;
        }

        return false;
    }

    public static String validateString(String string) {
        String stringToValidate = string;

        return stringToValidate.length() == 0 ? "" : stringToValidate;
    }

    public static boolean isEmpty(String str) {
        if (str == null) {
            return true;
        }

        return str.trim().length() == 0;
    }

    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return "";
        } else {
            char[] stringArray = str.toCharArray();
            stringArray[0] = Character.toUpperCase(stringArray[0]);

            return new String(stringArray);
        }
    }

    public static boolean isEmailValid(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target)
                    .matches();
        }
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * SHA1 Encyption metod, encrypts a String into SHA1
     */
    public static String SHA1(String text) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];

        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();

        return convertToHex(sha1hash);
    }

    public static String convertFromInputStream(InputStream stream) {

        BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        StringBuilder sb = new StringBuilder();

        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            br.close();
        } catch (Exception e) {
            LogIt.e(e, e, e.getMessage());
        }

        return sb.toString();
    }

    public static String removeTrailingNewlines(String text,
            int maxNumberToRemove) {

        int removeCount = 0;

        if (text.length() > 0) {
            while ((text.charAt(text.length() - 1) == NEW_LINE_CHAR)
                    && (removeCount < maxNumberToRemove)) {
                text = text.substring(0, text.length() - 1);
                ++removeCount;
            }
        }

        return text;
    }
}