package com.coredroid.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

/**
 * Miscellaneous string functions
 */
public class StringUtil {

    /**
     * Replaces {propname} matches in the string with the corresponding object.toString
     */
    private static final Pattern REPLACE_PATTERN = Pattern
            .compile("([^\\\\]|^)\\{(.+?)\\}");

    public static String replace(String str, Map<String, Object> replacementMap) {
        Matcher matcher = REPLACE_PATTERN.matcher(str);

        //populate the replacements map ...
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (matcher.find()) {
            Object replacement = replacementMap.get(matcher.group(2));
            builder.append(str.substring(i,
                    matcher.start() == 0 ? 0 : matcher.start() + 1));
            builder.append(replacement != null ? replacement : "");
            i = matcher.end();
        }
        builder.append(str.substring(i, str.length()));

        String result = builder.toString();
        result = result.replace("\\{", "{");
        result = result.replace("\\}", "}");

        return result;
    }

    public static String replace(String str, JSONObject replacementMap) {
        Matcher matcher = REPLACE_PATTERN.matcher(str);

        //populate the replacements map ...
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (matcher.find()) {
            Object replacement = replacementMap.opt(matcher.group(2));
            builder.append(str.substring(i,
                    matcher.start() == 0 ? 0 : matcher.start() + 1));
            builder.append(replacement != null ? replacement : "");
            i = matcher.end();
        }
        builder.append(str.substring(i, str.length()));

        String result = builder.toString();
        result = result.replace("\\{", "{");
        result = result.replace("\\}", "}");

        return result;
    }

    /**
     * String helper to ensure the object safely translates to a string, will not return null
     */
    public static String toString(Object o) {
        return o != null ? o.toString() : "";
    }

    public static boolean isEmpty(String str) {
        if (str == null) {
            return true;
        }

        return str.trim().length() == 0;
    }
}
