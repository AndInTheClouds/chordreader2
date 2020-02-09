package org.handmadeideas.chordreader.helper;

import android.text.Html;
import android.text.TextUtils;
import org.handmadeideas.chordreader.ChordWebpage;
import org.handmadeideas.chordreader.chords.NoteNaming;
import org.handmadeideas.chordreader.chords.regex.ChordParser;
import org.handmadeideas.chordreader.util.StringUtil;
import org.handmadeideas.chordreader.util.UtilLogger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebPageExtractionHelper {

	private static UtilLogger log = new UtilLogger(org.handmadeideas.chordreader.helper.WebPageExtractionHelper.class);
	
    // html tag or html escaped character
    private static Pattern htmlObjectPattern = Pattern.compile(
            "(" +
            "<\\s*style.*?>.*?<\\s*/style\\s*>" + // style span
            "|" + // OR
            "<\\s*script.*?>.*?<\\s*/script\\s*>" + // script span
            "|" + // OR
            "<\\s*head.*?>.*?<\\s*/head\\s*>" + // head span
            "|" + // OR
            "<[^>]++>" + // html tag, such as '<br/>' or '<a href="www.google.com">'
            "|" + // OR
            "&[^; \n\t]++;" + // escaped html character, such as '&amp;' or '&#0233;'
            ")", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);


    // HTML newline tag, such as '<p>' or '<br/>'
    private static Pattern htmlNewlinePattern = Pattern.compile(
            "<(?:p|br)\\s*+(?:/\\s*+)?>", Pattern.CASE_INSENSITIVE);
    
	private static Pattern prePattern = Pattern.compile("<pre[^>]*>(.*?)</pre>", 
				Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
	
	private static Pattern chordiePattern = Pattern.compile(
			"<!-- END HEADER -->(.*?)<!-- BOTTOM GRIDS - START -->",
			Pattern.DOTALL);
	
	private static Pattern multipleNewlinePattern = Pattern.compile("([ \t\r]*\n[\t\r ]*){2,}");
	
	public static String extractChordChart(ChordWebpage webpage, String html, NoteNaming noteNaming) {
		
		Pattern pattern = null;
		
		switch (webpage) {
		case Chordie:
			pattern = chordiePattern;
			break;
		}
		Matcher matcher = pattern.matcher(html);
		
		if (matcher.find()) {
			String chordHtml = matcher.group(1);
			String chordTxt = convertHtmlToText(chordHtml);
			if (ChordParser.containsLineWithChords(chordTxt, noteNaming)) {
				return cleanUpText(chordTxt);
			}
		}
		return null;
	}
	
	/**
	 * Try to find a likely chord chard from the "pre" section of a page
	 * Returns null if it doesn't find anything likely to be a chord chart
	 * @param html
	 * @return
	 */
	public static String extractLikelyChordChart(String html, NoteNaming noteNaming) {
		
		Matcher matcher = prePattern.matcher(html);
		
		while (matcher.find()) {
			String preHtml = matcher.group(1);
			String preTxt = convertHtmlToText(preHtml);
			if (ChordParser.containsLineWithChords(preTxt, noteNaming)) {
				return cleanUpText(preTxt);
			}
		}
		return null;
		
	}


	public static String convertHtmlToText(String htmlText) {

        StringBuilder plainText = new StringBuilder();

        // replace HTML tags with spaces and unescape HTML characters
        Matcher matcher = htmlObjectPattern.matcher(htmlText);
        int searchIndex = 0;
        while (matcher.find(searchIndex)) {
            int start = matcher.start();
            int end = matcher.end();
            String htmlObject = matcher.group();

            String replacementString;
            if (htmlText.charAt(start) == '&') { // html escaped character
                
                if (htmlObject.equalsIgnoreCase("&nbsp;")) {
                    replacementString = " "; // apache replaces nbsp with unicode \xc2\xa0, but we prefer just " "
                } else {
                    replacementString = Html.fromHtml(htmlObject).toString();
                }

                if (TextUtils.isEmpty(replacementString)) { // ensure non-empty - otherwise offsets would be screwed up
                    log.i("Warning: couldn't escape html string: '" + htmlObject + "'");
                    replacementString = " ";
                }
            } else if (htmlNewlinePattern.matcher(htmlObject).matches()) { // newline tag
                if (htmlObject.toLowerCase().contains("p")) { // paragraph break
                    replacementString = "\n\n";
                } else { // 'br' (carriage return)
                    replacementString = "\n";
                }
            } else { // html tag or <style>/<script> span
                replacementString = "";
            }

            plainText.append(htmlText.substring(searchIndex, start));

            plainText.append(replacementString);

            searchIndex = end;

        }
        
        plainText.append(htmlText.substring(searchIndex, htmlText.length()));

        return plainText.toString();
    }
	
	private static String cleanUpText(String text) {
		
		if (text == null) {
			return text;
		}
		
		
		text = text.trim();
		
		// get rid of \r
		text = StringUtil.replace(text, "\r", "");
		
		// replace multiple newlines with just two newlines
		text = multipleNewlinePattern.matcher(text).replaceAll("\n\n");
		
		return text;
		
	}
	
}
