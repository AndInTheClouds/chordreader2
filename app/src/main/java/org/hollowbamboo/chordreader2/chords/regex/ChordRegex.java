package org.hollowbamboo.chordreader2.chords.regex;

import java.util.regex.Pattern;

public class ChordRegex {

	private Pattern pattern;
	private Pattern patternWithParens;

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
