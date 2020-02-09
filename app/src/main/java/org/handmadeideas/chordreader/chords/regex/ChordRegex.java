package org.handmadeideas.chordreader.chords.regex;

import java.util.regex.Pattern;

public class ChordRegex {

	private String regexString;
	private String regexStringWithParens;
	private Pattern pattern;
	private Pattern patternWithParens;
	public String getRegexString() {
		return regexString;
	}
	public void setRegexString(String regexString) {
		this.regexString = regexString;
	}
	public String getRegexStringWithParens() {
		return regexStringWithParens;
	}
	public void setRegexStringWithParens(String regexStringWithParens) {
		this.regexStringWithParens = regexStringWithParens;
	}
	public Pattern getPattern() {
		return pattern;
	}
	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}
	public Pattern getPatternWithParens() {
		return patternWithParens;
	}
	public void setPatternWithParens(Pattern patternWithParens) {
		this.patternWithParens = patternWithParens;
	}
}
