package org.handmadeideas.chordreader.chords.regex;

import android.text.TextUtils;
import org.handmadeideas.chordreader.chords.*;
import org.handmadeideas.chordreader.util.StringUtil;
import org.handmadeideas.chordreader.util.UtilLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChordParser {

	private static UtilLogger log = new UtilLogger(org.handmadeideas.chordreader.chords.regex.ChordParser.class);
	// characters that may show up in a written chord
	private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\w#+/]+");
	private static final Pattern LOWERCASE_WORD_PATTERN = Pattern.compile("[a-z]+");
	
	
	/**
	 * Attempts to parse a string representation of a chord into a Chord object.  Returns null if it fails to match.
	 * @param chordString
	 * @return
	 */
	public static Chord parseChord(CharSequence chordString, NoteNaming noteNaming) {
		
		Pattern pattern = ChordRegexes.getChordPattern(noteNaming);
		Matcher matcher = pattern.matcher(chordString);
		
		if (matcher.matches()) {
			return convertMatchedPatternToChord(matcher, noteNaming);
			
		}
		
		return null;
		
	}
	
	private static Chord convertMatchedPatternToChord(Matcher matcher, NoteNaming noteNaming) {

		String root = matcher.group(1);
		String qualityOrSeventh = matcher.group(2);
		String add = matcher.group(3);
		String sus = matcher.group(4);
		String overridingRoot = matcher.group(5);
		
		ChordRoot chordRoot = noteNaming.findByAlias(root);
		if (chordRoot == null) {
			return null;
		}
		
		
		// seventh and quality are typically written in the same place in the chord name's lexical structure
		ChordQuality chordQuality = ChordQuality.Major;
		ChordExtended chordSeventh = ChordExtended.findByAlias(qualityOrSeventh);
		
		if (chordSeventh == null) {
			chordQuality = ChordQuality.findByAlias(qualityOrSeventh);
		} else {
			chordQuality = chordSeventh.getChordQuality();
		}
		
		ChordAdded chordAdded = ChordAdded.findByAlias(add);
		ChordSuspended chordSuspsended = ChordSuspended.findByAlias(sus);
		
		ChordRoot overridingChordRoot = null;
		
		if (!TextUtils.isEmpty(overridingRoot)) {
			overridingChordRoot = noteNaming.findByAlias(overridingRoot.substring(1)); // cut off initial "/"
		}
		
		Chord chord = Chord.newChord(
				chordRoot, chordQuality, chordSeventh, chordAdded, chordSuspsended, overridingChordRoot);
		
		return chord;
	}

	/**
	 * Return true if it looks like there are "chord lines" in this text -e.g.  C   D  G  C
	 * @param text
	 * @return
	 */
	public static boolean containsLineWithChords(String text, NoteNaming noteNaming) {
		
		if (TextUtils.isEmpty(text == null ? null : text.trim())) {
			return false;
		}
		
		String[] lines = StringUtil.split(text, "\n");
		
		for (String line : lines) {
			if (isLineContainingChords(line, noteNaming)) {
				return true;
			}
		}
		log.d("found no lines containing chords in text");
		return false;
		
	}

	private static boolean isLineContainingChords(String line, NoteNaming noteNaming) {
		return !findChordsInTextInLine(line, 0, noteNaming).isEmpty();

	}

	/**
	 * Return an ordered list of the chordsInText in the text
	 * @param text
	 * @return
	 */
	public static List<ChordInText> findChordsInText(String text, NoteNaming noteNaming) {
		
		List<ChordInText> result = new ArrayList<ChordInText>();
		
		String[] lines = StringUtil.split(text, "\n");
		
		int offset = 0;
		
		for (String line : lines) {
			
			result.addAll(findChordsInTextInLine(line, offset, noteNaming));
			
			offset += line.length() + 1; // plus one for the \n
			
		}

		// walk backwards through each chord from finish to start
		Collections.sort(result, ChordInText.sortByStartIndex());
		
		return result;
		
	}
	
	private static List<ChordInText> findChordsInTextInLine(String line, int offset, NoteNaming noteNaming) {
		
		if (TextUtils.isEmpty(line.trim())) {
			return Collections.emptyList();
		}
		
		// heuristic description:
		// find all chord tokens that are adjacent - those are pretty sure to be chords unless it's a common bigram like "am a"
		// also, any chords in parens are sure-fire chords as well
		
		List<ChordInText> result = new ArrayList<ChordInText>();
		
		TokenInText[] tokens = getTokensInTextFromLine(line);
		
		ChordInText[] candidateChordsInText = null;
		
		Pattern chordPattern = ChordRegexes.getChordPattern(noteNaming);
		Pattern chordWithParensPattern = ChordRegexes.getChordWithParensPattern(noteNaming);
		
		for (int i = 0; i < tokens.length; i++) {
			
			TokenInText tokenInText = tokens[i];
			String token = tokenInText.getToken();
			Matcher matcher = chordWithParensPattern.matcher(token);
			if (matcher.find()) { // it's okay for it to just be part of the token in this case
				// add a sure-fire chord with parens and continue
				Chord chord = convertMatchedPatternToChord(matcher, noteNaming);
				ChordInText chordInText = ChordInText.newChordInText(
						chord, 
						tokenInText.getStartIndex() + matcher.start() + offset + 1, // +1 for starting parenthesis 
						tokenInText.getStartIndex() + matcher.end() + offset - 1 // -1 for ending parenthesis
						);
				result.add(chordInText);
			} else {
				// add some candidate chordsInText to the array
				matcher = chordPattern.matcher(token);
				if (matcher.matches()) { // must match exactly
					Chord chord = convertMatchedPatternToChord(matcher, noteNaming);
					ChordInText chordInText = ChordInText.newChordInText(chord, tokenInText.getStartIndex() + offset, tokenInText.getEndIndex() + offset);
					
					if (candidateChordsInText == null) {
						candidateChordsInText = new ChordInText[tokens.length];
					}
					
					candidateChordsInText[i] = chordInText;
				}
				
			}
		}
		
		// check all the candidates to see which ones are adjacent.  All adjacents ones (i.e. tokens that aren't just
		// some instance of the word 'a' or 'am' or something) are sure-fire bets
		
		if (candidateChordsInText != null) {
			
			for (int i = 0; i < candidateChordsInText.length; i++) {
				
				ChordInText candidateChordInText = candidateChordsInText[i];
				
				if (candidateChordInText == null) {
					continue;
				}
				
				String candidateChordString = line.substring(
						candidateChordInText.getStartIndex() - offset, 
						candidateChordInText.getEndIndex() - offset);
				
				//
				// The following are heuristics to filter out tokens that are unlikely to be actual chords
				//
				
				
				// case where a capital letter is followed by a period
				if (candidateChordString.length() == 1 
						&& candidateChordInText.getEndIndex() - offset < line.length()
						&& line.charAt(candidateChordInText.getEndIndex() - offset) == '.') {
					// unlikely to be a chord
					continue;
					
				}
				
				// case where "Am" is followed by "I"
				if (candidateChordString.equals("Am") 
						&& i + 1 < tokens.length
						&& "Am I".equals(line.substring(tokens[i].getStartIndex(), tokens[i+1].getEndIndex()))) {
					// unlikely to be a chord
					continue;
					
				}				
				// case where "A" is followed by a lowercase word, e.g. "lady"
				if (candidateChordString.equals("A") 
						&& i + 1 < tokens.length
						&& LOWERCASE_WORD_PATTERN.matcher(tokens[i + 1].getToken()).matches()
						&& StringUtil.isAllWhitespace(line.substring(tokens[i].getEndIndex(), tokens[i+1].getStartIndex()))) {
					// unlikely to be a chord
					continue;
					
				}
				
				result.add(candidateChordsInText[i]);
			}
		}
		
		
		return result;		
	}

	private static TokenInText[] getTokensInTextFromLine(String line) {
		
		List<TokenInText> result = new ArrayList<TokenInText>();
		
		Matcher matcher = TOKEN_PATTERN.matcher(line);
		
		while (matcher.find()) {
			result.add(TokenInText.newTokenInText(matcher.group(), matcher.start(), matcher.end()));
		}
		
		return result.toArray(new TokenInText[result.size()]);
		
	}
	
}
