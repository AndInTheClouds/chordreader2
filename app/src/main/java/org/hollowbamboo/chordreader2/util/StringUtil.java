package org.hollowbamboo.chordreader2.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nolan
 */
public class StringUtil {

    /**
     * same as the String.split(), except it doesn't use regexes, so it's faster.
     *
     * @param str       - the string to split up
     * @param delimiter the delimiter
     * @return the split string
     */
    public static String[] split(String str, String delimiter) {
        List<String> result = new ArrayList<String>();
        int lastIndex = 0;
        int index = str.indexOf(delimiter);
        while (index != -1) {
            result.add(str.substring(lastIndex, index));
            lastIndex = index + delimiter.length();
            index = str.indexOf(delimiter, index + delimiter.length());
        }
        result.add(str.substring(lastIndex, str.length()));

        return result.toArray(new String[result.size()]);
    }

 /*
     * Replace all occurances of the searchString in the originalString with the replaceString.  Faster than the
     * String.replace() method.  Does not use regexes.
     * <p/>
     * If your searchString is empty, this will spin forever.
     *
     *
     * @param originalString
     * @param searchString
     * @param replaceString
     * @return
     */
    public static String replace(String originalString, String searchString, String replaceString) {
        StringBuilder sb = new StringBuilder(originalString);
        int index = sb.indexOf(searchString);
        while (index != -1) {
            sb.replace(index, index + searchString.length(), replaceString);
            index += replaceString.length();
            index = sb.indexOf(searchString, index);
        }
        return sb.toString();
    }


    public static boolean isAllWhitespace(CharSequence str) {
    	for (int i = 0; i < str.length(); i++) {
    		if(!Character.isWhitespace(str.charAt(i))) {
    			return false;
    		}
    	}
    	return true;
    }
}
