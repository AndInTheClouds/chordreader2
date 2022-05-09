package org.hollowbamboo.chordreader2.chords.regex;

public class TokenInText {

	private String token;
	private int startIndex;
	private int endIndex;
	public String getToken() {
		return token;
	}
	public int getStartIndex() {
		return startIndex;
	}
	public int getEndIndex() {
		return endIndex;
	}

	public static org.hollowbamboo.chordreader2.chords.regex.TokenInText newTokenInText(String token, int startIndex, int endIndex) {
		
		org.hollowbamboo.chordreader2.chords.regex.TokenInText tokenInText = new org.hollowbamboo.chordreader2.chords.regex.TokenInText();
		
		tokenInText.token = token;
		tokenInText.startIndex = startIndex;
		tokenInText.endIndex = endIndex;
		
		return tokenInText;
	}
	
	
	
}
