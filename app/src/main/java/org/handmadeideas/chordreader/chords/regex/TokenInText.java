package org.handmadeideas.chordreader.chords.regex;

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

	public static org.handmadeideas.chordreader.chords.regex.TokenInText newTokenInText(String token, int startIndex, int endIndex) {
		
		org.handmadeideas.chordreader.chords.regex.TokenInText tokenInText = new org.handmadeideas.chordreader.chords.regex.TokenInText();
		
		tokenInText.token = token;
		tokenInText.startIndex = startIndex;
		tokenInText.endIndex = endIndex;
		
		return tokenInText;
	}
	
	
	
}
