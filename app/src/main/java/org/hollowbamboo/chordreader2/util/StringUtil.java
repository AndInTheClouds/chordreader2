package org.hollowbamboo.chordreader2.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nolan
 */
public class StringUtil {
	
	/**
	 * Pad the specified number of spaces to the input string to make it that length
	 * @param input
	 * @param size
	 * @return
	 */
	public static String padLeft(String input, int size) {
		
		if(input.length() > size) {
			throw new IllegalArgumentException("input must be shorter than or equal to the number of spaces: " + size);
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = input.length(); i < size; i ++) {
			sb.append(" ");
		}
		return sb.append(input).toString();
	}
	
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

    public static String join(String delimiter, String[] strings) {
        
        if(strings.length == 0) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String str : strings) {
            stringBuilder.append(" ").append(str);
        }

        return stringBuilder.substring(1);
    }
    

    public static String capitalize(String str) {
    	
    	StringBuilder sb = new StringBuilder(str);
    	
    	for (int i = 0; i < sb.length(); i++) {
    		if(i == 0 || Character.isWhitespace(sb.charAt(i - 1))) {
    			sb.replace(i, i + 1, Character.toString(Character.toUpperCase(sb.charAt(i))));
    		}
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
